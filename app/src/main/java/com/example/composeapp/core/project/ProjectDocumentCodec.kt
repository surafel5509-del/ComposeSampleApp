package com.example.composeapp.core.project

import com.example.composeapp.core.model.Project
import kotlinx.serialization.json.Json

interface ProjectDocumentCodec {
    fun encode(project: Project): String
    fun decode(document: String): Project
}

class KotlinxProjectDocumentCodec(
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        explicitNulls = false
    },
) : ProjectDocumentCodec {
    override fun encode(project: Project): String = json.encodeToString(Project.serializer(), project)

    override fun decode(document: String): Project =
        json.decodeFromString(Project.serializer(), document)
}
