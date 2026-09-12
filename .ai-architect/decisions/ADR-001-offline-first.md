# ADR-001: Offline-first local project source of truth

- Status: Accepted
- Date: 2026-09-12

## Context

The editor must remain useful with airplane mode, unavailable cloud services, interrupted downloads, and unreliable connectivity. Video projects also contain large local media and can require long-running editing sessions.

## Decision

Local project state is authoritative. Cloud services store synchronized metadata and versioned project snapshots only when the user enables cloud functionality. The application exposes cloud access through repository and service interfaces so editor features do not require Supabase availability.

Projects use a versioned serialized document with explicit schema migration. Assets are referenced by stable IDs and local/remote locations rather than embedding binary media in project metadata.

## Consequences

- Offline editing can be tested independently from Supabase.
- Sync must compare revisions and preserve conflicts rather than silently replacing local state.
- Cloud backup and cross-device continuation are additive capabilities.
- Media and render engines can evolve without changing the project persistence contract.
