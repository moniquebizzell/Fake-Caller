package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CallConstants
import com.example.model.DelayOption
import com.example.model.PRESET_CALLERS
import com.example.util.CallScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FakeCallRobolectricTest {

  @Test
  fun testDefaultCallerValues() {
    assertEquals("Home", CallConstants.DEFAULT_CALLER_NAME)
    assertEquals("+1 (555) 019-2834", CallConstants.DEFAULT_CALLER_NUMBER)
  }

  @Test
  fun testDelayOptionsSeconds() {
    assertEquals(10, DelayOption.TEN_SECONDS.seconds)
    assertEquals(30, DelayOption.THIRTY_SECONDS.seconds)
    assertEquals(60, DelayOption.ONE_MINUTE.seconds)
    assertEquals(300, DelayOption.FIVE_MINUTES.seconds)

    assertEquals("10 Seconds", DelayOption.TEN_SECONDS.label)
    assertEquals("30 Seconds", DelayOption.THIRTY_SECONDS.label)
    assertEquals("1 Minute", DelayOption.ONE_MINUTE.label)
    assertEquals("5 Minutes", DelayOption.FIVE_MINUTES.label)
  }

  @Test
  fun testPresetCallersList() {
    assertTrue(PRESET_CALLERS.isNotEmpty())
    val homePreset = PRESET_CALLERS.firstOrNull { it.label == "Home" }
    assertNotNull(homePreset)
    assertEquals("Home", homePreset?.name)
    assertEquals("+1 (555) 019-2834", homePreset?.number)
  }

  @Test
  fun testCallSchedulerScheduleAndCancel() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // Verify schedule call runs without throwing exceptions
    CallScheduler.scheduleCall(context, 10, "Test Caller", "+123456789")
    // Verify cancel call runs without throwing exceptions
    CallScheduler.cancelCall(context)
  }
}
