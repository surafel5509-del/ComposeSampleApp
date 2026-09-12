package com.example.composeapp.core.editor

import com.example.composeapp.core.model.Project

data class EditorState(
    val project: Project? = null,
    val selectedClipId: String? = null,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val isDirty: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)
