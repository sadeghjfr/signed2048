package com.sadegh4j.signed2048.engine

import com.sadegh4j.signed2048.model.GameState
import com.sadegh4j.signed2048.model.WinTarget
import kotlin.math.abs
import kotlin.math.floor

class GameEngine {

    enum class Direction { LEFT, RIGHT, UP, DOWN }

    // ─── شروع / ریست ───────────────────────────────────────────────

    fun newGame(best: Int, winTarget: WinTarget): GameState {
        val state = GameState(best = best, winTarget = winTarget)
        return dropNextNumber(dropNextNumber(state))
    }



    // ─── حرکت ──────────────────────────────────────────────────────

    /**
     * یک حرکت در جهت داده‌شده اعمال می‌کند.
     * اگر تغییری رخ نداده باشد، state دست‌نخورده برمی‌گردد.
     */
    fun move(state: GameState, direction: Direction): GameState {
        if (state.isGameOver || state.isGameWon) return state

        val (moved, scoreDelta, wonTile) = applyMove(state.cells, direction)

        // اگر هیچ تغییری نبود حرکت نمی‌شمریم
        if (!hasBoardChanged(state.cells, moved)) return state

        val newScore = state.score + scoreDelta
        // best فقط با امتیاز مثبت آپدیت می‌شه
        val newBest  = if (newScore > 0) maxOf(state.best, newScore) else state.best

        val afterDrop = dropNumberOnBoard(moved)
        val gameOver  = checkGameOver(afterDrop)
        val gameWon   = wonTile && !state.isGameWon

        return state.copy(
            cells      = afterDrop,
            score      = newScore,
            best       = newBest,
            isGameOver = gameOver && !gameWon,
            isGameWon  = gameWon
        )
    }

    // ─── منطق داخلی حرکت ──────────────────────────────────────────

    private data class MoveResult(
        val board: Array<IntArray>,
        val scoreDelta: Int,
        val wonTile: Boolean
    )

    private fun applyMove(
        cells: Array<IntArray>,
        direction: Direction
    ): MoveResult {
        // برای سادگی همه جهت‌ها را به LEFT تبدیل می‌کنیم، اعمال می‌کنیم و برمی‌گردانیم
        val rotated = rotate(cells, direction)
        var score = 0
        var won   = false

        val result = Array(4) { row ->
            val line    = rotated[row]
            val (merged, s, w) = mergeLine(line, 0)
            score += s
            if (w) won = true
            merged
        }

        return MoveResult(rotateBack(result, direction), score, won)
    }

    /**
     * یک ردیف را ادغام می‌کند (همیشه به سمت چپ).
     * قوانین:
     *  - سلول‌های صفر را از بین می‌برد
     *  - دو سلول با مقدار یکسان → ضرب‌در۲، امتیاز اضافه
     *  - دو سلول با مقدار قرینه  → هر دو صفر می‌شوند
     */
    private data class LineResult(val line: IntArray, val score: Int, val won: Boolean)

    private fun mergeLine(line: IntArray, winTarget: Int = 0): LineResult {
        // حذف صفرها
        val nonZero = line.filter { it != 0 }.toMutableList()
        var score = 0
        var won   = false
        var i = 0

        while (i < nonZero.size - 1) {
            val a = nonZero[i]
            val b = nonZero[i + 1]

            when {
                a == b -> {
                    val merged = a * 2
                    // مثبت: امتیاز اضافه، منفی: امتیاز کم می‌شه — دقیقاً مثل بازی اصلی
                    score += merged
                    nonZero[i] = merged
                    nonZero.removeAt(i + 1)
                    if (winTarget != 0 && merged == winTarget) won = true
                    i++
                }
                a == -b -> {
                    // خنثی‌سازی — بدون تغییر امتیاز
                    // اول i+1 حذف میشه چون index بزرگ‌تره
                    if (i + 1 < nonZero.size) nonZero.removeAt(i + 1)
                    if (i < nonZero.size) nonZero.removeAt(i)
                    // i ثابت می‌مونه تا تایل بعدی هم بررسی بشه
                }
                else -> i++
            }
        }

        // پر کردن با صفر تا طول ۴
        val result = IntArray(4)
        val filtered = nonZero.filter { it != 0 }
        filtered.forEachIndexed { idx, v -> result[idx] = v }
        return LineResult(result, score, won)
    }

    // چرخش board برای یکسان‌سازی جهت حرکت
    private fun rotate(cells: Array<IntArray>, direction: Direction): Array<IntArray> =
        when (direction) {
            Direction.LEFT  -> cells.map { it.copyOf() }.toTypedArray()
            Direction.RIGHT -> cells.map { it.reversedArray() }.toTypedArray()
            Direction.UP    -> transpose(cells)
            Direction.DOWN  -> transpose(cells).map { it.reversedArray() }.toTypedArray()
        }

    private fun rotateBack(cells: Array<IntArray>, direction: Direction): Array<IntArray> =
        when (direction) {
            Direction.LEFT  -> cells.map { it.copyOf() }.toTypedArray()
            Direction.RIGHT -> cells.map { it.reversedArray() }.toTypedArray()
            Direction.UP    -> transpose(cells)
            Direction.DOWN  -> transpose(cells.map { it.reversedArray() }.toTypedArray())
        }

    private fun transpose(cells: Array<IntArray>): Array<IntArray> =
        Array(4) { i -> IntArray(4) { j -> cells[j][i] } }

    // ─── تولید عدد جدید ───────────────────────────────────────────

    private fun dropNextNumber(state: GameState): GameState {
        if (state.isGameOver) return state
        val newBoard = dropNumberOnBoard(state.cells)
        return state.copy(cells = newBoard)
    }

    fun dropNumberOnBoard(cells: Array<IntArray>): Array<IntArray> {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0..3) for (j in 0..3) if (cells[i][j] == 0) emptyCells.add(i to j)
        if (emptyCells.isEmpty()) return cells

        val (row, col) = emptyCells.random()
        val newBoard   = Array(4) { i -> cells[i].copyOf() }

        // نرخ تولید: 2=70%, 4=15%, -2=10%, -4=5%
        val r = Math.random()
        newBoard[row][col] = when {
            r < 0.05 -> -4
            r < 0.15 -> -2
            r < 0.30 ->  4
            else     ->  2
        }

        return newBoard
    }

    // ─── بررسی پایان بازی ─────────────────────────────────────────

    fun checkGameOver(cells: Array<IntArray>): Boolean {
        // اگر خانه خالی باشد، بازی تموم نشده
        for (i in 0..3) for (j in 0..3) if (cells[i][j] == 0) return false

        // بررسی ادغام‌های افقی
        for (i in 0..3) for (j in 0..2) {
            val a = cells[i][j]; val b = cells[i][j + 1]
            if (a == b || a == -b) return false
        }

        // بررسی ادغام‌های عمودی
        for (i in 0..2) for (j in 0..3) {
            val a = cells[i][j]; val b = cells[i + 1][j]
            if (a == b || a == -b) return false
        }

        return true
    }

    // ─── undo ──────────────────────────────────────────────────────

    // تاریخچه برای undo - نگه‌داری در Engine (max 1 حرکت)
    private var previousState: GameState? = null

    fun saveForUndo(state: GameState) {
        previousState = state.deepCopy()
    }

    fun undo(): GameState? {
        return previousState?.also { previousState = null }
    }

    fun canUndo(): Boolean = previousState != null

    // ─── کمکی ─────────────────────────────────────────────────────

    private fun hasBoardChanged(old: Array<IntArray>, new: Array<IntArray>): Boolean {
        for (i in 0..3) for (j in 0..3) if (old[i][j] != new[i][j]) return true
        return false
    }

    // اعمال mergeLine با winTarget از GameState (برای استفاده در applyMove کامل)
    fun moveWithWinCheck(state: GameState, direction: Direction): GameState {
        if (state.isGameOver || state.isGameWon) return state

        val rotated = rotate(state.cells, direction)
        var scoreDelta = 0
        var wonTile    = false

        val result = Array(4) { row ->
            val line = rotated[row]
            val (merged, s, w) = mergeLine(line, state.winTarget.value)
            scoreDelta += s
            if (w) wonTile = true
            merged
        }

        val moved = rotateBack(result, direction)

        if (!hasBoardChanged(state.cells, moved)) return state

        val newScore = state.score + scoreDelta
        // best فقط با امتیاز مثبت آپدیت می‌شه
        val newBest  = if (newScore > 0) maxOf(state.best, newScore) else state.best
        val afterDrop = dropNumberOnBoard(moved)
        val gameOver  = checkGameOver(afterDrop)
        val gameWon   = wonTile && !state.isGameWon

        return state.copy(
            cells      = afterDrop,
            score      = newScore,
            best       = newBest,
            isGameOver = gameOver && !gameWon,
            isGameWon  = gameWon
        )
    }
}
