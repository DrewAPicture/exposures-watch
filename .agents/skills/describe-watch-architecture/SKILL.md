---
name: describe-watch-architecture
description: Describes the current exposures-watch architecture, including module ownership, navigation/state flow, local persistence strategy, and phone sync behavior. Use when asked about watch app architecture or where watch-side changes belong.
disable-model-invocation: true
---

# Describe Watch Architecture

Use this skill to explain the current `exposures-watch` architecture accurately and consistently.

## Scope

Cover implemented architecture:

- module boundaries
- app wiring and lifecycle
- navigation/view-model structure
- local persistence and sync apply behavior
- capture request and result handling
- tile integration

## Module map

- `app`
  - Wear entry points, Compose screens/navigation, view models, sync send/receive wiring, tile service.
- `core-model`
  - domain model shared by app/database/datalayer.
- `core-database`
  - Room schema/DAOs/repository (`ExposureRepository`) and mapping.
- `core-datalayer`
  - Wear Data Layer client/gateway, paths, DTO/json mapping.

## App wiring

- `ExposuresApplication`
  - builds `DefaultAppContainer`
  - triggers background seeding via repository.
- `DefaultAppContainer`
  - manual DI (no Hilt)
  - provides:
    - `ExposureRepository`
    - `DataLayerClient`
    - `ExposurePusher`
    - `CaptureRequestSender`
    - `RollCompletionSender`
    - `RollsSyncRequestSender`.

## Navigation and state flow

- `ExposuresNavHost` start destination: roll switcher.
- Main flow:
  - roll switcher -> roll detail -> exposure entry -> frame history/detail.
- `ExposureEntryScreen` uses one view model state machine for picker + confirm steps.
- After save:
  - local exposure saved first
  - exposure sync push + capture request sent to phone.

## Local data strategy

- Watch keeps a local mirror DB for:
  - equipment
  - rolls
  - exposures
  - app state (active roll, last used settings)
  - capture outbox queue.
- Sync apply model is merge/upsert-oriented (non-destructive in refresh path) to avoid FK crashes with historical exposures.

## Phone/watch boundary

- `WearMessageListenerService` on watch handles:
  - DATA_CHANGED payloads for equipment/roll/photo status
  - MESSAGE_RECEIVED for capture-result and connectivity ping ack.
- `EquipmentSyncReceiver` applies incoming phone payloads to local mirror repository.
- `RollSwitcherViewModel` manual refresh uses `RollsSyncRequestSender` (`ping` + `request-rolls-sync`).

## Refresh behavior (current)

- Refresh is a request for latest phone snapshot, not a destructive reset.
- Expected outcome:
  - rehydrate missing equipment/rolls
  - preserve historical rows referenced by saved exposures
  - avoid refresh-time FK crashes.

## Tile behavior

- `ExposuresTileService` resolves content from local active roll/exposure state.
- Tile tap launches `MainActivity`, optionally deep-linking to exposure entry via roll id extra.
- Startup UX now shows explicit loading text in roll switcher to reduce "blank screen" ambiguity.

## How to answer architecture questions

When responding:

1. Start from local-first watch behavior.
2. Identify owning class for each step in the flow.
3. Distinguish:
  - local persistence concerns
  - sync transport concerns
  - UI/navigation concerns.
4. For proposed changes, point to smallest owning file set first.

