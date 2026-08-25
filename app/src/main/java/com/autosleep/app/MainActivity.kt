package com.autosleep.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autosleep.app.engine.SleepAction
import com.autosleep.app.engine.SleepConfidenceEngine
import com.autosleep.app.engine.SleepObservation
import com.autosleep.app.media.AutoSleepNotificationListenerService
import com.autosleep.app.media.MediaSessionObserver
import com.autosleep.app.media.MediaSessionSnapshot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AutoSleepDashboard()
            }
        }
    }
}

@Composable
fun AutoSleepDashboard() {
    val engine = remember { SleepConfidenceEngine() }
    var observation by remember {
        mutableStateOf(
            SleepObservation(
                withinSleepWindow = true,
                inactivityMinutes = 24,
                stationaryMinutes = 22,
                mediaPlaying = true,
                playbackMinutes = 35,
                mediaTransitions = 3,
                screenOn = true,
                charging = false,
            ),
        )
    }

    val decision = engine.evaluate(observation)

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("AutoSleep", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Prototype 0.1 · local inference only",
                style = MaterialTheme.typography.bodyMedium,
            )

            MediaSessionDebugCard()

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Sleep confidence", style = MaterialTheme.typography.titleMedium)
                    Text("${decision.score}/100", style = MaterialTheme.typography.displaySmall)
                    Text("Recommended action: ${decision.action.name}")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Why this score", style = MaterialTheme.typography.titleMedium)
                    decision.reasons.forEach { reason ->
                        Text("• $reason")
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        observation = observation.copy(
                            inactivityMinutes = observation.inactivityMinutes + 10,
                            stationaryMinutes = observation.stationaryMinutes + 10,
                            playbackMinutes = observation.playbackMinutes + 10,
                            mediaTransitions = observation.mediaTransitions + 1,
                        )
                    },
                ) {
                    Text("Simulate +10 min")
                }

                Button(
                    onClick = {
                        observation = observation.copy(
                            inactivityMinutes = 0,
                            stationaryMinutes = 0,
                            mediaTransitions = 0,
                        )
                    },
                ) {
                    Text("I'm awake")
                }
            }

            if (decision.action != SleepAction.OBSERVE) {
                Text(
                    if (decision.action == SleepAction.PAUSE) {
                        "Prototype would request media pause after the intervention timeout."
                    } else {
                        "Prototype would ask: Still watching?"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun MediaSessionDebugCard() {
    val context = LocalContext.current
    val observer = remember(context) { MediaSessionObserver(context) }
    var sessions by remember { mutableStateOf<List<MediaSessionSnapshot>>(emptyList()) }
    var status by remember { mutableStateOf("Grant media access, then refresh sessions.") }

    val listenerComponent = remember(context) {
        ComponentName(context, AutoSleepNotificationListenerService::class.java)
    }
    val accessEnabled = isNotificationListenerEnabled(context, listenerComponent)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Media session experiment", style = MaterialTheme.typography.titleMedium)
            Text(if (accessEnabled) "Media access: enabled" else "Media access: not enabled")
            Text(status, style = MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                ) {
                    Text("Media access")
                }

                Button(
                    onClick = {
                        observer.activeSessions()
                            .onSuccess {
                                sessions = it
                                status = "Detected ${it.size} active media session(s)."
                            }
                            .onFailure {
                                sessions = emptyList()
                                status = "Unable to read sessions. Check media access."
                            }
                    },
                ) {
                    Text("Refresh")
                }
            }

            Button(
                onClick = {
                    observer.pausePlayingSessions()
                        .onSuccess { count ->
                            status = "Pause requested for $count compatible playing session(s)."
                        }
                        .onFailure {
                            status = "Pause request unavailable. Check media access."
                        }
                },
                enabled = accessEnabled,
            ) {
                Text("Pause playing media")
            }

            sessions.forEach { session ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(session.packageName, style = MaterialTheme.typography.labelLarge)
                    Text(session.title ?: "Untitled media")
                    session.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text(
                        "playing=${session.isPlaying} · canPause=${session.canPause} · state=${session.playbackState}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun isNotificationListenerEnabled(
    context: android.content.Context,
    component: ComponentName,
): Boolean {
    val enabledListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
    ) ?: return false

    return enabledListeners
        .split(':')
        .mapNotNull(ComponentName::unflattenFromString)
        .any { it == component }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    MaterialTheme {
        AutoSleepDashboard()
    }
}
