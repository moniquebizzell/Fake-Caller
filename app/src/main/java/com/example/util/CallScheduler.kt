package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.MainActivity
import com.example.model.CallConstants
import com.example.receiver.CallAlarmReceiver
import com.example.service.CallTriggerService

object CallScheduler {

  fun scheduleCall(
    context: Context,
    delaySeconds: Int,
    callerName: String,
    callerNumber: String
  ) {
    // 1. Start foreground countdown service
    CallTriggerService.startService(context, delaySeconds, callerName, callerNumber)

    // 2. Schedule AlarmManager exact alarm as dual safety net
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerAtMillis = System.currentTimeMillis() + (delaySeconds * 1000L)

    val alarmIntent = Intent(context, CallAlarmReceiver::class.java).apply {
      action = CallConstants.ACTION_TRIGGER_ALARM
      putExtra(CallConstants.EXTRA_CALLER_NAME, callerName)
      putExtra(CallConstants.EXTRA_CALLER_NUMBER, callerNumber)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      CallConstants.NOTIFICATION_ID_CALL,
      alarmIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
          context,
          0,
          showIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
      } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      }
    } catch (_: SecurityException) {
      // If SCHEDULE_EXACT_ALARM is not granted, fallback to setAndAllowWhileIdle
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      } else {
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      }
    } catch (_: Exception) {}
  }

  fun cancelCall(context: Context) {
    // Stop foreground service
    CallTriggerService.stopService(context)

    // Cancel AlarmManager alarm
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val alarmIntent = Intent(context, CallAlarmReceiver::class.java).apply {
      action = CallConstants.ACTION_TRIGGER_ALARM
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      CallConstants.NOTIFICATION_ID_CALL,
      alarmIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
  }
}
