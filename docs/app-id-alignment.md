# App ID Alignment Notes (Watch)

- Runtime package ID: `default.exposures.ww.app`.
- Kotlin namespace stays `com.exposures.watch`.

## Debug install commands

- Install watch app:
  - `adb -s <watch-serial> install -r app/build/outputs/apk/debug/app-debug.apk`
- Verify package on watch:
  - `adb -s <watch-serial> shell pm list packages | rg default.exposures.ww.app`

## Data reset for migration testing

- Remove aligned package from watch:
  - `adb -s <watch-serial> uninstall default.exposures.ww.app`
