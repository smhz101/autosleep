package com.autosleep.app.engine

data class SleepObservation(
    val withinSleepWindow: Boolean,
    val inactivityMinutes: Int,
    val stationaryMinutes: Int,
    val mediaPlaying: Boolean,
    val playbackMinutes: Int,
    val mediaTransitions: Int,
    val screenOn: Boolean,
    val charging: Boolean,
)

enum class SleepAction {
    OBSERVE,
    PROMPT,
    PAUSE,
}

data class SleepDecision(
    val score: Int,
    val action: SleepAction,
    val reasons: List<String>,
)

data class SleepThresholds(
    val promptScore: Int = 70,
    val pauseScore: Int = 85,
)

class SleepConfidenceEngine(
    private val thresholds: SleepThresholds = SleepThresholds(),
) {
    fun evaluate(observation: SleepObservation): SleepDecision {
        var score = 0
        val reasons = mutableListOf<String>()

        fun add(points: Int, reason: String) {
            score += points
            reasons += reason
        }

        if (observation.withinSleepWindow) {
            add(10, "Within configured sleep window")
        }

        when {
            observation.inactivityMinutes >= 30 -> add(25, "No interaction for at least 30 minutes")
            observation.inactivityMinutes >= 15 -> add(20, "No interaction for at least 15 minutes")
        }

        when {
            observation.stationaryMinutes >= 30 -> add(25, "Device stationary for at least 30 minutes")
            observation.stationaryMinutes >= 15 -> add(20, "Device stationary for at least 15 minutes")
        }

        if (observation.mediaPlaying && observation.playbackMinutes >= 20) {
            add(15, "Media has continued playing for at least 20 minutes")
        }

        when {
            observation.mediaTransitions >= 5 -> add(20, "Five or more media transitions without interaction")
            observation.mediaTransitions >= 2 -> add(15, "Multiple media transitions without interaction")
        }

        if (observation.screenOn) {
            add(10, "Screen remains on")
        }

        if (!observation.charging && observation.mediaPlaying) {
            add(5, "Battery is discharging while media is active")
        }

        val boundedScore = score.coerceIn(0, 100)
        val action = when {
            boundedScore >= thresholds.pauseScore -> SleepAction.PAUSE
            boundedScore >= thresholds.promptScore -> SleepAction.PROMPT
            else -> SleepAction.OBSERVE
        }

        return SleepDecision(
            score = boundedScore,
            action = action,
            reasons = reasons,
        )
    }
}
