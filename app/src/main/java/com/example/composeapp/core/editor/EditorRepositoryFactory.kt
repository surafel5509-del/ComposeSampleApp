package com.example.composeapp.core.editor

import android.content.Context
import com.example.composeapp.core.database.DatabaseFactory
import com.example.composeapp.core.project.OfflineProjectRepository
import com.example.composeapp.core.storage.ProjectFileStore

object EditorRepositoryFactory {
    fun create(context: Context): OfflineProjectRepository {
        val database = DatabaseFactory.create(context)
        return OfflineProjectRepository(database.projectDao(), ProjectFileStore(context))
    }
}
