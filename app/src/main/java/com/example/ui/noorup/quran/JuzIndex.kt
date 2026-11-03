package com.example.ui.noorup.quran

/** First ayah of each of the 30 Juz (para), using the standard Madani mushaf divisions. */
object JuzIndex {
    data class Start(val juz: Int, val surah: Int, val ayah: Int)

    val STARTS: List<Start> = listOf(
        Start(1, 1, 1), Start(2, 2, 142), Start(3, 2, 253), Start(4, 3, 93), Start(5, 4, 24),
        Start(6, 4, 148), Start(7, 5, 82), Start(8, 6, 111), Start(9, 7, 88), Start(10, 8, 41),
        Start(11, 9, 93), Start(12, 11, 6), Start(13, 12, 53), Start(14, 15, 1), Start(15, 17, 1),
        Start(16, 18, 75), Start(17, 21, 1), Start(18, 23, 1), Start(19, 25, 21), Start(20, 27, 56),
        Start(21, 29, 46), Start(22, 33, 31), Start(23, 36, 28), Start(24, 39, 32), Start(25, 41, 47),
        Start(26, 46, 1), Start(27, 51, 31), Start(28, 58, 1), Start(29, 67, 1), Start(30, 78, 1)
    )

    /** The Juz that contains [surah]:[ayah]. */
    fun juzOf(surah: Int, ayah: Int): Int {
        var result = 1
        for (s in STARTS) {
            if (s.surah < surah || (s.surah == surah && s.ayah <= ayah)) result = s.juz else break
        }
        return result
    }
}
