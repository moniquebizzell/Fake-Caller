package com.example

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallConstants
import com.example.ui.theme.EmeraldAnswer
import com.example.ui.theme.RoseDecline
import com.example.util.CallSoundVibratorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class CallState {
  RINGING,
  ACTIVE,
  ENDED
}

class CallActivity : ComponentActivity() {

  private lateinit var soundVibratorManager: CallSoundVibratorManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Configure Activity to wake up device screen and show over the lock screen
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
      val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
      keyguardManager?.requestDismissKeyguard(this, null)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
      )
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Clear incoming call notification
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(CallConstants.NOTIFICATION_ID_CALL)

    soundVibratorManager = CallSoundVibratorManager(this)

    val callerName = intent.getStringExtra(CallConstants.EXTRA_CALLER_NAME) ?: CallConstants.DEFAULT_CALLER_NAME
    val callerNumber = intent.getStringExtra(CallConstants.EXTRA_CALLER_NUMBER) ?: CallConstants.DEFAULT_CALLER_NUMBER

    setContent {
      CallScreen(
        callerName = callerName,
        callerNumber = callerNumber,
        soundVibratorManager = soundVibratorManager,
        onCallFinished = {
          finishAndRemoveTask()
        }
      )
    }
  }

  override fun onDestroy() {
    if (::soundVibratorManager.isInitialized) {
      soundVibratorManager.stop()
    }
    super.onDestroy()
  }
}

@Composable
fun CallScreen(
  callerName: String,
  callerNumber: String,
  soundVibratorManager: CallSoundVibratorManager,
  onCallFinished: () -> Unit
) {
  var callState by remember { mutableStateOf(CallState.RINGING) }
  var callDurationSeconds by remember { mutableIntStateOf(0) }
  var isMuted by remember { mutableStateOf(false) }
  var isSpeakerOn by remember { mutableStateOf(false) }
  var isOnHold by remember { mutableStateOf(false) }
  var showKeypad by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()

  // Start sound and vibration when entering RINGING state
  DisposableEffect(Unit) {
    soundVibratorManager.startRingingAndVibration()
    onDispose {
      soundVibratorManager.stop()
    }
  }

  // Active call duration timer
  LaunchedEffect(callState, isOnHold) {
    if (callState == CallState.ACTIVE && !isOnHold) {
      while (true) {
        delay(1000L)
        callDurationSeconds++
      }
    }
  }

  // Handle system back button: Decline if ringing, End if active
  BackHandler {
    soundVibratorManager.stop()
    callState = CallState.ENDED
    coroutineScope.launch {
      delay(400L)
      onCallFinished()
    }
  }

  val backgroundBrush = Brush.verticalGradient(
    colors = listOf(
      Color(0xFF090D16),
      Color(0xFF0F172A),
      Color(0xFF020617)
    )
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(backgroundBrush)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Top Section: Carrier and Call Status
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp)
      ) {
        Text(
          text = "SIM 1 • HD Voice",
          color = Color.White.copy(alpha = 0.5f),
          fontSize = 13.sp,
          letterSpacing = 0.5.sp,
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Caller Avatar with pulsing rings when ringing
        CallerAvatar(callState = callState)

        Spacer(modifier = Modifier.height(20.dp))

        // Caller Name
        Text(
          text = callerName,
          color = Color.White,
          fontSize = 32.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("caller_name_text")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Caller Phone Number
        Text(
          text = callerNumber,
          color = Color.White.copy(alpha = 0.7f),
          fontSize = 17.sp,
          fontWeight = FontWeight.Normal,
          letterSpacing = 0.5.sp,
          modifier = Modifier.testTag("caller_number_text")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Subtitle status: "Incoming call..." or "00:04" or "Call ended"
        CallStatusSubtitle(
          callState = callState,
          durationSeconds = callDurationSeconds,
          isOnHold = isOnHold
        )
      }

      // 2. Middle Section: In-Call Controls when Active, or Quick Options when Ringing
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false),
        contentAlignment = Alignment.Center
      ) {
        if (callState == CallState.RINGING) {
          // Quick actions row above answer buttons
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            QuickOptionButton(
              icon = Icons.Outlined.Alarm,
              label = "Remind me",
              onClick = {}
            )
            QuickOptionButton(
              icon = Icons.Default.Sms,
              label = "Message",
              onClick = {}
            )
          }
        } else if (callState == CallState.ACTIVE) {
          // 6-button dialer grid
          InCallControlsGrid(
            isMuted = isMuted,
            onToggleMute = { isMuted = !isMuted },
            showKeypad = showKeypad,
            onToggleKeypad = { showKeypad = !showKeypad },
            isSpeakerOn = isSpeakerOn,
            onToggleSpeaker = { isSpeakerOn = !isSpeakerOn },
            isOnHold = isOnHold,
            onToggleHold = { isOnHold = !isOnHold }
          )
        }
      }

      // 3. Bottom Section: Answer/Decline or End Call
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 32.dp),
        contentAlignment = Alignment.Center
      ) {
        when (callState) {
          CallState.RINGING -> {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Decline Button (Red)
              CallActionButton(
                label = "Decline",
                backgroundColor = RoseDecline,
                icon = Icons.Default.CallEnd,
                testTag = "decline_call_button",
                onClick = {
                  soundVibratorManager.stop()
                  callState = CallState.ENDED
                  coroutineScope.launch {
                    delay(500L)
                    onCallFinished()
                  }
                }
              )

              // Accept Button (Green)
              CallActionButton(
                label = "Accept",
                backgroundColor = EmeraldAnswer,
                icon = Icons.Default.Call,
                testTag = "accept_call_button",
                onClick = {
                  soundVibratorManager.stop()
                  callState = CallState.ACTIVE
                }
              )
            }
          }

          CallState.ACTIVE -> {
            // End Call Button (Red, Centered)
            CallActionButton(
              label = "End Call",
              backgroundColor = RoseDecline,
              icon = Icons.Default.CallEnd,
              testTag = "end_call_button",
              onClick = {
                callState = CallState.ENDED
                coroutineScope.launch {
                  delay(700L)
                  onCallFinished()
                }
              }
            )
          }

          CallState.ENDED -> {
            Text(
              text = "Call ended",
              color = Color.White.copy(alpha = 0.6f),
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    // Keypad dialog overlay if toggled during active call
    if (showKeypad && callState == CallState.ACTIVE) {
      KeypadOverlay(onDismiss = { showKeypad = false })
    }
  }
}

@Composable
fun CallerAvatar(callState: CallState) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")

  val pulseScale1 by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulse_scale_1"
  )

  val pulseAlpha1 by infiniteTransition.animateFloat(
    initialValue = 0.45f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulse_alpha_1"
  )

  val pulseScale2 by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.6f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, delayMillis = 400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulse_scale_2"
  )

  val pulseAlpha2 by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, delayMillis = 400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "pulse_alpha_2"
  )

  Box(
    modifier = Modifier.size(150.dp),
    contentAlignment = Alignment.Center
  ) {
    if (callState == CallState.RINGING) {
      // Outer pulse ring 2
      Box(
        modifier = Modifier
          .size(105.dp)
          .scale(pulseScale2)
          .border(2.dp, Color(0xFF10B981).copy(alpha = pulseAlpha2), CircleShape)
      )

      // Outer pulse ring 1
      Box(
        modifier = Modifier
          .size(105.dp)
          .scale(pulseScale1)
          .border(2.dp, Color(0xFF10B981).copy(alpha = pulseAlpha1), CircleShape)
      )
    }

    // Avatar Circle
    Box(
      modifier = Modifier
        .size(105.dp)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(Color(0xFF334155), Color(0xFF1E293B))
          )
        )
        .border(1.5.dp, Color.White.copy(alpha = 0.15f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "Caller Avatar",
        tint = Color.White.copy(alpha = 0.85f),
        modifier = Modifier.size(62.dp)
      )
    }
  }
}

@Composable
fun CallStatusSubtitle(
  callState: CallState,
  durationSeconds: Int,
  isOnHold: Boolean
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse_text")
  val alphaPulse by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha_pulse"
  )

  when (callState) {
    CallState.RINGING -> {
      Text(
        text = "Incoming call...",
        color = Color(0xFF38BDF8).copy(alpha = alphaPulse),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        modifier = Modifier.testTag("incoming_call_subtitle")
      )
    }

    CallState.ACTIVE -> {
      if (isOnHold) {
        Text(
          text = "Call on hold",
          color = Color(0xFFF59E0B),
          fontSize = 15.sp,
          fontWeight = FontWeight.SemiBold
        )
      } else {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        Text(
          text = timeString,
          color = Color(0xFF4ADE80),
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 1.sp,
          modifier = Modifier.testTag("call_timer_text")
        )
      }
    }

    CallState.ENDED -> {
      Text(
        text = "Call ended",
        color = RoseDecline,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
fun CallActionButton(
  label: String,
  backgroundColor: Color,
  icon: ImageVector,
  testTag: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .clip(CircleShape)
        .background(backgroundColor)
        .testTag(testTag),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = Color.White,
        modifier = Modifier.size(36.dp)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = label,
      color = Color.White.copy(alpha = 0.9f),
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun QuickOptionButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.1f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = Color.White.copy(alpha = 0.8f),
        modifier = Modifier.size(24.dp)
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      color = Color.White.copy(alpha = 0.65f),
      fontSize = 12.sp
    )
  }
}

@Composable
fun InCallControlsGrid(
  isMuted: Boolean,
  onToggleMute: () -> Unit,
  showKeypad: Boolean,
  onToggleKeypad: () -> Unit,
  isSpeakerOn: Boolean,
  onToggleSpeaker: () -> Unit,
  isOnHold: Boolean,
  onToggleHold: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      InCallControlItem(
        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
        label = if (isMuted) "Unmute" else "Mute",
        isActive = isMuted,
        onClick = onToggleMute
      )
      InCallControlItem(
        icon = Icons.Default.Dialpad,
        label = "Keypad",
        isActive = showKeypad,
        onClick = onToggleKeypad
      )
      InCallControlItem(
        icon = Icons.Default.VolumeUp,
        label = "Speaker",
        isActive = isSpeakerOn,
        onClick = onToggleSpeaker
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      InCallControlItem(
        icon = Icons.Default.Add,
        label = "Add call",
        isActive = false,
        onClick = {}
      )
      InCallControlItem(
        icon = Icons.Default.Pause,
        label = if (isOnHold) "Resume" else "Hold",
        isActive = isOnHold,
        onClick = onToggleHold
      )
      InCallControlItem(
        icon = Icons.Default.MoreVert,
        label = "More",
        isActive = false,
        onClick = {}
      )
    }
  }
}

@Composable
fun InCallControlItem(
  icon: ImageVector,
  label: String,
  isActive: Boolean,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .size(60.dp)
        .clip(CircleShape)
        .background(
          if (isActive) Color.White else Color.White.copy(alpha = 0.12f)
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isActive) Color.Black else Color.White,
        modifier = Modifier.size(28.dp)
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = label,
      color = Color.White.copy(alpha = 0.8f),
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun KeypadOverlay(onDismiss: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.85f))
      .clickable(onClick = onDismiss),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF1E293B),
      modifier = Modifier
        .padding(24.dp)
        .clickable(enabled = false) {}
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Keypad",
          color = Color.White,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        val digits = listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9"),
          listOf("*", "0", "#")
        )

        digits.forEach { row ->
          Row(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
          ) {
            row.forEach { digit ->
              Box(
                modifier = Modifier
                  .size(56.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.12f))
                  .clickable {},
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = digit,
                  color = Color.White,
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Tap outside to close",
          color = Color.White.copy(alpha = 0.5f),
          fontSize = 13.sp
        )
      }
    }
  }
}
