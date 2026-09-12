package com.example.composeapp.core.undo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoRedoStackTest {
    private data class State(val value: Int)

    private class Add(private val amount: Int) : EditorCommand<State> {
        override fun apply(state: State) = state.copy(value = state.value + amount)
        override fun undo(state: State) = state.copy(value = state.value - amount)
    }

    @Test
    fun executeUndoRedo_preservesCommandHistory() {
        val stack = UndoRedoStack<State>()
        var state = State(0)

        state = stack.execute(Add(4), state)
        assertEquals(4, state.value)
        assertTrue(stack.canUndo())
        assertFalse(stack.canRedo())

        state = stack.undo(state)!!
        assertEquals(0, state.value)
        assertFalse(stack.canUndo())
        assertTrue(stack.canRedo())

        state = stack.redo(state)!!
        assertEquals(4, state.value)
        assertTrue(stack.canUndo())
        assertFalse(stack.canRedo())
    }
}
