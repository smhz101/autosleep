# AutoSleep Privacy Principles

AutoSleep operates in a sensitive context: personal device behavior and potentially, in future experiments, bedroom audio. Privacy is therefore an architectural requirement rather than a later compliance task.

## Principles

### Local first

The initial product does not require a backend. Detection and experiment data should remain on-device.

### Minimum permissions

Request only permissions required for an enabled feature. Explain why each permission is needed before sending the user to Android settings or a system permission dialog.

### No raw bedroom audio upload

If audio/snore research is introduced, the default architecture must process audio on-device. Raw microphone audio should not be transmitted to a server as part of normal detection.

### Audio is optional evidence

Snoring or breathing classification must never be treated as proof that a person is asleep and must not be required for the core product.

### No medical claims

AutoSleep is an energy/intent-awareness product, not a medical sleep-monitoring or diagnostic system.

### Explainable intervention

When practical, the application should be able to explain why it intervened, for example:

- no interaction for 27 minutes
- device stationary for 31 minutes
- media continued playing
- inside configured sleep window

### User control

Users must be able to disable protection, change thresholds, continue playback after a prompt, and exclude behavior that creates false positives.

## Future telemetry

Any future cloud telemetry must be explicitly designed, minimized, documented, and separated from raw sensitive sensor data. Aggregate product metrics should be preferred over raw event streams whenever possible.
