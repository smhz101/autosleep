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
