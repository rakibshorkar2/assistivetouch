package com.example.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.VolumeAssistantTheme
import kotlin.math.abs
import kotlin.math.hypot

class FloatingButtonOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    private val viewModelStoreOwner: androidx.lifecycle.ViewModelStoreOwner,
    private val savedStateRegistryOwner: androidx.savedstate.SavedStateRegistryOwner,
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onTripleTap: () -> Unit,
    private val onLongPress: () -> Unit,
    private val onPositionChanged: (Int, Int) -> Unit
) {

    private var composeView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())

    private var initialX = 0
    private var initialY = 0
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isDragging = false
    private var hasTriggeredLongPress = false

    private var tapCount = 0
    private var tapRunnable: Runnable? = null
    private var longPressRunnable: Runnable? = null
    private var idleDimRunnable: Runnable? = null

    private val currentOpacityState = mutableStateOf(0.85f)

    var currentX: Int = 100
        private set
    var currentY: Int = 300
        private set

    var currentPreferences: UserPreferences = UserPreferences()
        private set

    fun show(preferences: UserPreferences) {
        currentPreferences = preferences
        currentOpacityState.value = preferences.buttonOpacity
        if (composeView != null) {
            updatePreferences(preferences)
            return
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sizePx = (preferences.buttonSizeDp * context.resources.displayMetrics.density).toInt()

        // Calculate initial position
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        currentX = if (preferences.buttonX >= 0) preferences.buttonX else (screenWidth - sizePx - 20)
        currentY = if (preferences.buttonY >= 0) preferences.buttonY else (screenHeight / 3)

        params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentX
            y = currentY
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
                    val opacity by currentOpacityState
                    FloatingButtonContent(
                        sizeDp = currentPreferences.buttonSizeDp,
                        opacity = opacity,
                        skin = currentPreferences.buttonSkin
                    )
                }
            }

            setOnTouchListener { _, event ->
                handleTouch(event)
            }
        }

        try {
            windowManager.addView(composeView, params)
            scheduleIdleDimming()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updatePreferences(preferences: UserPreferences) {
        currentPreferences = preferences
        currentOpacityState.value = preferences.buttonOpacity
        val sizePx = (preferences.buttonSizeDp * context.resources.displayMetrics.density).toInt()
        params?.let { p ->
            p.width = sizePx
            p.height = sizePx
            try {
                windowManager.updateViewLayout(composeView, p)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView?.setContent {
            VolumeAssistantTheme {
                val opacity by currentOpacityState
                FloatingButtonContent(
                    sizeDp = currentPreferences.buttonSizeDp,
                    opacity = opacity,
                    skin = currentPreferences.buttonSkin
                )
            }
        }
        scheduleIdleDimming()
    }

    private fun resetOpacityOnTouch() {
        idleDimRunnable?.let { handler.removeCallbacks(it) }
        currentOpacityState.value = currentPreferences.buttonOpacity
    }

    private fun scheduleIdleDimming() {
        idleDimRunnable?.let { handler.removeCallbacks(it) }
        if (currentPreferences.autoDimOnIdle) {
            idleDimRunnable = Runnable {
                currentOpacityState.value = currentPreferences.idleDimOpacity
            }
            handler.postDelayed(idleDimRunnable!!, 3000)
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        val p = params ?: return false
        val touchSlop = (ViewConfiguration.get(context).scaledTouchSlop / 3).coerceAtLeast(6)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetOpacityOnTouch()
                initialX = p.x
                initialY = p.y
                touchStartX = event.rawX
                touchStartY = event.rawY
                isDragging = false
                hasTriggeredLongPress = false

                // Schedule long press
                longPressRunnable = Runnable {
                    if (!isDragging) {
                        hasTriggeredLongPress = true
                        tapCount = 0
                        tapRunnable?.let { handler.removeCallbacks(it) }
                        triggerHapticFeedback()
                        onLongPress()
                    }
                }
                handler.postDelayed(longPressRunnable!!, ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - touchStartX).toInt()
                val deltaY = (event.rawY - touchStartY).toInt()

                if (!isDragging && hypot(deltaX.toDouble(), deltaY.toDouble()) > touchSlop) {
                    isDragging = true
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    tapCount = 0
                    tapRunnable?.let { handler.removeCallbacks(it) }
                }

                if (isDragging) {
                    val displayMetrics = context.resources.displayMetrics
                    val maxX = (displayMetrics.widthPixels - p.width).coerceAtLeast(0)
                    val maxY = (displayMetrics.heightPixels - p.height).coerceAtLeast(0)

                    p.x = (initialX + deltaX).coerceIn(0, maxX)
                    p.y = (initialY + deltaY).coerceIn(0, maxY)

                    currentX = p.x
                    currentY = p.y

                    try {
                        windowManager.updateViewLayout(composeView, p)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }

                if (isDragging) {
                    if (currentPreferences.edgeSnapEnabled) {
                        snapToNearestEdge()
                    } else {
                        onPositionChanged(p.x, p.y)
                    }
                } else if (hasTriggeredLongPress) {
                    // Long press already handled
                } else {
                    // Multi-tap detection logic (Single, Double, Triple)
                    tapCount++
                    tapRunnable?.let { handler.removeCallbacks(it) }

                    if (tapCount >= 3) {
                        tapCount = 0
                        if (currentPreferences.hapticFeedback) triggerHapticFeedback()
                        onTripleTap()
                    } else {
                        tapRunnable = Runnable {
                            val count = tapCount
                            tapCount = 0
                            if (currentPreferences.hapticFeedback) triggerHapticFeedback()
                            if (count == 1) {
                                onSingleTap()
                            } else if (count == 2) {
                                onDoubleTap()
                            }
                        }
                        handler.postDelayed(tapRunnable!!, 280)
                    }
                }
                scheduleIdleDimming()
                return true
            }
        }
        return false
    }

    private fun snapToNearestEdge() {
        val p = params ?: return
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val targetX = if (p.x + (p.width / 2) < screenWidth / 2) {
            12 // Snap to left margin
        } else {
            screenWidth - p.width - 12 // Snap to right margin
        }

        val startX = p.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                p.x = anim.animatedValue as Int
                currentX = p.x
                try {
                    windowManager.updateViewLayout(composeView, p)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        animator.start()
        onPositionChanged(targetX, p.y)
    }

    private fun triggerHapticFeedback() {
        if (!currentPreferences.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(20)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismiss() {
        longPressRunnable?.let { handler.removeCallbacks(it) }
        tapRunnable?.let { handler.removeCallbacks(it) }
        idleDimRunnable?.let { handler.removeCallbacks(it) }
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null
        params = null
    }
}

@Composable
fun FloatingButtonContent(
    sizeDp: Int,
    opacity: Float,
    skin: String = "ASSISTIVE_TOUCH"
) {
    val cornerRadiusDp = (sizeDp * 0.28f).dp

    Surface(
        modifier = Modifier
            .size(sizeDp.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(cornerRadiusDp),
                clip = false
            )
            .alpha(opacity),
        shape = RoundedCornerShape(cornerRadiusDp),
        color = Color.Unspecified
    ) {
        when (skin) {
            "MINIMAL_DOT" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xCC000000),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0x40FFFFFF),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((sizeDp * 0.40f).dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
            }
            "GLASSMORPHIC_ORB" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x803A86FF),
                                    Color(0xCC0F172A)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0x80FFFFFF), Color(0x1A3A86FF))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((sizeDp * 0.35f).dp)
                            .background(
                                color = Color(0xEFF1F5F9),
                                shape = CircleShape
                            )
                    )
                }
            }
            "CYBERPUNK_NEON" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xF20D0221),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF00F5FF),
                                    Color(0xFFFF007F),
                                    Color(0xFF00F5FF)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((sizeDp * 0.42f).dp)
                            .background(
                                color = Color(0xFF00F5FF),
                                shape = CircleShape
                            )
                    )
                }
            }
            else -> { // "ASSISTIVE_TOUCH" default
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xEC1C1C1E),
                            shape = RoundedCornerShape(cornerRadiusDp)
                        )
                        .border(
                            width = 1.2.dp,
                            color = Color(0x33FFFFFF),
                            shape = RoundedCornerShape(cornerRadiusDp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Layer 1
                    Box(
                        modifier = Modifier
                            .size((sizeDp * 0.76f).dp)
                            .background(color = Color.Transparent, shape = CircleShape)
                            .border(
                                width = 1.6.dp,
                                color = Color.White.copy(alpha = 0.20f),
                                shape = CircleShape
                            )
                    )
                    // Layer 2
                    Box(
                        modifier = Modifier
                            .size((sizeDp * 0.58f).dp)
                            .background(color = Color.Transparent, shape = CircleShape)
                            .border(
                                width = 1.8.dp,
                                color = Color.White.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                    )
                    // Layer 3
                    Box(
                        modifier = Modifier
                            .size((sizeDp * 0.44f).dp)
                            .background(color = Color.Transparent, shape = CircleShape)
                            .border(
                                width = 2.0.dp,
                                color = Color.White.copy(alpha = 0.65f),
                                shape = CircleShape
                            )
                    )
                    // Center White Circle
                    Box(
                        modifier = Modifier
                            .size((sizeDp * 0.32f).dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
