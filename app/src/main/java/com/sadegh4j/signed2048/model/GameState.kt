package com.sadegh4j.signed2048.model

data class GameState(
    val cells: Array<IntArray> = Array(4) { IntArray(4) },
    val score: Int = 0,
    val best: Int = 0,
    val isGameOver: Boolean = false,
    val isGameWon: Boolean = false,
    val winTarget: WinTarget = WinTarget.T2048
) {
    // عمیق کپی برای جلوگیری از mutation مشکلات
    fun deepCopy(): GameState = copy(
        cells = Array(4) { i -> cells[i].copyOf() }
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return score == other.score &&
                best == other.best &&
                isGameOver == other.isGameOver &&
                isGameWon == other.isGameWon &&
                winTarget == other.winTarget &&
                cells.contentDeepEquals(other.cells)
    }

    override fun hashCode(): Int {
        var result = cells.contentDeepHashCode()
        result = 31 * result + score
        result = 31 * result + best
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + isGameWon.hashCode()
        result = 31 * result + winTarget.hashCode()
        return result
    }
}

enum class WinTarget(val value: Int, val label: String) {
    T1024(16, "16"),
    T2048(2048, "2048"),
    T4096(4096, "4096")
}
