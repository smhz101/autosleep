# AutoSleep Roadmap

## Phase 0 — Foundation

- Product requirements
- Architecture and privacy principles
- Android feasibility map
- Experiment plan
- Development backlog

## Phase 1 — Observation prototype

- Android/Kotlin project
- Compose shell
- Screen/battery state observation
- Media session observation
- Motion/stationary experiment
- Local event logging
- Debug signal dashboard

Exit criterion: produce a trustworthy timestamped signal log during real media sessions.

## Phase 2 — Deterministic inference

- Observation snapshots
- Weighted scoring engine
- Explainable score contributions
- Threshold configuration
- Unit tests for scoring

Exit criterion: classify controlled awake/unattended scenarios offline and on-device.

## Phase 3 — Intervention

- Still-watching prompt
- User response handling
- Cooldown policy
- Media pause requests
- Compatibility matrix

Exit criterion: reliably pause compatible media after a no-response intervention without repeated prompts.

## Phase 4 — Measurement

- Session reports
- Prevented playback estimates
- Monitoring-energy measurements
- False-intervention feedback
- 20–50 person controlled beta design

## Phase 5 — Optional intelligence

Only if baseline evidence supports it:

- On-device audio/snore research
- Personalized inference
- Lightweight ML
- Wearable signals

## Phase 6 — Startup/OEM validation

- Demo video
- Landing page
- Validation metrics
- Pitch deck
- Accelerator applications
- OEM/platform partnership research
- SDK/system-integration proposition
