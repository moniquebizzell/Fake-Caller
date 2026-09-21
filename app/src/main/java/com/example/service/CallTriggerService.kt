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
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.CallConstants
import com.example.receiver.CallAlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallTriggerService : Service() {

  private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
  private var countdownJob: Job? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannels()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      CallConstants.ACTION_CANCEL_TRIGGER -> {
        stopCountdownAndSelf()
        return START_NOT_STICKY
      }
      CallConstants.ACTION_START_TRIGGER -> {
        val delaySec = intent.getIntExtra(CallConstants.EXTRA_DELAY_SECONDS, 10)
        val name = intent.getStringExtra(CallConstants.EXTRA_CALLER_NAME) ?: CallConstants.DEFAULT_CALLER_NAME
        val number = intent.getStringExtra(CallConstants.EXTRA_CALLER_NUMBER) ?: CallConstants.DEFAULT_CALLER_NUMBER

        _callerInfo.value = Pair(name, number)
        startForeground(CallConstants.NOTIFICATION_ID_TIMER, buildTimerNotification(delaySec, name))
        startCountdown(delaySec, name, number)
      }
    }
    return START_NOT_STICKY
  }

  private fun startCountdown(totalSeconds: Int, callerName: String, callerNumber: String) {
    countdownJob?.cancel()
    countdownJob = serviceScope.launch {
      var remaining = totalSeconds
      while (remaining > 0) {
        _activeCountdown.value = remaining
        updateNotification(remaining, callerName)
        delay(1000L)
        remaining--
      }
      _activeCountdown.value = null
      _callerInfo.value = null

      // Trigger incoming call
      CallAlarmReceiver.triggerIncomingCall(applicationContext, callerName, callerNumber)
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
    }
  }

  private fun stopCountdownAndSelf() {
    countdownJob?.cancel()
    countdownJob = null
    _activeCountdown.value = null
    _callerInfo.value = null
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun updateNotification(seconds: Int, callerName: String) {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(
      CallConstants.NOTIFICATION_ID_TIMER,
      buildTimerNotification(seconds, callerName)
    )
  }

  private fun buildTimerNotification(seconds: Int, callerName: String): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openPendingIntent = PendingIntent.getActivity(
      this,
      0,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val cancelIntent = Intent(this, CallTriggerService::class.java).apply {
      action = CallConstants.ACTION_CANCEL_TRIGGER
    }
    val cancelPendingIntent = PendingIntent.getService(
      this,
      1,
      cancelIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val timeText = if (seconds >= 60) {
      val mins = seconds / 60
      val secs = seconds % 60
      "${mins}m ${secs}s"
    } else {
      "${seconds}s"
    }

    return NotificationCompat.Builder(this, CallConstants.NOTIFICATION_CHANNEL_ID_TIMER)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Fake Call Scheduled")
      .setContentText("Incoming call from $callerName in $timeText")
      .setContentIntent(openPendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setSilent(true)
      .build()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val channel = NotificationChannel(
        CallConstants.NOTIFICATION_CHANNEL_ID_TIMER,
        "Fake Call Timer",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Shows countdown when a fake call is scheduled"
        setShowBadge(false)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  override fun onDestroy() {
    countdownJob?.cancel()
    _activeCountdown.value = null
    _callerInfo.value = null
    super.onDestroy()
  }

  companion object {
    private val _activeCountdown = MutableStateFlow<Int?>(null)
    val activeCountdown: StateFlow<Int?> = _activeCountdown.asStateFlow()

    private val _callerInfo = MutableStateFlow<Pair<String, String>?>(null)
    val callerInfo: StateFlow<Pair<String, String>?> = _callerInfo.asStateFlow()

    fun startService(context: Context, delaySeconds: Int, callerName: String, callerNumber: String) {
      val intent = Intent(context, CallTriggerService::class.java).apply {
        action = CallConstants.ACTION_START_TRIGGER
        putExtra(CallConstants.EXTRA_DELAY_SECONDS, delaySeconds)
        putExtra(CallConstants.EXTRA_CALLER_NAME, callerName)
        putExtra(CallConstants.EXTRA_CALLER_NUMBER, callerNumber)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, CallTriggerService::class.java).apply {
        action = CallConstants.ACTION_CANCEL_TRIGGER
      }
      context.startService(intent)
    }
  }
}
