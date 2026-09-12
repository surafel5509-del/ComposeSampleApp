package com.example.composeapp.core.database

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    fun create(context: Context): MoreCutDatabase =
        Room.databaseBuilder(
            context,
            MoreCutDatabase::class.java,
            "morecut.db",
        ).build()
}
