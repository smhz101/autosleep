# AutoSleep Architecture

## Architectural principle

Keep sleep/unattended inference independent from Android UI and platform adapters so signals and scoring can be tested deterministically and later replaced or supplemented by ML.

## Logical pipeline

```text
Android/platform signals
        |
        v
Signal adapters / normalization
        |
        v
Observation snapshot
        |
        v
Confidence engine
        |
        v
Intervention policy
        |
        +--> observe
        +--> prompt user
        +--> request media pause
        |
        v
Local experiment/event log
```

## Planned modules

```text
app/                  Application composition and navigation
core/model/           Shared domain models
core/sensors/         Motion/device-state adapters
core/media/           Media session/playback adapters
core/battery/         Battery and charging observations
engine/signals/       Normalized signal definitions
engine/scoring/       Deterministic confidence scoring
engine/policy/        Intervention state machine
feature/dashboard/    Current protection state
feature/onboarding/   Initial setup
feature/permissions/  Permission explanations and status
feature/intervention/ Still-watching UX
feature/settings/     User configuration
feature/reports/      Local session summaries
data/local/           Local persistence
data/repository/      Domain repositories
data/analytics/       Local experiment aggregation
```

The physical Gradle-module split should remain conservative initially. Package boundaries may precede separate Gradle modules to keep build complexity low during feasibility work.

## Initial domain model

An `ObservationSnapshot` should eventually contain values similar to:

```text
timestamp
screenOn
isCharging
batteryPercent
mediaPlaying
mediaPackage
continuousPlaybackDuration
mediaTransitionCount
stationaryDuration
inactivityDuration
insideSleepWindow
```

The engine returns a `DetectionAssessment`:

```text
score: 0..100
reasons: list of contributing signals
recommendedAction: OBSERVE | PROMPT | PAUSE
```

## Intervention state machine

```text
MONITORING
  -> PROBABLE_UNATTENDED
  -> PROMPTING
      -> USER_CONTINUED -> COOLDOWN
      -> NO_RESPONSE -> PAUSE_REQUESTED
  -> COOLDOWN
  -> MONITORING
```

Cooldown behavior is required to prevent repeated prompts and accidental intervention loops.

## Android integration research

Priority experiments:

1. Enumerate active media sessions with user-granted notification-listener access.
2. Inspect playback state and metadata across representative apps.
3. Request pause through compatible media controllers.
4. Measure behavior for short-form/autoplay apps that do not expose conventional media sessions.
5. Measure process/background limitations across current Android versions.
6. Determine the lowest-energy motion strategy that still provides useful stationary evidence.

## Data policy

The feasibility prototype is local-first. Raw event observations and experiment results remain on the device unless an explicit future research export feature is designed.

## Backend

No backend for v0.1. A server will only be introduced after the core intervention mechanism demonstrates value that requires synchronization, fleet analytics, controlled beta telemetry, or account functionality.
