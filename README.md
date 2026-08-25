# AutoSleep

AutoSleep is an Android-first research and product project focused on detecting when active mobile media consumption has unintentionally continued after the user falls asleep.

The initial prototype combines low-cost device and playback signals to estimate probable sleep/unattended usage, asks the user whether they are still watching, and—where Android permits—pauses active media to reduce unnecessary battery and energy consumption.

## Project goals

- Detect probable unintended media playback with a low false-intervention rate.
- Minimize the energy cost of the detection system itself.
- Prefer privacy-preserving, on-device processing.
- Measure prevented playback time and estimated energy savings.
- Establish technical feasibility across common Android media applications.

## MVP v0.1

The first prototype uses deterministic signals rather than machine learning:

- Time of day / configured sleep window
- User inactivity
- Device stationary duration
- Active media playback
- Continuous playback duration
- Media item changes
- Screen state
- Battery state

Audio/snore detection is intentionally deferred until the non-audio approach has been measured.

## Technology direction

- Android
- Kotlin
- Jetpack Compose
- Local-first data storage
- Android media/session APIs
- Android sensor APIs

No backend is required for the initial feasibility prototype.

## Documentation

See `docs/` for product requirements, architecture, privacy principles, experiments, and roadmap.

## Status

Phase 0 — product definition and Android feasibility prototype.
