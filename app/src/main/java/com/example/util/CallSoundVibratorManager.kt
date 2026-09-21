package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class CallSoundVibratorManager(private val context: Context) {

  private var mediaPlayer: MediaPlayer? = null
  private var fallbackRingtone: Ringtone? = null
  private var toneGenerator: ToneGenerator? = null
  private var toneThread: Thread? = null
  private var isRinging = false

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  fun startRingingAndVibration() {
    if (isRinging) return
    isRinging = true

    startRingtone()
    startVibration()
  }

  private fun startRingtone() {
    try {
      val ringtoneUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(
        context,
        RingtoneManager.TYPE_RINGTONE
      ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

      mediaPlayer = MediaPlayer().apply {
        setDataSource(context, ringtoneUri)
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        isLooping = true
        prepare()
        start()
      }
    } catch (e: Exception) {
      Log.w("CallSoundVibrator", "MediaPlayer failed for ringtone, trying fallback: ${e.message}")
      tryFallbackRingtone()
    }
  }

  private fun tryFallbackRingtone() {
    try {
      val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
      fallbackRingtone = RingtoneManager.getRingtone(context, defaultUri)?.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          isLooping = true
        }
        play()
      }
    } catch (e: Exception) {
      Log.w("CallSoundVibrator", "Fallback Ringtone failed, using tone generator: ${e.message}")
      startToneGeneratorFallback()
    }
  }

  private fun startToneGeneratorFallback() {
    try {
      toneGenerator = ToneGenerator(AudioManager.STREAM_RING, 85)
      toneThread = Thread {
        while (isRinging) {
          try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1500)
            Thread.sleep(3000)
          } catch (_: InterruptedException) {
            break
          }
        }
      }.apply {
        isDaemon = true
        start()
      }
    } catch (e: Exception) {
      Log.e("CallSoundVibrator", "Tone generator also failed: ${e.message}")
    }
  }

  private fun startVibration() {
    try {
      val pattern = longArrayOf(0, 1000, 1000, 1000, 1000)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createWaveform(pattern, 1)
        vibrator?.vibrate(effect)
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(pattern, 1)
      }
    } catch (e: Exception) {
      Log.w("CallSoundVibrator", "Vibration failed: ${e.message}")
    }
  }

  fun stop() {
    isRinging = false
    try {
      mediaPlayer?.apply {
        if (isPlaying) {
          stop()
        }
        release()
      }
      mediaPlayer = null
    } catch (e: Exception) {
      Log.w("CallSoundVibrator", "Error stopping MediaPlayer: ${e.message}")
    }

    try {
      fallbackRingtone?.stop()
      fallbackRingtone = null
    } catch (e: Exception) {
      Log.w("CallSoundVibrator", "Error stopping Ringtone: ${e.message}")
    }

    try {
      toneThread?.interrupt()
      toneThread = null
      toneGenerator?.release()
      toneGenerator = null
    } catch (e: Exception) {
      Log.w("CallSoundVibrator", "Error stopping ToneGenerator: ${e.message}")
    }

    try {
      vibrator?.cancel()
    } catch (e: Exception) {
      Log.w("CallSoundVibrator", "Error cancelling vibrator: ${e.message}")
    }
  }
}
