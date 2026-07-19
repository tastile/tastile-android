package app.tastile.android.notifications

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tastile.android.data.repository.UserSettingsRepository
import app.tastile.android.core.designsystem.theme.NiaTheme
import app.tastile.android.ui.util.SystemBarEffect
import app.tastile.android.ui.util.resolveDarkTheme
import app.tastile.android.ui.util.supportsDynamicColor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.hypot
import kotlin.math.max

@AndroidEntryPoint
class ExecutionAlarmActivity : ComponentActivity() {
    @Inject
    lateinit var userSettingsRepository: UserSettingsRepository

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })
        prepareAlarmWindow()
        startAlarmSignal()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Tastile"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "Tastile needs your attention."
        setContent {
            val themeMode = remember { mutableStateOf(userSettingsRepository.getThemeMode()) }
            val darkTheme = resolveDarkTheme(themeMode.value)
            NiaTheme(
                darkTheme = darkTheme,
                androidTheme = false,
                disableDynamicTheming = true,
            ) {
                SystemBarEffect(color = MaterialTheme.colorScheme.background, darkTheme = darkTheme)
                AlarmSurface(
                    title = title,
                    body = body,
                    onDismiss = { dismissAlarm() }
                )
            }
        }
    }

    override fun onDestroy() {
        stopAlarmSignal()
        super.onDestroy()
    }

    private fun prepareAlarmWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.post {
                window.decorView.windowInsetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    private fun startAlarmSignal() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mediaPlayer = runCatching {
            MediaPlayer().apply {
                setDataSource(this@ExecutionAlarmActivity, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        runCatching {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 600, 350, 600, 900), 0)
            )
        }
    }

    private fun stopAlarmSignal() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    private fun dismissAlarm() {
        stopAlarmSignal()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            intent.getIntExtra(EXTRA_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID)
        )
        finish()
    }

    companion object {
        const val EXTRA_TITLE = "extra_alarm_title"
        const val EXTRA_BODY = "extra_alarm_body"
        const val EXTRA_NOTIFICATION_ID = "extra_alarm_notification_id"
        const val DEFAULT_NOTIFICATION_ID = 492
    }
}

@Composable
private fun AlarmSurface(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val dragDistance = hypot(dragX, dragY)
    val progress = (dragDistance / 260f).coerceIn(0f, 1f)
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val foregroundColor = MaterialTheme.colorScheme.onSurface
    val expansionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f + progress * 0.24f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = surfaceColor,
        contentColor = foregroundColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragX = 0f
                            dragY = 0f
                        },
                        onDragEnd = {
                            if (progress >= 0.82f) {
                                onDismiss()
                            } else {
                                dragX = 0f
                                dragY = 0f
                            }
                        },
                        onDragCancel = {
                            dragX = 0f
                            dragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragX += dragAmount.x
                            dragY += dragAmount.y
                            if (hypot(dragX, dragY) >= 260f) {
                                onDismiss()
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val origin = Offset(size.width / 2f, size.height * 0.68f)
                val maxRadius = max(size.width, size.height) * 1.08f
                drawCircle(
                    color = expansionColor,
                    radius = maxRadius * progress,
                    center = origin
                )
                drawCircle(
                    color = foregroundColor.copy(alpha = 0.10f + progress * 0.08f),
                    radius = 42.dp.toPx() + 34.dp.toPx() * progress,
                    center = origin
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    body,
                    modifier = Modifier.padding(top = 18.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = foregroundColor.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                "Swipe outward",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = foregroundColor.copy(alpha = 0.52f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
