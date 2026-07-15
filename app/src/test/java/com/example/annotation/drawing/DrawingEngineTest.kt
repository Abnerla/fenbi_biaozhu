package com.example.annotation.drawing

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun pressureIsNormalizedBeforeItIsStored() {
        val engine = DrawingEngine()

        engine.startDrawing(Offset.Zero, -1f)
        engine.continueDrawing(Offset(10f, 0f), 2f)

        assertEquals(0f, engine.getCurrentDrawingPath()[0].pressure, 0f)
        assertEquals(1f, engine.getCurrentDrawingPath()[1].pressure, 0f)
        assertEquals(10f, engine.calculatePressureAdjustedWidth(10f, 2f), 0f)
    }

    @Test
    fun eraserSizeAndHitAreaFollowPressure() {
        val engine = DrawingEngine()
        drawStroke(engine, 0f)
        engine.setTool(com.example.annotation.model.DrawingTool.ERASER)
        engine.updateEraserSize(20f)

        engine.startDrawing(Offset(5f, 8f), pressure = 0f)
        assertEquals(6f, engine.currentEraserSize.value ?: 0f, 0f)
        engine.endDrawing()
        assertEquals(1, engine.paths.size)
        assertNull(engine.currentEraserSize.value)

        engine.startDrawing(Offset(5f, 8f), pressure = 1f)
        assertEquals(20f, engine.currentEraserSize.value ?: 0f, 0f)
        engine.endDrawing()
        assertEquals(0, engine.paths.size)
    }

    private fun drawStroke(engine: DrawingEngine, y: Float) {
        engine.startDrawing(Offset(0f, y))
        engine.continueDrawing(Offset(10f, y))
        engine.endDrawing()
    }
}
