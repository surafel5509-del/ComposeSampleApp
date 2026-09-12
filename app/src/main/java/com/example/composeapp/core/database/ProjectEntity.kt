package com.example.composeapp.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val projectId: String,
    val name: String,
    val schemaVersion: Int,
    val revision: Long,
    val durationMs: Long,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val frameRate: Int,
    val documentPath: String,
    val syncState: String,
    val updatedAtEpochMs: Long,
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val assetId: String,
    val kind: String,
    val originalUri: String?,
    val localPath: String?,
    val proxyPath: String?,
    val checksum: String?,
    val version: Int,
    val updatedAtEpochMs: Long,
)
