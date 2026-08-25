package com.autosleep.app

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autosleep.app.engine.SleepAction
import com.autosleep.app.engine.SleepConfidenceEngine
import com.autosleep.app.engine.SleepObservation
import com.autosleep.app.media.MediaMonitorState
import com.autosleep.app.media.MediaSessionMonitor
import com.autosleep.app.media.MediaSessionSnapshot

class MainActivity : ComponentActivity() {
    private lateinit var mediaSessionMonitor: MediaSessionMonitor
    private var mediaMonitorState by mutableStateOf(MediaMonitorState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaSessionMonitor = MediaSessionMonitor(this) { state ->
            mediaMonitorState = state
        }

        setContent {
            MaterialTheme {
                AutoSleepDashboard(
                    mediaState = mediaMonitorState,
                    onOpenMediaAccess = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onRefreshMedia = mediaSessionMonitor::refresh,
                    onPauseMedia = mediaSessionMonitor::requestPause,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mediaSessionMonitor.start()
    }

    override fun onStop() {
        mediaSessionMonitor.stop()
        super.onStop()
    }
}

@Composable
fun AutoSleepDashboard(
    mediaState: MediaMonitorState = MediaMonitorState(),
    onOpenMediaAccess: () -> Unit = {},
    onRefreshMedia: () -> Unit = {},
    onPauseMedia: (String) -> Unit = {},
) {
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
                "Prototype 0.2 · local inference + media compatibility probe",
                style = MaterialTheme.typography.bodyMedium,
            )

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

            Spacer(modifier = Modifier.height(8.dp))
            MediaCompatibilityPanel(
                state = mediaState,
                onOpenAccess = onOpenMediaAccess,
                onRefresh = onRefreshMedia,
                onPause = onPauseMedia,
            )
        }
    }
}

@Composable
private fun MediaCompatibilityPanel(
    state: MediaMonitorState,
    onOpenAccess: () -> Unit,
    onRefresh: () -> Unit,
    onPause: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Media compatibility probe", style = MaterialTheme.typography.titleLarge)
            Text(
                if (state.accessGranted) {
                    "Notification Access enabled. AutoSleep can inspect media sessions Android exposes to enabled notification listeners."
                } else {
                    "Notification Access is required before AutoSleep can inspect active media sessions."
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onOpenAccess) {
                    Text(if (state.accessGranted) "Media access settings" else "Enable media access")
                }
                Button(
                    onClick = onRefresh,
                    enabled = state.accessGranted,
                ) {
                    Text("Refresh")
                }
            }

            Text(
                "Visible sessions: ${state.sessions.size}",
                style = MaterialTheme.typography.titleMedium,
            )

            if (state.accessGranted && state.sessions.isEmpty()) {
                Text("No active media sessions are currently visible. Start playback in a test app and refresh.")
            }

            state.sessions.forEach { session ->
                MediaSessionCard(session = session, onPause = onPause)
            }

            if (state.logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Compatibility log", style = MaterialTheme.typography.titleMedium)
                state.logs.takeLast(10).reversed().forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun MediaSessionCard(
    session: MediaSessionSnapshot,
    onPause: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(session.packageName, style = MaterialTheme.typography.titleSmall)
            Text("Playback: ${session.playbackState}")
            session.title?.let { Text("Title: $it") }
            session.artist?.let { Text("Artist: $it") }
            session.durationMs?.let { duration ->
                Text("Duration: ${duration / 1000}s")
            }
            Text("Pause advertised: ${if (session.pauseSupported) "yes" else "no"}")

            Button(
                onClick = { onPause(session.id) },
                enabled = session.pauseSupported,
            ) {
                Text("Request pause")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    MaterialTheme {
        AutoSleepDashboard(
            mediaState = MediaMonitorState(
                accessGranted = true,
                sessions = listOf(
                    MediaSessionSnapshot(
                        id = "demo",
                        packageName = "com.example.player",
                        playbackState = "PLAYING",
                        title = "Sample video",
                        artist = "Example",
                        durationMs = 600_000,
                        pauseSupported = true,
                        actions = 0L,
                    ),
                ),
                logs = listOf("00:14:52  Manual refresh: 1 active session(s) visible."),
            ),
        )
    }
}
