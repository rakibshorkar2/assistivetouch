package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.MainActivity
import com.example.audio.AudioVolumeManager
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.overlay.FloatingButtonOverlay
import com.example.overlay.VolumePopupOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingVolumeService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val CHANNEL_ID = "volume_assistant_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START_FLOATING_ASSISTANT"
        const val ACTION_STOP = "ACTION_STOP_FLOATING_ASSISTANT"
        const val ACTION_SHOW_POPUP = "ACTION_SHOW_POPUP"
        const val ACTION_VOL_UP = "ACTION_VOL_UP"
        const val ACTION_VOL_DOWN = "ACTION_VOL_DOWN"
        const val ACTION_TOGGLE_MUTE = "ACTION_TOGGLE_MUTE"

        var isRunning = false
            private set
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var preferencesRepository: UserPreferencesRepository
    private lateinit var audioVolumeManager: AudioVolumeManager
    private lateinit var windowManager: WindowManager

    private var floatingButtonOverlay: FloatingButtonOverlay? = null
    private var volumePopupOverlay: VolumePopupOverlay? = null
    private var currentPreferences: UserPreferences = UserPreferences()
    private var isTorchOn = false

    private val audioStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1) {
                        onHeadphonesConnected()
                    }
                }
                android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(android.bluetooth.BluetoothProfile.EXTRA_STATE, -1)
                    if (state == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                        onHeadphonesConnected()
                    }
                }
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    checkVolumeSafetyLimiter()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        preferencesRepository = UserPreferencesRepository(applicationContext)
        audioVolumeManager = AudioVolumeManager(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()

        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }
        try {
            registerReceiver(audioStateReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        isRunning = true

        // Observe preferences in real-time
        serviceScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                currentPreferences = prefs
                floatingButtonOverlay?.updatePreferences(prefs)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (action) {
            ACTION_STOP -> {
                stopFloatingAssistant()
                return START_NOT_STICKY
            }
            ACTION_SHOW_POPUP -> {
                showVolumePopup()
            }
            ACTION_VOL_UP -> {
                audioVolumeManager.volumeUp(currentPreferences.audioStream)
            }
            ACTION_VOL_DOWN -> {
                audioVolumeManager.volumeDown(currentPreferences.audioStream)
            }
            ACTION_TOGGLE_MUTE -> {
                serviceScope.launch {
                    val (info, saved) = audioVolumeManager.toggleMute(
                        currentPreferences.audioStream,
                        currentPreferences.savedMuteVolume
                    )
                    preferencesRepository.updateSavedMuteVolume(saved)
                }
            }
            ACTION_START -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            startForeground(
                                NOTIFICATION_ID,
                                buildNotification(),
                                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                            )
                        } else {
                            startForeground(NOTIFICATION_ID, buildNotification())
                        }
                    } else {
                        startForeground(NOTIFICATION_ID, buildNotification())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setupFloatingOverlays()
            }
        }

        return START_STICKY
    }

    private fun setupFloatingOverlays() {
        if (!Settings.canDrawOverlays(this)) {
            return
        }
        if (floatingButtonOverlay != null) return

        serviceScope.launch {
            val prefs = preferencesRepository.userPreferencesFlow.first()
            currentPreferences = prefs

            withContext(Dispatchers.Main) {
                floatingButtonOverlay = FloatingButtonOverlay(
                    context = this@FloatingVolumeService,
                    windowManager = windowManager,
                    lifecycleOwner = this@FloatingVolumeService,
                    viewModelStoreOwner = this@FloatingVolumeService,
                    savedStateRegistryOwner = this@FloatingVolumeService,
                    onSingleTap = { handleGestureAction(currentPreferences.singleTapAction) },
                    onDoubleTap = { handleGestureAction(currentPreferences.doubleTapAction) },
                    onTripleTap = { handleGestureAction(currentPreferences.tripleTapAction) },
                    onLongPress = { handleGestureAction(currentPreferences.longPressAction) },
                    onPositionChanged = { x, y ->
                        serviceScope.launch {
                            preferencesRepository.updateButtonPosition(x, y)
                        }
                    }
                ).also {
                    it.show(prefs)
                }
            }
        }
    }

    private fun handleGestureAction(action: String) {
        when (action) {
            "POPUP" -> showVolumePopup()
            "VOL_UP" -> audioVolumeManager.volumeUp(currentPreferences.audioStream)
            "VOL_DOWN" -> audioVolumeManager.volumeDown(currentPreferences.audioStream)
            "MUTE" -> {
                serviceScope.launch {
                    val (info, saved) = audioVolumeManager.toggleMute(
                        currentPreferences.audioStream,
                        currentPreferences.savedMuteVolume
                    )
                    preferencesRepository.updateSavedMuteVolume(saved)
                }
            }
            "LOCK_SCREEN" -> performLockScreenAction()
            "SCREENSHOT" -> performScreenshotAction()
            "FLASHLIGHT" -> toggleFlashlight()
            "NOTIFICATION_SHADE" -> performNotificationShadeAction()
            "SETTINGS" -> openAppSettings()
            "DISABLED" -> { /* Do nothing */ }
        }
    }

    private fun toggleFlashlight() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
            android.widget.Toast.makeText(
                this,
                if (isTorchOn) "Flashlight ON" else "Flashlight OFF",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Flashlight unavailable", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun performNotificationShadeAction() {
        val success = AssistiveAccessibilityService.performNotificationShade()
        if (!success) {
            promptEnableAccessibility("Notification Shade")
        }
    }

    private fun onHeadphonesConnected() {
        if (currentPreferences.headphoneAutoProfileEnabled) {
            val maxVol = audioVolumeManager.getMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val targetVol = (maxVol * currentPreferences.headphoneTargetVolumePercent / 100f).toInt().coerceIn(0, maxVol)
            audioVolumeManager.setVolume(android.media.AudioManager.STREAM_MUSIC, targetVol)
            android.widget.Toast.makeText(
                this,
                "Headphones Connected: Auto-set media volume to ${currentPreferences.headphoneTargetVolumePercent}%",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        checkVolumeSafetyLimiter()
    }

    private fun checkVolumeSafetyLimiter() {
        if (!currentPreferences.safetyLimiterEnabled) return
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val isHeadsetConnected = audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn
        if (isHeadsetConnected) {
            val currentVol = audioVolumeManager.getCurrentVolume(android.media.AudioManager.STREAM_MUSIC)
            val maxVol = audioVolumeManager.getMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val allowedMax = (maxVol * currentPreferences.maxSafetyVolumePercent / 100f).toInt().coerceAtLeast(1)
            if (currentVol > allowedMax) {
                audioVolumeManager.setVolume(android.media.AudioManager.STREAM_MUSIC, allowedMax)
                android.widget.Toast.makeText(
                    this,
                    "Safety Limiter: Volume capped at ${currentPreferences.maxSafetyVolumePercent}% for ear protection",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun performLockScreenAction() {
        val success = AssistiveAccessibilityService.performLockScreen()
        if (!success) {
            promptEnableAccessibility("Lock Screen")
        }
    }

    private fun performScreenshotAction() {
        val success = AssistiveAccessibilityService.performScreenshot()
        if (!success) {
            promptEnableAccessibility("Take Screenshot")
        }
    }

    private fun promptEnableAccessibility(featureName: String) {
        android.widget.Toast.makeText(
            this,
            "$featureName requires Accessibility Service. Opening settings...",
            android.widget.Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showVolumePopup() {
        val buttonOverlay = floatingButtonOverlay ?: return
        if (volumePopupOverlay == null) {
            volumePopupOverlay = VolumePopupOverlay(
                context = this,
                windowManager = windowManager,
                lifecycleOwner = this,
                viewModelStoreOwner = this,
                savedStateRegistryOwner = this,
                audioVolumeManager = audioVolumeManager,
                onOpenSettings = { openAppSettings() },
                onDismiss = { volumePopupOverlay = null }
            )
        }
        volumePopupOverlay?.show(
            buttonX = buttonOverlay.currentX,
            buttonY = buttonOverlay.currentY,
            buttonSizeDp = currentPreferences.buttonSizeDp,
            preferences = currentPreferences
        )
    }

    private fun openAppSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "settings")
        }
        startActivity(intent)
    }

    private fun stopFloatingAssistant() {
        volumePopupOverlay?.dismiss()
        volumePopupOverlay = null
        floatingButtonOverlay?.dismiss()
        floatingButtonOverlay = null
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Volume Assistant Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps persistent floating volume controls active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingMainIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val popupIntent = Intent(this, FloatingVolumeService::class.java).apply {
            action = ACTION_SHOW_POPUP
        }
        val pendingPopupIntent = PendingIntent.getService(
            this, 1, popupIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingVolumeService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Volume Assistant")
            .setContentText("Floating audio control button is active")
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingMainIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(com.example.R.drawable.ic_launcher_foreground, "Controls", pendingPopupIntent)
            .addAction(com.example.R.drawable.ic_launcher_foreground, "Stop", pendingStopIntent)
            .build()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(audioStateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        volumePopupOverlay?.dismiss()
        floatingButtonOverlay?.dismiss()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        serviceScope.cancel()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
