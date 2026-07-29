package com.example.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioStreamInfo
import com.example.audio.AudioVolumeManager
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.service.FloatingVolumeService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository(application)
    private val audioManager = AudioVolumeManager(application)

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    private val _isServiceRunning = MutableStateFlow(FloatingVolumeService.isRunning)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _overlayPermissionGranted = MutableStateFlow(Settings.canDrawOverlays(application))
    val overlayPermissionGranted: StateFlow<Boolean> = _overlayPermissionGranted.asStateFlow()

    private val _notificationPermissionGranted = MutableStateFlow(checkNotificationPermission(application))
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()

    private val _dndPolicyGranted = MutableStateFlow(checkDndPolicyGranted(application))
    val dndPolicyGranted: StateFlow<Boolean> = _dndPolicyGranted.asStateFlow()

    private val _batteryOptimizationIgnored = MutableStateFlow(checkBatteryOptimizationIgnored(application))
    val batteryOptimizationIgnored: StateFlow<Boolean> = _batteryOptimizationIgnored.asStateFlow()

    private val _currentStreamInfo = MutableStateFlow(audioManager.getStreamInfo(AudioManager.STREAM_MUSIC))
    val currentStreamInfo: StateFlow<AudioStreamInfo> = _currentStreamInfo.asStateFlow()

    init {
        // Observe current active stream volume changes in real-time
        viewModelScope.launch {
            userPreferences
                .flatMapLatest { prefs -> audioManager.observeStreamVolume(prefs.audioStream) }
                .collect { info ->
                    _currentStreamInfo.value = info
                }
        }
    }

    fun refreshState() {
        val app = getApplication<Application>()
        _isServiceRunning.value = FloatingVolumeService.isRunning
        _overlayPermissionGranted.value = Settings.canDrawOverlays(app)
        _notificationPermissionGranted.value = checkNotificationPermission(app)
        _dndPolicyGranted.value = checkDndPolicyGranted(app)
        _batteryOptimizationIgnored.value = checkBatteryOptimizationIgnored(app)
        _currentStreamInfo.value = audioManager.getStreamInfo(userPreferences.value.audioStream)
    }

    private fun checkNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkDndPolicyGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return nm?.isNotificationPolicyAccessGranted ?: false
    }

    private fun checkBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
    }

    fun startFloatingService() {
        val app = getApplication<Application>()
        if (!Settings.canDrawOverlays(app)) return

        val intent = Intent(app, FloatingVolumeService::class.java).apply {
            action = FloatingVolumeService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
        _isServiceRunning.value = true
    }

    fun stopFloatingService() {
        val app = getApplication<Application>()
        val intent = Intent(app, FloatingVolumeService::class.java).apply {
            action = FloatingVolumeService.ACTION_STOP
        }
        app.startService(intent)
        _isServiceRunning.value = false
    }

    fun testFloatingPopup() {
        val app = getApplication<Application>()
        if (FloatingVolumeService.isRunning) {
            val intent = Intent(app, FloatingVolumeService::class.java).apply {
                action = FloatingVolumeService.ACTION_SHOW_POPUP
            }
            app.startService(intent)
        } else {
            startFloatingService()
        }
    }

    fun setStreamVolume(streamType: Int, volume: Int) {
        audioManager.setVolume(streamType, volume)
        _currentStreamInfo.value = audioManager.getStreamInfo(streamType)
    }

    fun volumeUp(streamType: Int) {
        _currentStreamInfo.value = audioManager.volumeUp(streamType)
    }

    fun volumeDown(streamType: Int) {
        _currentStreamInfo.value = audioManager.volumeDown(streamType)
    }

    fun toggleMute(streamType: Int) {
        viewModelScope.launch {
            val (info, savedVol) = audioManager.toggleMute(streamType, userPreferences.value.savedMuteVolume)
            _currentStreamInfo.value = info
            repository.updateSavedMuteVolume(savedVol)
        }
    }
}
