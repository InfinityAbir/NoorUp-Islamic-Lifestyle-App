package com.example.ui.noorup

/**
 * Order of the sub-tabs on the Tools screen. Each tab keeps a stable id (the index it always had), so the
 * screen's content never depends on position and the user can reorder freely.
 *
 * Ids: 0 Prayer Alerts, 1 Digital Tasbih, 2 Solar Engine, 3 Qibla, 4 Hijri, 5 Zakat, 6 Home Widget,
 * 7 Ramadan, 8 Family Dhikr, 9 Backup.
 */
object ToolTabOrder {
    const val TOOL_COUNT = 10

    /** Default: Family Dhikr sits right after Digital Tasbih. */
    val DEFAULT: List<Int> = listOf(0, 1, 8, 2, 3, 4, 5, 6, 7, 9)

    /**
     * Reads a saved order. Unknown or duplicate ids are dropped, and any tab missing from the saved list
     * (for example one added in a later version) is appended in its default position order, so a stale
     * or empty value can never hide a tab.
     */
    fun decode(raw: String?): List<Int> {
        val saved = raw.orEmpty().split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 0 until TOOL_COUNT }
            .distinct()
        return saved + DEFAULT.filter { it !in saved }
    }

    fun encode(order: List<Int>): String = order.joinToString(",")

    /** Moves the item at [from] to position [to]; out-of-range requests leave the list unchanged. */
    fun move(order: List<Int>, from: Int, to: Int): List<Int> {
        if (from !in order.indices || to !in order.indices || from == to) return order
        val out = order.toMutableList()
        out.add(to, out.removeAt(from))
        return out
    }
}
