package com.example.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.Bookmark
import com.example.service.BlackoutOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlackoutViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences(
        BlackoutOverlayService.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Flows representing DB content
    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Interactive States
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _isBlackedOut = MutableStateFlow(false)
    val isBlackedOut: StateFlow<Boolean> = _isBlackedOut.asStateFlow()

    private val _sensorState = MutableStateFlow("Standby (Face-up)")
    val sensorState: StateFlow<String> = _sensorState.asStateFlow()

    // Preferences states
    private val _useProximity = MutableStateFlow(true)
    val useProximity: StateFlow<Boolean> = _useProximity.asStateFlow()

    private val _useAccel = MutableStateFlow(true)
    val useAccel: StateFlow<Boolean> = _useAccel.asStateFlow()

    private val _accelThreshold = MutableStateFlow(BlackoutOverlayService.DEFAULT_ACCEL_THRESHOLD)
    val accelThreshold: StateFlow<Float> = _accelThreshold.asStateFlow()

    private val _showFloatingShortcut = MutableStateFlow(false)
    val showFloatingShortcut: StateFlow<Boolean> = _showFloatingShortcut.asStateFlow()

    private val _floatingShortcutOpacity = MutableStateFlow(BlackoutOverlayService.DEFAULT_FLOATING_SHORTCUT_OPACITY)
    val floatingShortcutOpacity: StateFlow<Float> = _floatingShortcutOpacity.asStateFlow()

    // Current web browser state
    private val _currentUrl = MutableStateFlow("https://m.youtube.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BlackoutOverlayService.BROADCAST_STATUS) {
                _isMonitoring.value = intent.getBooleanExtra(BlackoutOverlayService.EXTRA_IS_MONITORING, false)
                _isBlackedOut.value = intent.getBooleanExtra(BlackoutOverlayService.EXTRA_IS_BLACKED_OUT, false)
                _sensorState.value = intent.getStringExtra(BlackoutOverlayService.EXTRA_SENSOR_STATE) ?: "Standby"
            }
        }
    }

    init {
        _useProximity.value = prefs.getBoolean(BlackoutOverlayService.KEY_USE_PROXIMITY, true)
        _useAccel.value = prefs.getBoolean(BlackoutOverlayService.KEY_USE_ACCEL, true)
        _accelThreshold.value = prefs.getFloat(
            BlackoutOverlayService.KEY_ACCEL_THRESHOLD,
            BlackoutOverlayService.DEFAULT_ACCEL_THRESHOLD
        )
        _showFloatingShortcut.value = prefs.getBoolean(BlackoutOverlayService.KEY_SHOW_FLOATING_SHORTCUT, false)
        _floatingShortcutOpacity.value = prefs.getFloat(
            BlackoutOverlayService.KEY_FLOATING_SHORTCUT_OPACITY,
            BlackoutOverlayService.DEFAULT_FLOATING_SHORTCUT_OPACITY
        )
        
        // Register Broadcast Receiver for service status updates
        val filter = IntentFilter(BlackoutOverlayService.BROADCAST_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            application.registerReceiver(receiver, filter)
        }
    }

    fun toggleService() {
        val context = getApplication<Application>()
        val intent = Intent(context, BlackoutOverlayService::class.java)
        if (_isMonitoring.value) {
            intent.action = BlackoutOverlayService.ACTION_STOP
            context.startService(intent)
            _isMonitoring.value = false
            _isBlackedOut.value = false
        } else {
            // Check Overlay Permission first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Settings.canDrawOverlays(context)) {
                _sensorState.value = "Overlay permission required"
                return
            }
            intent.action = BlackoutOverlayService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isMonitoring.value = true
        }
    }

    fun triggerManualBlackout() {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Settings.canDrawOverlays(context)) {
            _sensorState.value = "Overlay permission required"
            return
        }
        val intent = Intent(context, BlackoutOverlayService::class.java).apply {
            action = BlackoutOverlayService.ACTION_FORCE_BLACKOUT_TOGGLE
        }
        context.startService(intent)
    }

    fun updateUseProximity(enabled: Boolean) {
        _useProximity.value = enabled
        prefs.edit().putBoolean(BlackoutOverlayService.KEY_USE_PROXIMITY, enabled).apply()
        restartServiceIfRunning()
    }

    fun updateUseAccel(enabled: Boolean) {
        _useAccel.value = enabled
        prefs.edit().putBoolean(BlackoutOverlayService.KEY_USE_ACCEL, enabled).apply()
        restartServiceIfRunning()
    }

    fun updateAccelThreshold(value: Float) {
        _accelThreshold.value = value
        prefs.edit().putFloat(BlackoutOverlayService.KEY_ACCEL_THRESHOLD, value).apply()
    }

    fun updateShowFloatingShortcut(enabled: Boolean) {
        _showFloatingShortcut.value = enabled
        prefs.edit().putBoolean(BlackoutOverlayService.KEY_SHOW_FLOATING_SHORTCUT, enabled).apply()
    }

    fun updateFloatingShortcutOpacity(value: Float) {
        _floatingShortcutOpacity.value = value
        prefs.edit().putFloat(BlackoutOverlayService.KEY_FLOATING_SHORTCUT_OPACITY, value).apply()
    }

    private fun restartServiceIfRunning() {
        if (_isMonitoring.value) {
            val context = getApplication<Application>()
            val stopIntent = Intent(context, BlackoutOverlayService::class.java).apply {
                action = BlackoutOverlayService.ACTION_STOP
            }
            context.startService(stopIntent)
            
            val startIntent = Intent(context, BlackoutOverlayService::class.java).apply {
                action = BlackoutOverlayService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }
    }

    // Bookmark DB Actions
    fun addBookmark(name: String, url: String) {
        viewModelScope.launch {
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            repository.insertBookmark(Bookmark(name = name, url = formattedUrl, iconType = "globe"))
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
        }
    }

    // History log Actions
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun updateBrowserUrl(url: String) {
        _currentUrl.value = url
    }

    override fun onCleared() {
        try {
            getApplication<Application>().unregisterReceiver(receiver)
        } catch (e: Exception) {
            // already unregistered
        }
        super.onCleared()
    }
}

class BlackoutViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlackoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BlackoutViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
