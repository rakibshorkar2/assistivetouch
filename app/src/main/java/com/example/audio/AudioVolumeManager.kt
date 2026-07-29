package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class AudioStreamInfo(
    val streamType: Int,
    val streamName: String,
    val currentVolume: Int,
    val maxVolume: Int,
    val minVolume: Int,
    val isMuted: Boolean
) {
    val volumePercentage: Int
        get() {
            val range = maxVolume - minVolume
            if (range <= 0) return 0
            val normalized = (currentVolume - minVolume).coerceAtLeast(0)
            return ((normalized.toFloat() / range.toFloat()) * 100).toInt()
        }
}

class AudioVolumeManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    companion object {
        val SUPPORTED_STREAMS = listOf(
            AudioManager.STREAM_MUSIC to "Media",
            AudioManager.STREAM_RING to "Ring",
            AudioManager.STREAM_NOTIFICATION to "Notification",
            AudioManager.STREAM_ALARM to "Alarm",
            AudioManager.STREAM_VOICE_CALL to "Call"
        )

        fun getStreamName(streamType: Int): String {
            return when (streamType) {
                AudioManager.STREAM_MUSIC -> "Media"
                AudioManager.STREAM_RING -> "Ring"
                AudioManager.STREAM_NOTIFICATION -> "Notification"
                AudioManager.STREAM_ALARM -> "Alarm"
                AudioManager.STREAM_VOICE_CALL -> "Call"
                else -> "Media"
            }
        }
    }

    fun getMinVolume(streamType: Int): Int {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                audioManager.getStreamMinVolume(streamType)
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun getMaxVolume(streamType: Int): Int {
        return try {
            audioManager.getStreamMaxVolume(streamType)
        } catch (e: Exception) {
            15
        }
    }

    fun getCurrentVolume(streamType: Int): Int {
        return try {
            audioManager.getStreamVolume(streamType)
        } catch (e: Exception) {
            0
        }
    }

    fun isMuted(streamType: Int): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                audioManager.isStreamMute(streamType) || getCurrentVolume(streamType) == getMinVolume(streamType)
            } else {
                getCurrentVolume(streamType) == getMinVolume(streamType)
            }
        } catch (e: Exception) {
            getCurrentVolume(streamType) == 0
        }
    }

    fun getStreamInfo(streamType: Int): AudioStreamInfo {
        val current = getCurrentVolume(streamType)
        val max = getMaxVolume(streamType)
        val min = getMinVolume(streamType)
        val muted = isMuted(streamType)
        return AudioStreamInfo(
            streamType = streamType,
            streamName = getStreamName(streamType),
            currentVolume = current,
            maxVolume = max,
            minVolume = min,
            isMuted = muted
        )
    }

    fun setVolume(streamType: Int, volume: Int) {
        val min = getMinVolume(streamType)
        val max = getMaxVolume(streamType)
        val target = volume.coerceIn(min, max)
        try {
            audioManager.setStreamVolume(streamType, target, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun volumeUp(streamType: Int): AudioStreamInfo {
        try {
            audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, 0)
        } catch (e: Exception) {
            val current = getCurrentVolume(streamType)
            setVolume(streamType, current + 1)
        }
        return getStreamInfo(streamType)
    }

    fun volumeDown(streamType: Int): AudioStreamInfo {
        try {
            audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, 0)
        } catch (e: Exception) {
            val current = getCurrentVolume(streamType)
            setVolume(streamType, current - 1)
        }
        return getStreamInfo(streamType)
    }

    fun toggleMute(streamType: Int, savedVolume: Int): Pair<AudioStreamInfo, Int> {
        val current = getCurrentVolume(streamType)
        val min = getMinVolume(streamType)
        val max = getMaxVolume(streamType)

        return if (current > min) {
            // Mute it and return current volume so caller can save it
            try {
                audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_MUTE, 0)
            } catch (e: Exception) {
                setVolume(streamType, min)
            }
            getStreamInfo(streamType) to current
        } else {
            // Unmute: restore saved volume or set to default (e.g. 50% max)
            val restoreVol = if (savedVolume > min && savedVolume <= max) {
                savedVolume
            } else {
                ((max - min) / 2).coerceAtLeast(min + 1)
            }
            try {
                audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, 0)
                setVolume(streamType, restoreVol)
            } catch (e: Exception) {
                setVolume(streamType, restoreVol)
            }
            getStreamInfo(streamType) to restoreVol
        }
    }

    /**
     * Flow that emits stream info updates whenever volume changes on system in real-time.
     */
    fun observeStreamVolume(streamType: Int): Flow<AudioStreamInfo> = callbackFlow {
        // Emit initial value
        trySend(getStreamInfo(streamType))

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(getStreamInfo(streamType))
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(getStreamInfo(streamType))
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val filter = android.content.IntentFilter().apply {
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        awaitClose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
