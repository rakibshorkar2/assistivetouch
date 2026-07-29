package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserPreferencesRepository(application)

    val userPreferences: StateFlow<UserPreferences> = repository.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    fun setButtonSizeDp(sizeDp: Int) = viewModelScope.launch {
        repository.updateButtonSize(sizeDp)
    }

    fun setButtonOpacity(opacity: Float) = viewModelScope.launch {
        repository.updateButtonOpacity(opacity)
    }

    fun setEdgeSnap(enabled: Boolean) = viewModelScope.launch {
        repository.updateEdgeSnap(enabled)
    }

    fun setAudioStream(streamType: Int) = viewModelScope.launch {
        repository.updateAudioStream(streamType)
    }

    fun setSingleTapAction(action: String) = viewModelScope.launch {
        repository.updateSingleTapAction(action)
    }

    fun setDoubleTapAction(action: String) = viewModelScope.launch {
        repository.updateDoubleTapAction(action)
    }

    fun setTripleTapAction(action: String) = viewModelScope.launch {
        repository.updateTripleTapAction(action)
    }

    fun setLongPressAction(action: String) = viewModelScope.launch {
        repository.updateLongPressAction(action)
    }

    fun setSafetyLimiterEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateSafetyLimiterEnabled(enabled)
    }

    fun setMaxSafetyVolumePercent(percent: Int) = viewModelScope.launch {
        repository.updateMaxSafetyVolumePercent(percent)
    }

    fun setHeadphoneAutoProfileEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateHeadphoneAutoProfileEnabled(enabled)
    }

    fun setHeadphoneTargetVolumePercent(percent: Int) = viewModelScope.launch {
        repository.updateHeadphoneTargetVolumePercent(percent)
    }

    fun setQuietHoursEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateQuietHoursEnabled(enabled)
    }

    fun setQuietHoursStart(timeStr: String) = viewModelScope.launch {
        repository.updateQuietHoursStart(timeStr)
    }

    fun setQuietHoursEnd(timeStr: String) = viewModelScope.launch {
        repository.updateQuietHoursEnd(timeStr)
    }

    fun setQuietHoursMode(mode: String) = viewModelScope.launch {
        repository.updateQuietHoursMode(mode)
    }

    fun setButtonSkin(skin: String) = viewModelScope.launch {
        repository.updateButtonSkin(skin)
    }

    fun setAutoDimOnIdle(enabled: Boolean) = viewModelScope.launch {
        repository.updateAutoDimOnIdle(enabled)
    }

    fun setIdleDimOpacity(opacity: Float) = viewModelScope.launch {
        repository.updateIdleDimOpacity(opacity)
    }

    fun setPopupTimeout(seconds: Int) = viewModelScope.launch {
        repository.updatePopupTimeout(seconds)
    }

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        repository.updateHapticFeedback(enabled)
    }

    fun setPersistentNotification(enabled: Boolean) = viewModelScope.launch {
        repository.updatePersistentNotification(enabled)
    }

    fun setStartOnBoot(enabled: Boolean) = viewModelScope.launch {
        repository.updateStartOnBoot(enabled)
    }

    fun setThemeMode(theme: String) = viewModelScope.launch {
        repository.updateThemeMode(theme)
    }
}
