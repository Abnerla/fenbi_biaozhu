package com.example.annotation.drawing

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawingEngineTest {
    @Test
    fun undoAndRedoRestoreSnapshots() {
        val engine = DrawingEngine()
        drawStroke(engine, 0f)
        drawStroke(engine, 20f)

        engine.undo()
        assertEquals(1, engine.paths.size)

        engine.redo()
        assertEquals(2, engine.paths.size)
    }

    @Test
    fun newStrokeAfterUndoClearsRedoHistory() {
        val engine = DrawingEngine()
        drawStroke(engine, 0f)
        drawStroke(engine, 20f)
        engine.undo()
        drawStroke(engine, 40f)

        engine.redo()
        assertEquals(2, engine.paths.size)
    }

    @Test
    fun clearCanBeUndoneAndRedone() {
        val engine = DrawingEngine()
        drawStroke(engine, 0f)
        engine.clearAll()
        assertEquals(0, engine.paths.size)

        engine.undo()
        assertEquals(1, engine.paths.size)

        engine.redo()
        assertEquals(0, engine.paths.size)
    }

    private fun drawStroke(engine: DrawingEngine, y: Float) {
        engine.startDrawing(Offset(0f, y))
        engine.continueDrawing(Offset(10f, y))
        engine.endDrawing()
    }
}
