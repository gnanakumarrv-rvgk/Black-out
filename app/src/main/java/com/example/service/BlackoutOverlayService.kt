package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.BlackoutHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class BlackoutOverlayService : Service(), SensorEventListener, SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        const val PREFS_NAME = "blackout_overlay_service_prefs"
        const val KEY_AUTO_START = "key_auto_start_on_boot"
        const val KEY_USE_PROXIMITY = "key_use_proximity"
        const val KEY_USE_ACCEL = "key_use_accel"
        const val KEY_ACCEL_THRESHOLD = "key_accel_threshold_v2"
        const val DEFAULT_ACCEL_THRESHOLD = -7.0f
        const val KEY_SHOW_FLOATING_SHORTCUT = "key_show_floating_shortcut_v2"
        const val KEY_FLOATING_SHORTCUT_OPACITY = "key_floating_shortcut_opacity_v2"
        const val DEFAULT_FLOATING_SHORTCUT_OPACITY = 0.35f

        const val ACTION_START = "action_start_service"
        const val ACTION_STOP = "action_stop_service"
        const val ACTION_FORCE_BLACKOUT_TOGGLE = "action_force_blackout_toggle"

        const val BROADCAST_STATUS = "com.example.service.STATUS_UPDATE"
        const val EXTRA_IS_MONITORING = "extra_is_monitoring"
        const val EXTRA_IS_BLACKED_OUT = "extra_is_blacked_out"
        const val EXTRA_SENSOR_STATE = "extra_sensor_state"

        private const val NOTIFICATION_ID = 99122
        private const val CHANNEL_ID = "blackout_sensor_overlay_channel"
    }

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var proxSensor: Sensor? = null
    private lateinit var windowManager: WindowManager
    private lateinit var prefs: SharedPreferences

    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: FrameLayout? = null
    private var isBlackedOut = false
    private var isMonitoring = false
    private var isManuallyForced = false

    private var useProximity = true
    private var useAccel = true
    private var accelThreshold = DEFAULT_ACCEL_THRESHOLD
    private var showFloatingShortcut = false
    private var floatingShortcutOpacity = DEFAULT_FLOATING_SHORTCUT_OPACITY
    private var floatingShortcutView: FrameLayout? = null

    private var liveZ = 0.0f
    private var liveProx = 5.0f

    private var isFaceDown = false
    private var isNearBox = false

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        loadPreferences()
        createNotificationChannel()
    }

    private fun loadPreferences() {
        useProximity = prefs.getBoolean(KEY_USE_PROXIMITY, true)
        useAccel = prefs.getBoolean(KEY_USE_ACCEL, true)
        accelThreshold = prefs.getFloat(KEY_ACCEL_THRESHOLD, DEFAULT_ACCEL_THRESHOLD)
        showFloatingShortcut = prefs.getBoolean(KEY_SHOW_FLOATING_SHORTCUT, false)
        floatingShortcutOpacity = prefs.getFloat(KEY_FLOATING_SHORTCUT_OPACITY, DEFAULT_FLOATING_SHORTCUT_OPACITY)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopMonitoring()
            stopSelf()
            return START_NOT_STICKY
        }

        loadPreferences()

        if (action == ACTION_FORCE_BLACKOUT_TOGGLE) {
            if (isBlackedOut) {
                dismissOverlay()
            } else {
                isManuallyForced = true
                showOverlay()
            }
            broadcastStatus()
            return START_STICKY
        }

        // Default or ACTION_START
        startMonitoring()
        displayNotification()
        return START_STICKY
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        if (useAccel) {
            accelSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        if (useProximity) {
            proxSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        updateFloatingShortcutVisibility()
        broadcastStatus()
    }

    private fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        sensorManager.unregisterListener(this)
        dismissOverlay()
        updateFloatingShortcutVisibility()
        broadcastStatus()
    }

    private fun broadcastStatus() {
        val sensorStr = when {
            isBlackedOut -> "Blackout Active"
            !isMonitoring -> "Standby"
            else -> {
                val accelPart = if (useAccel) "Z: ${String.format(Locale.US, "%.1f", liveZ)}" else ""
                val proxPart = if (useProximity) "Prox: ${if (isNearBox) "NEAR" else "FAR"}" else ""
                listOf(accelPart, proxPart).filter { it.isNotEmpty() }.joinToString(" | ")
            }
        }

        val intent = Intent(BROADCAST_STATUS).apply {
            putExtra(EXTRA_IS_MONITORING, isMonitoring)
            putExtra(EXTRA_IS_BLACKED_OUT, isBlackedOut)
            putExtra(EXTRA_SENSOR_STATE, sensorStr)
        }
        sendBroadcast(intent)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        var changed = false

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER && useAccel) {
            val z = event.values[2]
            liveZ = z
            val x = event.values[0]
            val y = event.values[1]
            val flatDown = z < accelThreshold && abs(x) < 4.5f && abs(y) < 4.5f
            if (isFaceDown != flatDown) {
                isFaceDown = flatDown
                changed = true
            }
        } else if (event.sensor.type == Sensor.TYPE_PROXIMITY && useProximity) {
            val d = event.values[0]
            liveProx = d
            val maximum = event.sensor.maximumRange
            val near = d < 1.0f || (maximum > 0 && d < maximum * 0.2f)
            if (isNearBox != near) {
                isNearBox = near
                changed = true
            }
        }

        if (changed || isBlackedOut) {
            evaluateTrigger()
            broadcastStatus()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun evaluateTrigger() {
        if (isManuallyForced) return
        val trigger = when {
            useAccel && useProximity -> isFaceDown && isNearBox
            useAccel -> isFaceDown
            useProximity -> isNearBox
            else -> false
        }

        if (trigger) {
            showOverlay()
        } else {
            dismissOverlay()
        }
    }

    private fun showOverlay() {
        if (isBlackedOut) return
        if (!Settings.canDrawOverlays(this)) return

        isBlackedOut = true
        handler.post {
            try {
                if (!isBlackedOut) return@post
                val params = WindowManager.LayoutParams().apply {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                    type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    }
                    flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            or WindowManager.LayoutParams.FLAG_FULLSCREEN
                            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    format = PixelFormat.TRANSLUCENT
                    screenBrightness = 0.01f
                    gravity = Gravity.CENTER
                }

                val frame = FrameLayout(this).apply {
                    setBackgroundColor(Color.BLACK)
                    isClickable = true
                    isFocusable = true
                }

                val text = TextView(this).apply {
                    setText("Blackout Screen Enabled\nDouble tap screen to unlock")
                    setTextColor(Color.parseColor("#35D0BCFF"))
                    textSize = 15f
                    gravity = Gravity.CENTER
                    val paddingDp = (16 * resources.displayMetrics.density).toInt()
                    setPadding(paddingDp, 0, paddingDp, 0)
                }

                val textParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                frame.addView(text, textParams)

                val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        dismissOverlay()
                        // Pause monitoring temporarily for 5s
                        pauseMonitoringTemporarily()
                        return true
                    }
                })

                frame.setOnTouchListener { _, event ->
                    gd.onTouchEvent(event)
                    true
                }

                windowManager.addView(frame, params)
                overlayView = frame
                saveHistoryEntry()
                displayNotification()
                broadcastStatus()
            } catch (e: Exception) {
                isBlackedOut = false
                e.printStackTrace()
            }
        }
    }

    private fun dismissOverlay() {
        isManuallyForced = false
        val oView = overlayView
        if (oView == null) {
            isBlackedOut = false
            return
        }
        overlayView = null
        isBlackedOut = false

        handler.post {
            try {
                windowManager.removeView(oView)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                saveHistoryToDb()
                displayNotification()
                broadcastStatus()
            }
        }
    }

    private fun pauseMonitoringTemporarily() {
        sensorManager.unregisterListener(this)
        handler.postDelayed({
            if (isMonitoring) {
                if (useAccel) {
                    accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
                }
                if (useProximity) {
                    proxSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
                }
            }
        }, 6000L)
    }

    private var blackoutStartTime: Long = 0L

    private fun saveHistoryEntry() {
        blackoutStartTime = System.currentTimeMillis()
    }

    private fun saveHistoryToDb() {
        if (blackoutStartTime == 0L) return
        val durationMs = System.currentTimeMillis() - blackoutStartTime
        val durationSecs = durationMs / 1000
        if (durationSecs < 1) {
            blackoutStartTime = 0L
            return
        }

        // Estimate typical OLED power conservation (0.25W x hours)
        val estimatedEnergySaved = 0.25 * (durationSecs.toDouble() / 3600.0)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext, this)
                val status = if (useAccel && useProximity) "Dual Sensor" else if (useAccel) "Face-Down" else "Proximity"
                db.blackoutHistoryDao().insertHistory(
                    BlackoutHistory(
                        timestamp = blackoutStartTime,
                        durationSeconds = durationSecs,
                        triggerType = status,
                        energySavedEstWh = estimatedEnergySaved
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                blackoutStartTime = 0L
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val desc = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = desc
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun displayNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = if (isBlackedOut) {
            getString(R.string.notification_text_blackout)
        } else {
            getString(R.string.notification_text_face_up)
        }

        val stopIntent = Intent(this, BlackoutOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.button_stop_service),
                stopPendingIntent
            )
            .setColor(Color.parseColor("#381E72"))
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        stopMonitoring()
        removeFloatingShortcutView()
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == KEY_USE_PROXIMITY || key == KEY_USE_ACCEL || key == KEY_ACCEL_THRESHOLD) {
            val oldUseAccel = useAccel
            val oldUseProximity = useProximity
            loadPreferences()
            
            // If active monitoring sensors changed, we need to update registrations!
            if (isMonitoring && (oldUseAccel != useAccel || oldUseProximity != useProximity)) {
                sensorManager.unregisterListener(this)
                if (useAccel) {
                    accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
                }
                if (useProximity) {
                    proxSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
                }
            }
            evaluateTrigger()
            broadcastStatus()
        } else if (key == KEY_SHOW_FLOATING_SHORTCUT || key == KEY_FLOATING_SHORTCUT_OPACITY) {
            loadPreferences()
            updateFloatingShortcutVisibility()
        }
    }

    private fun updateFloatingShortcutVisibility() {
        handler.post {
            if (isMonitoring && showFloatingShortcut) {
                if (floatingShortcutView == null) {
                    createFloatingShortcutView()
                } else {
                    floatingShortcutView?.alpha = floatingShortcutOpacity
                }
            } else {
                removeFloatingShortcutView()
            }
        }
    }

    private fun createFloatingShortcutView() {
        if (!Settings.canDrawOverlays(this)) return
        if (floatingShortcutView != null) return

        try {
            val sizePx = (56 * resources.displayMetrics.density).toInt()
            val params = WindowManager.LayoutParams().apply {
                width = sizePx
                height = sizePx
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                format = PixelFormat.TRANSLUCENT
                gravity = Gravity.TOP or Gravity.START
                
                val displayMetrics = resources.displayMetrics
                x = displayMetrics.widthPixels - sizePx - (16 * displayMetrics.density).toInt()
                y = displayMetrics.heightPixels - sizePx - (120 * displayMetrics.density).toInt()
            }

            val frame = FrameLayout(this).apply {
                alpha = floatingShortcutOpacity
            }

            val circleDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor("#1C1B1F"))
                setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#AC8EFF"))
            }
            frame.background = circleDrawable

            val iconView = TextView(this).apply {
                text = "📴"
                textSize = 20f
                gravity = Gravity.CENTER
            }
            frame.addView(iconView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false

            frame.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x = (initialX + dx).toInt()
                            params.y = (initialY + dy).toInt()
                            try {
                                windowManager.updateViewLayout(frame, params)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            toggleManualBlackout()
                        }
                        true
                    }
                    else -> false
                }
            }

            windowManager.addView(frame, params)
            floatingShortcutView = frame
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingShortcutView() {
        val viewToRemove = floatingShortcutView ?: return
        try {
            windowManager.removeView(viewToRemove)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            floatingShortcutView = null
        }
    }

    private fun toggleManualBlackout() {
        if (isBlackedOut) {
            dismissOverlay()
        } else {
            isManuallyForced = true
            showOverlay()
        }
    }
}
