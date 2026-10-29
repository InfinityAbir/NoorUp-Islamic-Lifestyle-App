package com.example

import com.example.ui.noorup.ToolTabOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolTabOrderTest {

    @Test
    fun `default puts family dhikr right after digital tasbih`() {
        val d = ToolTabOrder.DEFAULT
        assertEquals(ToolTabOrder.TOOL_COUNT, d.size)
        assertEquals((0 until ToolTabOrder.TOOL_COUNT).toSet(), d.toSet())
        assertEquals(1, d[1])          // Digital Tasbih
        assertEquals(8, d[2])          // Family Dhikr
    }

    @Test
    fun `nothing saved gives the default order`() {
        assertEquals(ToolTabOrder.DEFAULT, ToolTabOrder.decode(null))
        assertEquals(ToolTabOrder.DEFAULT, ToolTabOrder.decode(""))
    }

    @Test
    fun `a saved order round trips`() {
        val custom = listOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
        assertEquals(custom, ToolTabOrder.decode(ToolTabOrder.encode(custom)))
    }

    @Test
    fun `bad saved values can never hide or duplicate a tab`() {
        val decoded = ToolTabOrder.decode("5,5,99,abc,-1,2")
        assertEquals(listOf(5, 2), decoded.take(2))
        assertEquals(ToolTabOrder.TOOL_COUNT, decoded.size)
        assertEquals((0 until ToolTabOrder.TOOL_COUNT).toSet(), decoded.toSet())
        // A list saved by an older version with fewer tabs gets the missing ones appended.
        assertTrue(ToolTabOrder.decode("3,2,1,0").containsAll(ToolTabOrder.DEFAULT))
    }

    @Test
    fun `move shifts one tab and ignores bad indexes`() {
        val base = listOf(0, 1, 2, 3)
        assertEquals(listOf(1, 2, 0, 3), ToolTabOrder.move(base, 0, 2))
        assertEquals(listOf(3, 0, 1, 2), ToolTabOrder.move(base, 3, 0))
        assertEquals(base, ToolTabOrder.move(base, 1, 1))
        assertEquals(base, ToolTabOrder.move(base, 4, 0))
        assertEquals(base, ToolTabOrder.move(base, 0, -1))
    }
}
