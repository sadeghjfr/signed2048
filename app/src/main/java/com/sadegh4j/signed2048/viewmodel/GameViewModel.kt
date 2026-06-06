package com.sadegh4j.signed2048.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sadegh4j.signed2048.data.PreferencesManager
import com.sadegh4j.signed2048.engine.GameEngine
import com.sadegh4j.signed2048.model.GameState
import com.sadegh4j.signed2048.model.WinTarget

class GameViewModel(private val prefs: PreferencesManager) : ViewModel() {

    private val engine = GameEngine()

    private val _state = MutableLiveData<GameState>()
    val state: LiveData<GameState> get() = _state

    private val _canUndo = MutableLiveData(false)
    val canUndo: LiveData<Boolean> get() = _canUndo

    init {
        val loaded   = prefs.loadGame()
        val hasData  = loaded.cells.any { row -> row.any { it != 0 } }
        _state.value = if (hasData) loaded else engine.newGame(loaded.best, loaded.winTarget)

        // اعمال تم ذخیره‌شده هنگام init
        AppCompatDelegate.setDefaultNightMode(prefs.loadThemeMode())
    }

    // ─── حرکت ────────────────────────────────────────────────────

    fun onSwipe(direction: GameEngine.Direction) {
        val current = _state.value ?: return
        engine.saveForUndo(current)
        val next = engine.moveWithWinCheck(current, direction)
        if (next === current) return
        _state.value   = next
        _canUndo.value = engine.canUndo()
        prefs.saveGame(next)
    }

    // ─── کنترل بازی ──────────────────────────────────────────────


    fun resetGame() {
        val current  = _state.value ?: return
        val newState = engine.newGame(current.best, current.winTarget)
        _state.value   = newState
        _canUndo.value = false
        prefs.saveGame(newState)
    }


    fun undo() {
        val prev = engine.undo() ?: return
        _state.value   = prev
        _canUndo.value = false
        prefs.saveGame(prev)
    }

    // ─── تنظیمات ─────────────────────────────────────────────────

    fun setWinTarget(target: WinTarget) {
        val current = _state.value ?: return
        _state.value = current.copy(winTarget = target)
        prefs.saveWinTarget(target)
    }

    /**
     * سوئیچ تم — AppCompatDelegate کار رو می‌کنه،
     * Activity خودش recreate می‌شه، نیازی به LiveData تم نیست.
     */
    fun setThemeMode(mode: Int) {
        prefs.saveThemeMode(mode)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun currentThemeMode(): Int = prefs.loadThemeMode()

    // ─── ذخیره هنگام خروج ────────────────────────────────────────

    fun saveOnExit() {
        _state.value?.let {
            prefs.saveGame(it)
            prefs.saveBest(it.best)
        }
    }

    fun currentWinTarget(): WinTarget = _state.value?.winTarget ?: WinTarget.T2048
    fun currentScore(): Int           = _state.value?.score     ?: 0
    fun currentBest(): Int            = _state.value?.best      ?: 0
}
