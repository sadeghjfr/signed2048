package com.sadegh4j.signed2048.data

import android.content.Context
import android.content.SharedPreferences
import com.sadegh4j.signed2048.model.GameState
import com.sadegh4j.signed2048.model.WinTarget
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("signed2048_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BEST        = "best"
        private const val KEY_SCORE       = "score"
        private const val KEY_WIN_TARGET  = "win_target"
        private const val KEY_THEME_MODE  = "theme_mode"   // -1=system, 1=light, 2=dark
    }

    fun saveGame(state: GameState) {
        prefs.edit().apply {
            for (i in 0..3)
                for (j in 0..3)
                    putInt("cell_${i}_${j}", state.cells[i][j])
            putInt(KEY_BEST, state.best)
            putInt(KEY_SCORE, state.score)
            putInt(KEY_WIN_TARGET, state.winTarget.value)
            apply()
        }
    }

    fun loadGame(): GameState {
        val cells = Array(4) { i -> IntArray(4) { j -> prefs.getInt("cell_${i}_${j}", 0) } }
        val best  = prefs.getInt(KEY_BEST, 0)
        val score = prefs.getInt(KEY_SCORE, 0)
        val winTargetValue = prefs.getInt(KEY_WIN_TARGET, 2048)
        val winTarget = WinTarget.entries.find { it.value == winTargetValue } ?: WinTarget.T2048
        return GameState(cells = cells, score = score, best = best, winTarget = winTarget)
    }

    fun saveBest(best: Int) = prefs.edit { putInt(KEY_BEST, best) }

    fun saveWinTarget(target: WinTarget) =
        prefs.edit { putInt(KEY_WIN_TARGET, target.value) }

    // ذخیره mode اندروید: MODE_NIGHT_NO=1, MODE_NIGHT_YES=2, MODE_NIGHT_FOLLOW_SYSTEM=-1
    fun saveThemeMode(mode: Int) = prefs.edit { putInt(KEY_THEME_MODE, mode) }

    fun loadThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, 2) // پیش‌فرض: دارک
}
