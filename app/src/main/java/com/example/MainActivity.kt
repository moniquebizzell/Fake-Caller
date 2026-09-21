package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.CallConstants
import com.example.model.DelayOption
import com.example.model.PRESET_CALLERS
import com.example.service.CallTriggerService
import com.example.ui.theme.EmeraldAnswer
import com.example.ui.theme.EmeraldAnswerDark
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoPrimaryLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RoseDecline
import com.example.util.CallScheduler

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        MainSetupScreen()
      }
    }
  }
}

@Composable
fun MainSetupScreen() {
  val context = LocalContext.current
  val activity = context as? Activity

  var callerName by remember { mutableStateOf(CallConstants.DEFAULT_CALLER_NAME) }
  var callerNumber by remember { mutableStateOf(CallConstants.DEFAULT_CALLER_NUMBER) }
  var selectedDelay by remember { mutableStateOf(DelayOption.TEN_SECONDS) }

  // Observe active countdown if service is running
  val activeCountdown by CallTriggerService.activeCountdown.collectAsState()

  // Permission launcher for Android 13+
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { _ ->
      // Proceed with scheduling
    }
  )

  fun checkNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!hasPermission) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  fun triggerArm(delaySec: Int) {
    checkNotificationPermission()

    val validName = if (callerName.isBlank()) CallConstants.DEFAULT_CALLER_NAME else callerName.trim()
    val validNumber = if (callerNumber.isBlank()) CallConstants.DEFAULT_CALLER_NUMBER else callerNumber.trim()

    CallScheduler.scheduleCall(
      context = context,
      delaySeconds = delaySec,
      callerName = validName,
      callerNumber = validNumber
    )

    Toast.makeText(
      context,
      "Fake call armed for $delaySec seconds! Minimizing...",
      Toast.LENGTH_SHORT
    ).show()

    // Immediately minimize the setup view as required
    activity?.moveTaskToBack(true)
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Header Banner
      HeaderSection()

      // 2. Active Armed Countdown Banner (if armed)
      AnimatedVisibility(
        visible = activeCountdown != null,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
      ) {
        activeCountdown?.let { seconds ->
          ArmedBanner(
            secondsRemaining = seconds,
            onCancel = {
              CallScheduler.cancelCall(context)
              Toast.makeText(context, "Trigger cancelled", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }

      // 3. Caller Profile Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(IndigoPrimary.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = IndigoPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
            Text(
              text = "Caller Profile",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          // Quick Presets Row
          Text(
            text = "Quick Presets",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            PRESET_CALLERS.take(4).forEach { preset ->
              val isSelected = callerName == preset.name
              val chipBg by animateColorAsState(
                if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
                label = "chip_bg"
              )
              val chipTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = chipBg,
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    callerName = preset.name
                    callerNumber = preset.number
                  }
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(text = preset.emoji, fontSize = 16.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = preset.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = chipTextColor,
                    maxLines = 1
                  )
                }
              }
            }
          }

          // Input: Caller Name
          OutlinedTextField(
            value = callerName,
            onValueChange = { callerName = it },
            label = { Text("Caller Name") },
            placeholder = { Text("e.g. Home, Boss, Mom") },
            leadingIcon = {
              Icon(Icons.Default.Person, contentDescription = "Caller Name Icon", tint = IndigoPrimary)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("caller_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = IndigoPrimary,
              focusedLabelColor = IndigoPrimary
            )
          )

          // Input: Caller Number
          OutlinedTextField(
            value = callerNumber,
            onValueChange = { callerNumber = it },
            label = { Text("Caller Number") },
            placeholder = { Text("e.g. +1 (555) 019-2834") },
            leadingIcon = {
              Icon(Icons.Default.Phone, contentDescription = "Caller Number Icon", tint = IndigoPrimary)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("caller_number_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = IndigoPrimary,
              focusedLabelColor = IndigoPrimary
            )
          )
        }
      }

      // 4. Trigger Delay Selector Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(EmeraldAnswer.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = EmeraldAnswerDark,
                modifier = Modifier.size(20.dp)
              )
            }
            Text(
              text = "Select Delay",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          // Delay Buttons Grid (2x2)
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              DelayButton(
                option = DelayOption.TEN_SECONDS,
                isSelected = selectedDelay == DelayOption.TEN_SECONDS,
                testTag = "delay_10s_button",
                modifier = Modifier.weight(1f),
                onClick = { selectedDelay = DelayOption.TEN_SECONDS }
              )
              DelayButton(
                option = DelayOption.THIRTY_SECONDS,
                isSelected = selectedDelay == DelayOption.THIRTY_SECONDS,
                testTag = "delay_30s_button",
                modifier = Modifier.weight(1f),
                onClick = { selectedDelay = DelayOption.THIRTY_SECONDS }
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              DelayButton(
                option = DelayOption.ONE_MINUTE,
                isSelected = selectedDelay == DelayOption.ONE_MINUTE,
                testTag = "delay_1m_button",
                modifier = Modifier.weight(1f),
                onClick = { selectedDelay = DelayOption.ONE_MINUTE }
              )
              DelayButton(
                option = DelayOption.FIVE_MINUTES,
                isSelected = selectedDelay == DelayOption.FIVE_MINUTES,
                testTag = "delay_5m_button",
                modifier = Modifier.weight(1f),
                onClick = { selectedDelay = DelayOption.FIVE_MINUTES }
              )
            }
          }
        }
      }

      // 5. Action Buttons (Arm Trigger & Quick Test)
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Arm Trigger Button (Primary)
        Button(
          onClick = { triggerArm(selectedDelay.seconds) },
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = IndigoPrimary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("arm_trigger_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Alarm,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = "Arm Trigger (${selectedDelay.label})",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        // Quick Test Button (Trigger in 3s)
        OutlinedButton(
          onClick = { triggerArm(3) },
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("quick_test_button"),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = EmeraldAnswerDark
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.FlashOn,
              contentDescription = null,
              tint = EmeraldAnswerDark,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "Quick Test (3 Seconds)",
              fontSize = 15.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // 6. Practical Feature Summary & Tips Card
      FeatureTipsCard()

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
fun HeaderSection() {
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 2.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.horizontalGradient(
            listOf(
              IndigoPrimary.copy(alpha = 0.08f),
              EmeraldAnswer.copy(alpha = 0.08f)
            )
          )
        )
        .padding(18.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(listOf(IndigoPrimary, IndigoPrimaryLight))
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PhoneCallback,
            contentDescription = "Fake Call Logo",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
          )
        }

        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "Fake Call",
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = EmeraldAnswer.copy(alpha = 0.15f)
            ) {
              Text(
                text = "OFFLINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldAnswerDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Realistic inbound call utility & screen wake prank",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
          )
        }
      }
    }
  }
}

@Composable
fun ArmedBanner(
  secondsRemaining: Int,
  onCancel: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFFFEF3C7) // warm amber
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("armed_banner")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFFF59E0B)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }

        Column {
          Text(
            text = "Trigger Armed!",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF92400E)
          )
          Text(
            text = "Calling in $secondsRemaining seconds...",
            fontSize = 13.sp,
            color = Color(0xFFB45309)
          )
        }
      }

      Button(
        onClick = onCancel,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = RoseDecline
        ),
        modifier = Modifier.testTag("cancel_trigger_button")
      ) {
        Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}

@Composable
fun DelayButton(
  option: DelayOption,
  isSelected: Boolean,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val backgroundColor by animateColorAsState(
    if (isSelected) EmeraldAnswer else MaterialTheme.colorScheme.surfaceVariant,
    animationSpec = tween(200),
    label = "delay_btn_bg"
  )
  val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
  val borderColor = if (isSelected) EmeraldAnswerDark else Color.Transparent

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = backgroundColor,
    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, borderColor) else null,
    modifier = modifier
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = option.label,
        fontSize = 14.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = textColor
      )
      if (isSelected) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Selected",
          tint = Color.White,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
fun FeatureTipsCard() {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = IndigoPrimary,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "Reliability & Screen Wake",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Text(
        text = "• Locksreen Ready: Activity turns the screen on & displays over keyguard when triggered.\n" +
               "• Full-Screen Native Dialer: Dark backdrop, pulsing avatar, realistic ringtone & vibration.\n" +
               "• Active Call Simulation: Realistic timer and interactive in-call controls after accepting.\n" +
               "• 100% Offline: Zero telemetry, zero external networks.",
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
      )
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}
