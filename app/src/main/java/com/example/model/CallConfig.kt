package com.example.model

object CallConstants {
  const val EXTRA_CALLER_NAME = "com.example.extra.CALLER_NAME"
  const val EXTRA_CALLER_NUMBER = "com.example.extra.CALLER_NUMBER"
  const val EXTRA_DELAY_SECONDS = "com.example.extra.DELAY_SECONDS"

  const val ACTION_START_TRIGGER = "com.example.action.START_TRIGGER"
  const val ACTION_CANCEL_TRIGGER = "com.example.action.CANCEL_TRIGGER"
  const val ACTION_TRIGGER_ALARM = "com.example.action.TRIGGER_ALARM"

  const val NOTIFICATION_CHANNEL_ID_TIMER = "fake_call_timer_channel"
  const val NOTIFICATION_CHANNEL_ID_CALL = "fake_call_incoming_channel"
  const val NOTIFICATION_ID_TIMER = 1001
  const val NOTIFICATION_ID_CALL = 2002

  const val DEFAULT_CALLER_NAME = "Home"
  const val DEFAULT_CALLER_NUMBER = "+1 (555) 019-2834"
}

data class CallerPreset(
  val label: String,
  val name: String,
  val number: String,
  val emoji: String
)

val PRESET_CALLERS = listOf(
  CallerPreset("Home", "Home", "+1 (555) 019-2834", "🏠"),
  CallerPreset("Boss", "Manager Sarah", "+1 (555) 839-1102", "💼"),
  CallerPreset("Mom", "Mom ❤️", "+1 (555) 492-7711", "🌸"),
  CallerPreset("Doctor", "Dr. Harrison", "+1 (555) 234-9081", "🩺"),
  CallerPreset("Police", "Local Dispatch", "+1 (555) 911-0422", "👮")
)

enum class DelayOption(val label: String, val seconds: Int) {
  TEN_SECONDS("10 Seconds", 10),
  THIRTY_SECONDS("30 Seconds", 30),
  ONE_MINUTE("1 Minute", 60),
  FIVE_MINUTES("5 Minutes", 300)
}
