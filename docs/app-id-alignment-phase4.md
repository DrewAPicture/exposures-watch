# Phase 4 Device Reset/Reinstall (Watch)

Completed migration reset and reinstall for app ID alignment.

- Built debug APK from `fix/phase1-bluetooth-connectivity-watch`.
- Uninstalled prior package IDs from the watch (`com.exposures.watch`, `com.exposures.phone`, `default.exposures.ww.app`).
- Installed fresh debug APK with package `default.exposures.ww.app`.
- Verified install with `pm list packages`.
