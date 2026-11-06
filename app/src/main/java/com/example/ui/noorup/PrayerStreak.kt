package com.example.ui.noorup

import android.content.SharedPreferences
import java.time.LocalDate

/**
 * The one place the "all five prayers" streak is calculated, shared by the app and the home-screen
 * widget so they can never disagree. The widget used to read a stored number that only changed when
 * a prayer was ticked, so it kept showing an old streak after a day had been missed.
 *
 * Today only needs to be complete for the streak to include it; until then the streak stays alive
 * if yesterday was complete.
 */
object PrayerStreak {
    fun compute(prefs: SharedPreferences, todayCompleted: Int, today: LocalDate = LocalDate.now()): Int {
        fun completedOn(daysAgo: Int): Int =
            prefs.getInt("prayer_record_${today.minusDays(daysAgo.toLong())}_completed", 0)

        val startIndex: Int
        var streak: Int
        if (todayCompleted >= 5) {
            streak = 1
            startIndex = 1
        } else if (completedOn(1) >= 5) {
            streak = 1
            startIndex = 2
        } else {
            return 0
        }
        for (i in startIndex..365) {
            if (completedOn(i) >= 5) streak++ else break
        }
        return streak
    }
}
