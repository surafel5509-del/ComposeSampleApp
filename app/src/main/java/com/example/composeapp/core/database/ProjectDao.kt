package com.example.composeapp.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtEpochMs DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE projectId = :projectId LIMIT 1")
    suspend fun findById(projectId: String): ProjectEntity?

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE projectId = :projectId")
    suspend fun delete(projectId: String)
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets WHERE assetId = :assetId LIMIT 1")
    suspend fun findById(assetId: String): AssetEntity?

    @Upsert
    suspend fun upsert(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE assetId = :assetId")
    suspend fun delete(assetId: String)
}
