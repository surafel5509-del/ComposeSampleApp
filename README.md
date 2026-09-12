# MoreCut — Android Video Editor

Phase 1 foundation for a professional, offline-first Android video editor.

## Current scope

- Kotlin + Jetpack Compose + Material 3
- Compose Navigation
- Screen-level ViewModel state
- Offline/online/sync status shell
- Home, Projects, and Settings destinations
- New-project entry point
- Architecture/ADR documentation

Media rendering, timeline editing, Room persistence, download management, Supabase sync, and export are intentionally staged for later phases. No cloud dependency is required for the current UI shell.

## Architecture direction

The product is offline-first: local project state is authoritative and cloud synchronization is an optional transport. The eventual module boundaries are:

`app -> feature -> domain -> core/infrastructure`

with dedicated media/render/sync/asset engines behind interfaces. Project documents are versioned and asset references are separated from binary media.

## Development

Use the Gradle wrapper and verify the project locally before merging. CI will become the authoritative clean-build gate as the foundation grows.

## Branching

Phase work is developed on focused branches and merged through pull requests.
