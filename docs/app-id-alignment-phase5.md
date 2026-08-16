# Phase 5 Connectivity Matrix (Watch-Side Evidence)

Automated verification completed on aligned debug builds.

- Unit tests pass (`./gradlew :app:test`) with package ID `default.exposures.ww.app`.
- Package install verified on device (`pm list packages` includes `default.exposures.ww.app`).
- `dumpsys package` confirms `WearMessageListenerService` registration for:
  - `/rolls`
  - `/command/connectivity-ping-ack`
- `dumpsys activity service com.google.android.gms/.wearable.service.WearableService` shows the watch and phone nodes connected through companion transport.

Manual UI verification is still expected for watch "Refresh from phone" behavior against live phone data.
