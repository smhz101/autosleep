# AutoSleep Experiment Plan

## E1 — Media session visibility

Goal: determine which representative Android apps expose useful playback state and metadata.

Record:

- application/package
- media session visible
- playback state available
- metadata available
- transition observable
- pause command supported
- pause command result

### Prototype 0.2 test protocol

1. Install the debug build on a physical Android device.
2. Open AutoSleep and choose **Enable media access**.
3. Explicitly enable AutoSleep under Android's Notification Access settings.
4. Start media playback in one representative application.
5. Return to AutoSleep and choose **Refresh**.
6. Record whether the package, playback state, title/artist, and duration are visible.
7. Change the playing item and verify whether the compatibility log records a metadata or playback transition.
8. If **Pause advertised** is `yes`, choose **Request pause** and verify the target application actually pauses.
9. If pause is not advertised or the request fails, record that case as unsupported rather than treating it as a product-wide failure.
10. Repeat with long-form video, short-form/autoplay video, audio/podcast, streaming, and locally hosted media applications.

The prototype intentionally logs a pause *request* separately from the observed playback-state result. This avoids claiming control when an application exposes a session but ignores or only partially supports external transport commands.

## E2 — Stationary detection energy cost

Goal: identify the lowest-cost strategy that can distinguish active handling from long-term stationary placement.

Compare sensor sampling strategies and record battery impact over controlled periods.

## E3 — Rule-engine false positives

Goal: determine whether deterministic signals can reach useful precision before adding microphone/ML complexity.

Test scenarios:

- actively watching a long movie without touching the phone
- listening intentionally with screen off
- phone stationary while user remains awake
- autoplay after user falls asleep
- media intentionally left playing for ambient sound
- charging vs battery operation

## E4 — Intervention UX

Compare prompt timing and timeout behavior. Measure how frequently an awake user chooses `Continue` and whether cooldown prevents repeated annoyance.

## E5 — Energy ROI

Measure monitoring energy consumption against estimated/observed playback energy prevented.

The product must create positive energy ROI; detection accuracy alone is insufficient.

## Future E6 — Optional audio classifier

Only after E1–E5 establish the baseline, test whether an on-device snore/breathing classifier materially improves precision/recall enough to justify microphone permission and processing cost.
