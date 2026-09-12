package com.example.composeapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, AssetEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MoreCutDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun assetDao(): AssetDao
}
