package com.example.composeapp.core.sync

import kotlinx.serialization.Serializable

@Serializable
enum class SyncState {
    LOCAL_ONLY,
    SYNC_PENDING,
    SYNCING,
    SYNCED,
    CONFLICT,
    SYNC_ERROR,
}

@Serializable
data class SyncMetadata(
    val projectId: String,
    val localRevision: Long,
    val remoteRevision: Long? = null,
    val state: SyncState = SyncState.LOCAL_ONLY,
    val lastError: String? = null,
)
