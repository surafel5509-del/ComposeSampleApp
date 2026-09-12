# Project Context

## Product

MoreCut is a commercial-grade Android video editor designed to remain useful without an internet connection while supporting optional cloud catalog and synchronization features.

## Phase 1 outcome

Establish a clean, buildable application shell with a professional Compose UI, explicit navigation destinations, screen state ownership, and an architecture contract that prevents cloud/media concerns from leaking into the editor UI.

## Non-negotiables

- Kotlin + Jetpack Compose + Material 3.
- Offline editing is a first-class path.
- No Firebase.
- No proprietary copied UI, code, or assets from other products.
- No secrets or Supabase service-role credentials in the Android client.
- Local project state must never depend on cloud availability.
- Media processing belongs behind stable interfaces rather than feature UI code.

## Planned bounded areas

- `core`: common models, UI, persistence, storage, network, media contracts.
- `feature`: home, projects, editor, timeline, captions, audio, assets, downloads, export, auth, settings.
- `engine`: media decoding/encoding, render graph, effects, audio, export.
- `sync`: local/cloud revision comparison, queue, retries, conflicts.

Phase 1 keeps the repository small enough to validate the application shell before introducing heavyweight media and persistence infrastructure.
