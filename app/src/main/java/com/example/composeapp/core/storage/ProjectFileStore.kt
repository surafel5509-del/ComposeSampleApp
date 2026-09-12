package com.example.composeapp.core.storage

import android.content.Context
import com.example.composeapp.core.model.Project
import com.example.composeapp.core.project.KotlinxProjectDocumentCodec
import com.example.composeapp.core.project.ProjectDocumentCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProjectFileStore(
    context: Context,
    private val codec: ProjectDocumentCodec = KotlinxProjectDocumentCodec(),
) {
    private val root = File(context.filesDir, "projects").apply { mkdirs() }

    suspend fun save(project: Project) = withContext(Dispatchers.IO) {
        val projectDirectory = File(root, project.projectId).apply { mkdirs() }
        val temporary = File(projectDirectory, "project.json.tmp")
        val target = File(projectDirectory, "project.json")
        temporary.writeText(codec.encode(project))
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    suspend fun load(projectId: String): Project? = withContext(Dispatchers.IO) {
        val file = File(root, "$projectId/project.json")
        if (!file.exists()) return@withContext null
        codec.decode(file.readText())
    }

    suspend fun delete(projectId: String) = withContext(Dispatchers.IO) {
        File(root, projectId).deleteRecursively()
    }
}
