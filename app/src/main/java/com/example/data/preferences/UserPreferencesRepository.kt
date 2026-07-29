package com.example.data.preferences

import android.content.Context
import android.media.AudioManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val buttonSizeDp: Int = 56,
    val buttonOpacity: Float = 0.85f,
    val edgeSnapEnabled: Boolean = true,
    val buttonX: Int = -1,
    val buttonY: Int = -1,
    val audioStream: Int = AudioManager.STREAM_MUSIC,
    val singleTapAction: String = "POPUP", // POPUP, VOL_UP, VOL_DOWN, MUTE, LOCK_SCREEN, SCREENSHOT, FLASHLIGHT, NOTIFICATION_SHADE, SETTINGS, DISABLED
    val doubleTapAction: String = "LOCK_SCREEN",
    val tripleTapAction: String = "FLASHLIGHT",
    val longPressAction: String = "SCREENSHOT",
    val popupTimeoutSeconds: Int = 5, // 0 means off/never
    val hapticFeedback: Boolean = true,
    val persistentNotification: Boolean = true,
    val startOnBoot: Boolean = false,
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val savedMuteVolume: Int = -1,

    // Safety & Automation
    val safetyLimiterEnabled: Boolean = true,
    val maxSafetyVolumePercent: Int = 60,
    val headphoneAutoProfileEnabled: Boolean = true,
    val headphoneTargetVolumePercent: Int = 50,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "23:00",
    val quietHoursEnd: String = "07:00",
    val quietHoursMode: String = "VIBRATE", // VIBRATE, SILENT, MUTE_MEDIA

    // Customization & Skins
    val buttonSkin: String = "ASSISTIVE_TOUCH", // ASSISTIVE_TOUCH, MINIMAL_DOT, GLASSMORPHIC_ORB, CYBERPUNK_NEON
    val autoDimOnIdle: Boolean = true,
    val idleDimOpacity: Float = 0.25f
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val BUTTON_SIZE = intPreferencesKey("button_size_dp")
        val BUTTON_OPACITY = floatPreferencesKey("button_opacity")
        val EDGE_SNAP = booleanPreferencesKey("edge_snap_enabled")
        val BUTTON_X = intPreferencesKey("button_x")
        val BUTTON_Y = intPreferencesKey("button_y")
        val AUDIO_STREAM = intPreferencesKey("audio_stream")
        val SINGLE_TAP_ACTION = stringPreferencesKey("single_tap_action")
        val DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")
        val TRIPLE_TAP_ACTION = stringPreferencesKey("triple_tap_action")
        val LONG_PRESS_ACTION = stringPreferencesKey("long_press_action")
        val POPUP_TIMEOUT = intPreferencesKey("popup_timeout_seconds")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val PERSISTENT_NOTIFICATION = booleanPreferencesKey("persistent_notification")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SAVED_MUTE_VOLUME = intPreferencesKey("saved_mute_volume")

        val SAFETY_LIMITER_ENABLED = booleanPreferencesKey("safety_limiter_enabled")
        val MAX_SAFETY_VOLUME_PERCENT = intPreferencesKey("max_safety_volume_percent")
        val HEADPHONE_AUTO_PROFILE_ENABLED = booleanPreferencesKey("headphone_auto_profile_enabled")
        val HEADPHONE_TARGET_VOLUME_PERCENT = intPreferencesKey("headphone_target_volume_percent")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
        val QUIET_HOURS_MODE = stringPreferencesKey("quiet_hours_mode")

        val BUTTON_SKIN = stringPreferencesKey("button_skin")
        val AUTO_DIM_ON_IDLE = booleanPreferencesKey("auto_dim_on_idle")
        val IDLE_DIM_OPACITY = floatPreferencesKey("idle_dim_opacity")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            buttonSizeDp = prefs[PreferencesKeys.BUTTON_SIZE] ?: 56,
            buttonOpacity = prefs[PreferencesKeys.BUTTON_OPACITY] ?: 0.85f,
            edgeSnapEnabled = prefs[PreferencesKeys.EDGE_SNAP] ?: true,
            buttonX = prefs[PreferencesKeys.BUTTON_X] ?: -1,
            buttonY = prefs[PreferencesKeys.BUTTON_Y] ?: -1,
            audioStream = prefs[PreferencesKeys.AUDIO_STREAM] ?: AudioManager.STREAM_MUSIC,
            singleTapAction = prefs[PreferencesKeys.SINGLE_TAP_ACTION] ?: "POPUP",
            doubleTapAction = prefs[PreferencesKeys.DOUBLE_TAP_ACTION] ?: "LOCK_SCREEN",
            tripleTapAction = prefs[PreferencesKeys.TRIPLE_TAP_ACTION] ?: "FLASHLIGHT",
            longPressAction = prefs[PreferencesKeys.LONG_PRESS_ACTION] ?: "SCREENSHOT",
            popupTimeoutSeconds = prefs[PreferencesKeys.POPUP_TIMEOUT] ?: 5,
            hapticFeedback = prefs[PreferencesKeys.HAPTIC_FEEDBACK] ?: true,
            persistentNotification = prefs[PreferencesKeys.PERSISTENT_NOTIFICATION] ?: true,
            startOnBoot = prefs[PreferencesKeys.START_ON_BOOT] ?: false,
            themeMode = prefs[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            savedMuteVolume = prefs[PreferencesKeys.SAVED_MUTE_VOLUME] ?: -1,

            safetyLimiterEnabled = prefs[PreferencesKeys.SAFETY_LIMITER_ENABLED] ?: true,
            maxSafetyVolumePercent = prefs[PreferencesKeys.MAX_SAFETY_VOLUME_PERCENT] ?: 60,
            headphoneAutoProfileEnabled = prefs[PreferencesKeys.HEADPHONE_AUTO_PROFILE_ENABLED] ?: true,
            headphoneTargetVolumePercent = prefs[PreferencesKeys.HEADPHONE_TARGET_VOLUME_PERCENT] ?: 50,
            quietHoursEnabled = prefs[PreferencesKeys.QUIET_HOURS_ENABLED] ?: false,
            quietHoursStart = prefs[PreferencesKeys.QUIET_HOURS_START] ?: "23:00",
            quietHoursEnd = prefs[PreferencesKeys.QUIET_HOURS_END] ?: "07:00",
            quietHoursMode = prefs[PreferencesKeys.QUIET_HOURS_MODE] ?: "VIBRATE",

            buttonSkin = prefs[PreferencesKeys.BUTTON_SKIN] ?: "ASSISTIVE_TOUCH",
            autoDimOnIdle = prefs[PreferencesKeys.AUTO_DIM_ON_IDLE] ?: true,
            idleDimOpacity = prefs[PreferencesKeys.IDLE_DIM_OPACITY] ?: 0.25f
        )
    }

    suspend fun updateButtonSize(sizeDp: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.BUTTON_SIZE] = sizeDp }
    }

    suspend fun updateButtonOpacity(opacity: Float) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.BUTTON_OPACITY] = opacity }
    }

    suspend fun updateEdgeSnap(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.EDGE_SNAP] = enabled }
    }

    suspend fun updateButtonPosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.BUTTON_X] = x
            prefs[PreferencesKeys.BUTTON_Y] = y
        }
    }

    suspend fun updateAudioStream(stream: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.AUDIO_STREAM] = stream }
    }

    suspend fun updateSingleTapAction(action: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SINGLE_TAP_ACTION] = action }
    }

    suspend fun updateDoubleTapAction(action: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.DOUBLE_TAP_ACTION] = action }
    }

    suspend fun updateTripleTapAction(action: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.TRIPLE_TAP_ACTION] = action }
    }

    suspend fun updateLongPressAction(action: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.LONG_PRESS_ACTION] = action }
    }

    suspend fun updateSafetyLimiterEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SAFETY_LIMITER_ENABLED] = enabled }
    }

    suspend fun updateMaxSafetyVolumePercent(percent: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.MAX_SAFETY_VOLUME_PERCENT] = percent }
    }

    suspend fun updateHeadphoneAutoProfileEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.HEADPHONE_AUTO_PROFILE_ENABLED] = enabled }
    }

    suspend fun updateHeadphoneTargetVolumePercent(percent: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.HEADPHONE_TARGET_VOLUME_PERCENT] = percent }
    }

    suspend fun updateQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.QUIET_HOURS_ENABLED] = enabled }
    }

    suspend fun updateQuietHoursStart(timeStr: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.QUIET_HOURS_START] = timeStr }
    }

    suspend fun updateQuietHoursEnd(timeStr: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.QUIET_HOURS_END] = timeStr }
    }

    suspend fun updateQuietHoursMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.QUIET_HOURS_MODE] = mode }
    }

    suspend fun updateButtonSkin(skin: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.BUTTON_SKIN] = skin }
    }

    suspend fun updateAutoDimOnIdle(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.AUTO_DIM_ON_IDLE] = enabled }
    }

    suspend fun updateIdleDimOpacity(opacity: Float) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.IDLE_DIM_OPACITY] = opacity }
    }

    suspend fun updatePopupTimeout(seconds: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.POPUP_TIMEOUT] = seconds }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun updatePersistentNotification(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.PERSISTENT_NOTIFICATION] = enabled }
    }

    suspend fun updateStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.START_ON_BOOT] = enabled }
    }

    suspend fun updateThemeMode(theme: String) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.THEME_MODE] = theme }
    }

    suspend fun updateSavedMuteVolume(vol: Int) {
        context.dataStore.edit { prefs -> prefs[PreferencesKeys.SAVED_MUTE_VOLUME] = vol }
    }
}
