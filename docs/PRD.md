# AutoSleep Product Requirements Document

## Problem

People frequently fall asleep while video, audio, livestream, or autoplay content continues running on their phones. Conventional sleep timers require the user to predict that they will fall asleep. AutoSleep targets unintended continued consumption after user intent has effectively ended.

## Product hypothesis

A combination of inexpensive device and media signals can identify probable unattended playback accurately enough to intervene without frequently interrupting awake users.

## MVP objective

Prove that an Android application can:

1. Observe sufficient device/media state to estimate unattended playback.
2. Calculate a transparent sleep/unattended confidence score.
3. Prompt the user before intervention.
4. Pause compatible media when the user does not respond.
5. Record enough local data to measure accuracy and energy benefit.

## Primary user story

As a user who sometimes falls asleep while consuming media, I want my phone to recognize likely unintended playback and stop it automatically so that battery, energy, and playback time are not wasted.

## MVP signals

- Configured sleep window / time context
- Time since meaningful user interaction where observable
- Device stationary duration
- Media playback state
- Continuous playback duration
- Media item/session changes where observable
- Screen state
- Battery level and charging state

## Intervention policy

The engine produces a confidence score from 0–100. Thresholds must be configurable during experimentation.

Initial experimental policy:

- Below 70: observe only.
- 70–84: prompt `Still watching?`.
- 85+ after no response: request media pause where supported.

These thresholds are experimental and must not be presented as scientifically validated.

## MVP screens

1. Dashboard
2. Protection setup
3. Permissions/setup
4. Still-watching intervention
5. Nightly/session report

## Success metrics

### Detection precision
Percentage of probable-sleep detections that correspond to genuinely unattended playback.

### False-intervention rate
Percentage of interventions that interrupt intentional media consumption.

### Prevented playback
Minutes of playback stopped after probable sleep/unattended detection.

### Energy ROI
Estimated energy prevented divided by energy consumed by AutoSleep monitoring.

## Explicit non-goals for v0.1

- iOS implementation
- Cloud accounts or synchronization
- Backend APIs
- Remote audio storage
- General-purpose parental controls
- Medical sleep diagnosis
- Claims that snoring proves sleep
- Guaranteed control of every third-party media application

## Future research

- On-device snore/breathing classification as an optional confidence signal
- Personalized thresholds
- Lightweight on-device ML
- Wearable integration
- OEM/system integration
- TV/tablet support
- Energy and battery-cycle modeling
