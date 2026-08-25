package com.autosleep.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepConfidenceEngineTest {
    private val engine = SleepConfidenceEngine()

    @Test
    fun lowSignalObservation_remainsObserve() {
        val decision = engine.evaluate(
            SleepObservation(
                withinSleepWindow = false,
                inactivityMinutes = 2,
                stationaryMinutes = 1,
                mediaPlaying = true,
                playbackMinutes = 3,
                mediaTransitions = 0,
                screenOn = true,
                charging = true,
            ),
        )

        assertEquals(SleepAction.OBSERVE, decision.action)
        assertTrue(decision.score < 70)
    }

    @Test
    fun accumulatedSignals_reachPauseThreshold() {
        val decision = engine.evaluate(
            SleepObservation(
                withinSleepWindow = true,
                inactivityMinutes = 35,
                stationaryMinutes = 35,
                mediaPlaying = true,
                playbackMinutes = 40,
                mediaTransitions = 6,
                screenOn = true,
                charging = false,
            ),
        )

        assertEquals(100, decision.score)
        assertEquals(SleepAction.PAUSE, decision.action)
    }

    @Test
    fun score_isCappedAtOneHundred() {
        val decision = engine.evaluate(
            SleepObservation(
                withinSleepWindow = true,
                inactivityMinutes = 60,
                stationaryMinutes = 60,
                mediaPlaying = true,
                playbackMinutes = 120,
                mediaTransitions = 20,
                screenOn = true,
                charging = false,
            ),
        )

        assertEquals(100, decision.score)
    }
}
