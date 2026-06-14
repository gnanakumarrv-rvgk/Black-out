package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.service.BlackoutOverlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPreferences = context.getSharedPreferences(BlackoutOverlayService.PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = sharedPreferences.getBoolean(BlackoutOverlayService.KEY_AUTO_START, false)
            if (isEnabled) {
                val serviceIntent = Intent(context, BlackoutOverlayService::class.java)
                serviceIntent.action = BlackoutOverlayService.ACTION_START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
