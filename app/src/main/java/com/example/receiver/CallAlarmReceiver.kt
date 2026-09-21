package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.CallActivity
import com.example.R
import com.example.model.CallConstants
import com.example.service.CallTriggerService

class CallAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val callerName = intent.getStringExtra(CallConstants.EXTRA_CALLER_NAME) ?: CallConstants.DEFAULT_CALLER_NAME
    val callerNumber = intent.getStringExtra(CallConstants.EXTRA_CALLER_NUMBER) ?: CallConstants.DEFAULT_CALLER_NUMBER

    // Stop background service if it was still running
    CallTriggerService.stopService(context)

    // Wake screen and launch call
    triggerIncomingCall(context, callerName, callerNumber)
  }

  companion object {
    fun triggerIncomingCall(context: Context, callerName: String, callerNumber: String) {
      // 1. Wake the device screen via PowerManager WakeLock
      val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
      val wakeLock = powerManager?.newWakeLock(
        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
        "FakeCall:ScreenWakeLock"
      )
      try {
        wakeLock?.acquire(15000L) // hold wake lock for up to 15s to allow user interaction
      } catch (_: Exception) {}

      // 2. Prepare CallActivity Intent
      val callIntent = Intent(context, CallActivity::class.java).apply {
        putExtra(CallConstants.EXTRA_CALLER_NAME, callerName)
        putExtra(CallConstants.EXTRA_CALLER_NUMBER, callerNumber)
        addFlags(
          Intent.FLAG_ACTIVITY_NEW_TASK or
          Intent.FLAG_ACTIVITY_CLEAR_TOP or
          Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
      }

      // 3. Post High-Priority FullScreenIntent Notification (required by modern Android when screen is locked)
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
          CallConstants.NOTIFICATION_CHANNEL_ID_CALL,
          "Incoming Call Alert",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = "Full screen incoming call notification"
          setBypassDnd(true)
          lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
      }

      val fullScreenPendingIntent = PendingIntent.getActivity(
        context,
        CallConstants.NOTIFICATION_ID_CALL,
        callIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(context, CallConstants.NOTIFICATION_CHANNEL_ID_CALL)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(callerName)
        .setContentText("Incoming call...")
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_CALL)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .setAutoCancel(true)
        .build()

      notificationManager.notify(CallConstants.NOTIFICATION_ID_CALL, notification)

      // 4. Also start Activity directly
      try {
        context.startActivity(callIntent)
      } catch (e: Exception) {
        // Full screen intent notification handles this if background activity launch restriction applies
      }
    }
  }
}
