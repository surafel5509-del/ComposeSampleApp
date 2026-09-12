package com.example.composeapp.core.undo

interface EditorCommand<S> {
    fun apply(state: S): S
    fun undo(state: S): S
}

class UndoRedoStack<S>(private val maxDepth: Int = 100) {
    private val undoStack = ArrayDeque<EditorCommand<S>>()
    private val redoStack = ArrayDeque<EditorCommand<S>>()

    fun execute(command: EditorCommand<S>, state: S): S {
        val next = command.apply(state)
        undoStack.addLast(command)
        if (undoStack.size > maxDepth) undoStack.removeFirst()
        redoStack.clear()
        return next
    }

    fun undo(state: S): S? {
        val command = undoStack.removeLastOrNull() ?: return null
        val previous = command.undo(state)
        redoStack.addLast(command)
        return previous
    }

    fun redo(state: S): S? {
        val command = redoStack.removeLastOrNull() ?: return null
        val next = command.apply(state)
        undoStack.addLast(command)
        return next
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
}
