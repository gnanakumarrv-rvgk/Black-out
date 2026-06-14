package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.service.BlackoutOverlayService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private fun createSensor(type: Int, maxRange: Float = 10.0f): Sensor {
        val constructor = Sensor::class.java.getDeclaredConstructor()
        constructor.isAccessible = true
        val sensor = constructor.newInstance()
        
        val typeField = try {
            Sensor::class.java.getDeclaredField("mType")
        } catch (e: NoSuchFieldException) {
            Sensor::class.java.getDeclaredField("type")
        }
        typeField.isAccessible = true
        typeField.set(sensor, type)
        
        val maxRangeField = try {
            Sensor::class.java.getDeclaredField("mMaxRange")
        } catch (e: NoSuchFieldException) {
            try {
                Sensor::class.java.getDeclaredField("maxRange")
            } catch (ex: NoSuchFieldException) {
                Sensor::class.java.getDeclaredField("mMaximumRange")
            }
        }
        maxRangeField.isAccessible = true
        maxRangeField.set(sensor, maxRange)
        
        return sensor
    }

    private fun createSensorEvent(sensor: Sensor, values: FloatArray): SensorEvent {
        val constructor = SensorEvent::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
        constructor.isAccessible = true
        val event = constructor.newInstance(values.size)
        
        val valuesField = SensorEvent::class.java.getDeclaredField("values")
        valuesField.isAccessible = true
        valuesField.set(event, values)
        
        val sensorField = SensorEvent::class.java.getDeclaredField("sensor")
        sensorField.isAccessible = true
        sensorField.set(event, sensor)
        
        return event
    }

    @Test
    fun `test file existence and app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Blackout Audio", appName)
    }

    @Test
    fun `test blackout overlay service sensor triggers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Grant overlay permission so service can initiate the overlay view
        ShadowSettings.setCanDrawOverlays(true)
        assertTrue(Settings.canDrawOverlays(context))

        // Set preferences to use both Accelerometer and Proximity sensors
        val prefs: SharedPreferences = context.getSharedPreferences(
            BlackoutOverlayService.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        prefs.edit()
            .putBoolean(BlackoutOverlayService.KEY_USE_PROXIMITY, true)
            .putBoolean(BlackoutOverlayService.KEY_USE_ACCEL, true)
            .commit()

        // Create status receiver
        var lastIsMonitoring = false
        var lastIsBlackedOut = false
        var lastSensorState = ""

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == BlackoutOverlayService.BROADCAST_STATUS) {
                    lastIsMonitoring = intent.getBooleanExtra(BlackoutOverlayService.EXTRA_IS_MONITORING, false)
                    lastIsBlackedOut = intent.getBooleanExtra(BlackoutOverlayService.EXTRA_IS_BLACKED_OUT, false)
                    lastSensorState = intent.getStringExtra(BlackoutOverlayService.EXTRA_SENSOR_STATE) ?: ""
                }
            }
        }

        val filter = IntentFilter(BlackoutOverlayService.BROADCAST_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        // Action: Start monitoring with start intent passed directly to builder
        val startIntent = Intent(context, BlackoutOverlayService::class.java).apply {
            action = BlackoutOverlayService.ACTION_START
        }
        val controller = Robolectric.buildService(BlackoutOverlayService::class.java, startIntent)
        controller.create().startCommand(0, 1)
        val service = controller.get()

        assertTrue(lastIsMonitoring)
        assertFalse(lastIsBlackedOut)

        // Instantiate sensors
        val accelSensor = createSensor(Sensor.TYPE_ACCELEROMETER)
        val proxSensor = createSensor(Sensor.TYPE_PROXIMITY, maxRange = 10.0f)

        // 1. Simulate: Proximity is FAR (d = 5.0f), Accelerometer is face-up (z = 9.8f) -> Should NOT black out
        val proxFarEvent = createSensorEvent(proxSensor, floatArrayOf(5.0f))
        val accelUpEvent = createSensorEvent(accelSensor, floatArrayOf(0.0f, 0.0f, 9.8f))

        service.onSensorChanged(proxFarEvent)
        service.onSensorChanged(accelUpEvent)
        ShadowLooper.idleMainLooper()

        assertFalse(lastIsBlackedOut)

        // 2. Simulate: Proximity is NEAR (d = 0.0f), Accelerometer is STILL face-up (z = 9.8f) -> Should NOT black out (dual sensors required)
        val proxNearEvent = createSensorEvent(proxSensor, floatArrayOf(0.0f))
        service.onSensorChanged(proxNearEvent)
        ShadowLooper.idleMainLooper()

        assertFalse(lastIsBlackedOut)

        // 3. Simulate: Proximity is NEAR (d = 0.0f) AND Accelerometer is face-down (z = -9.8f, x = 0.0f, y = 0.0f) -> Should black out!
        val accelDownEvent = createSensorEvent(accelSensor, floatArrayOf(0.0f, 0.0f, -9.8f))
        service.onSensorChanged(accelDownEvent)
        ShadowLooper.idleMainLooper()

        assertTrue(lastIsBlackedOut)

        // 4. Simulate: Turn face-up (z = 9.8f) again -> Should remove black out
        service.onSensorChanged(accelUpEvent)
        ShadowLooper.idleMainLooper()
        assertFalse(lastIsBlackedOut)

        // 5. Test sensitivity threshold dynamically:
        // Set threshold to Strict/Strict-range (-9.0f)
        prefs.edit().putFloat(BlackoutOverlayService.KEY_ACCEL_THRESHOLD, -9.0f).commit()
        // Proximity NEAR
        service.onSensorChanged(proxNearEvent)
        // Accelerometer slightly tilted face-down at (z = -6.0f). Strict threshold (-9.0f) should reject this.
        val accelTiltedEvent = createSensorEvent(accelSensor, floatArrayOf(0.0f, 0.0f, -6.0f))
        service.onSensorChanged(accelTiltedEvent)
        ShadowLooper.idleMainLooper()
        assertFalse(lastIsBlackedOut) // Should NOT black out under Strict setting!

        // Now set threshold to Sensitive/Sensitive-range (-5.5f)
        prefs.edit().putFloat(BlackoutOverlayService.KEY_ACCEL_THRESHOLD, -5.5f).commit()
        // Trigger verification (sensor change event forces re-evaluation)
        service.onSensorChanged(accelTiltedEvent)
        ShadowLooper.idleMainLooper()
        assertTrue(lastIsBlackedOut) // Should black out under Sensitive setting on the exact same sensor input!

        // 6. Test Floating Shortcut configuration correctness
        prefs.edit().putBoolean(BlackoutOverlayService.KEY_SHOW_FLOATING_SHORTCUT, true).commit()
        prefs.edit().putFloat(BlackoutOverlayService.KEY_FLOATING_SHORTCUT_OPACITY, 0.45f).commit()
        ShadowLooper.idleMainLooper()
        assertTrue(prefs.getBoolean(BlackoutOverlayService.KEY_SHOW_FLOATING_SHORTCUT, false))
        assertEquals(0.45f, prefs.getFloat(BlackoutOverlayService.KEY_FLOATING_SHORTCUT_OPACITY, 0.35f), 0.01f)

        // Cleanup
        context.unregisterReceiver(receiver)
        controller.destroy()
    }
}
