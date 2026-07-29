package com.example.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.audio.AudioStreamInfo
import com.example.audio.AudioVolumeManager
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.VolumeAssistantTheme

class VolumePopupOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    private val viewModelStoreOwner: androidx.lifecycle.ViewModelStoreOwner,
    private val savedStateRegistryOwner: androidx.savedstate.SavedStateRegistryOwner,
    private val audioVolumeManager: AudioVolumeManager,
    private val onOpenSettings: () -> Unit,
    private val onDismiss: () -> Unit
) {

    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    fun show(
        buttonX: Int,
        buttonY: Int,
        buttonSizeDp: Int,
        preferences: UserPreferences
    ) {
        if (composeView != null) {
            dismiss()
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val popupWidthPx = (310 * density).toInt()
        val popupHeightPx = (250 * density).toInt()
        val buttonSizePx = (buttonSizeDp * density).toInt()

        // Intelligent positioning relative to button
        var popupX = if (buttonX + (buttonSizePx / 2) < screenWidth / 2) {
            // Button is on left -> open popup to right
            buttonX + buttonSizePx + (8 * density).toInt()
        } else {
            // Button is on right -> open popup to left
            buttonX - popupWidthPx - (8 * density).toInt()
        }

        var popupY = buttonY - (30 * density).toInt()

        // Keep inside screen bounds
        popupX = popupX.coerceIn((12 * density).toInt(), screenWidth - popupWidthPx - (12 * density).toInt())
        popupY = popupY.coerceIn((36 * density).toInt(), screenHeight - popupHeightPx - (48 * density).toInt())

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    @Suppress("DEPRECATION")
                    val modes = windowManager.defaultDisplay.supportedModes
                    val highRefreshMode = modes.maxByOrNull { it.refreshRate }
                    if (highRefreshMode != null) {
                        preferredDisplayModeId = highRefreshMode.modeId
                    }
                    @Suppress("DEPRECATION")
                    preferredRefreshRate = 120f
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

            setContent {
                VolumeAssistantTheme {
                    PopupContainer(
                        popupX = popupX,
                        popupY = popupY,
                        preferences = preferences,
                        audioVolumeManager = audioVolumeManager,
                        onOpenSettings = {
                            dismiss()
                            onOpenSettings()
                        },
                        onDismiss = { dismiss() },
                        onResetTimeout = { scheduleAutoDismiss(preferences.popupTimeoutSeconds) }
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            scheduleAutoDismiss(preferences.popupTimeoutSeconds)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleAutoDismiss(timeoutSeconds: Int) {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        if (timeoutSeconds > 0) {
            dismissRunnable = Runnable { dismiss() }
            handler.postDelayed(dismissRunnable!!, timeoutSeconds * 1000L)
        }
    }

    fun dismiss() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null
        params = null
        onDismiss()
    }
}

@Composable
private fun PopupContainer(
    popupX: Int,
    popupY: Int,
    preferences: UserPreferences,
    audioVolumeManager: AudioVolumeManager,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    onResetTimeout: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    var selectedStream by remember { mutableIntStateOf(preferences.audioStream) }
    var streamInfo by remember { mutableStateOf(audioVolumeManager.getStreamInfo(selectedStream)) }
    var savedMuteVol by remember { mutableIntStateOf(preferences.savedMuteVolume) }

    // Live update observation for active stream
    LaunchedEffect(selectedStream) {
        audioVolumeManager.observeStreamVolume(selectedStream).collect { updated ->
            streamInfo = updated
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.88f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.88f),
            modifier = Modifier
                .offset { IntOffset(popupX, popupY) }
                .width(310.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, shape = RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Prevent click propagation to background
                        onResetTimeout()
                    },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xF21C1C1E) // iOS Dark Frosted AssistiveTouch Background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Stream selector row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AudioVolumeManager.SUPPORTED_STREAMS.forEach { (streamType, name) ->
                            val isSelected = streamType == selectedStream
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedStream = streamType
                                    streamInfo = audioVolumeManager.getStreamInfo(streamType)
                                    onResetTimeout()
                                },
                                label = {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0A84FF),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0x26767680),
                                    labelColor = Color(0xE6FFFFFF)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title + Volume Percentage Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0x330A84FF),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (streamInfo.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = Color(0xFF0A84FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "${streamInfo.streamName} Volume",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Level ${streamInfo.currentVolume} / ${streamInfo.maxVolume}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0x99EBEBF5)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0x26767680),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "${streamInfo.volumePercentage}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64D2FF),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Decrement / Slider / Increment Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                streamInfo = audioVolumeManager.volumeDown(selectedStream)
                                onResetTimeout()
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0x26767680),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Volume Down")
                        }

                        Slider(
                            value = streamInfo.currentVolume.toFloat(),
                            onValueChange = { newValue ->
                                audioVolumeManager.setVolume(selectedStream, newValue.toInt())
                                streamInfo = audioVolumeManager.getStreamInfo(selectedStream)
                                onResetTimeout()
                            },
                            valueRange = streamInfo.minVolume.toFloat()..streamInfo.maxVolume.toFloat(),
                            steps = (streamInfo.maxVolume - streamInfo.minVolume - 1).coerceAtLeast(0),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF0A84FF),
                                inactiveTrackColor = Color(0x33767680)
                            )
                        )

                        IconButton(
                            onClick = {
                                streamInfo = audioVolumeManager.volumeUp(selectedStream)
                                onResetTimeout()
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0x26767680),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Volume Up")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Action Row: Mute/Unmute & Settings / Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                val (newInfo, newSavedVol) = audioVolumeManager.toggleMute(selectedStream, savedMuteVol)
                                streamInfo = newInfo
                                savedMuteVol = newSavedVol
                                onResetTimeout()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (streamInfo.isMuted) Color(0x40FF453A) else Color(0x26767680),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (streamInfo.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Mute Unmute",
                                    tint = if (streamInfo.isMuted) Color(0xFFFF453A) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (streamInfo.isMuted) "Unmute" else "Mute",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (streamInfo.isMuted) Color(0xFFFF453A) else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { onOpenSettings() },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF0A84FF))
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }

                        IconButton(
                            onClick = { onDismiss() },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0x99EBEBF5))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Popup"
                            )
                        }
                    }
                }
            }
        }
    }
}
