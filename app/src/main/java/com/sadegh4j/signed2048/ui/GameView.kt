package com.sadegh4j.signed2048.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import com.sadegh4j.signed2048.R
import com.sadegh4j.signed2048.engine.GameEngine
import kotlin.math.abs
import kotlin.math.min

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    var onSwipe: ((GameEngine.Direction) -> Unit)? = null

    private var cells: Array<IntArray> = Array(4) { IntArray(4) }
    private var isDark: Boolean = true

    // رنگ‌ها از theme و resources — بدون hardcode

    // ─── انیمیشن merge ───────────────────────────────────────────
    private data class TileAnim(val row: Int, val col: Int, var scale: Float = 1f)
    private val mergeAnims = mutableListOf<TileAnim>()

    // ─── Paint ───────────────────────────────────────────────────

    private val gameTypeface by lazy {
        context.resources.getFont(R.font.telma)
    }

    private val tilePaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface  = gameTypeface
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // ─── اندازه‌ها ────────────────────────────────────────────────
    private var boardSize = 0f
    private var cellSize  = 0f
    private var padding   = 0f
    private var offsetX   = 0f
    private var offsetY   = 0f
    private val cornerRadius get() = cellSize * 0.12f

    // ─── touch ───────────────────────────────────────────────────
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var movePending  = false

    // ─── Public API ───────────────────────────────────────────────

    fun updateCells(newCells: Array<IntArray>, isDarkTheme: Boolean) {
        for (i in 0..3) for (j in 0..3) {
            if (newCells[i][j] != 0
                && newCells[i][j] != cells[i][j]
                && abs(newCells[i][j]) == abs(cells[i][j]) * 2
            ) startMergeAnim(i, j)
        }
        cells  = newCells
        isDark = isDarkTheme
        invalidate()
    }

    // ─── اندازه‌گیری ──────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        boardSize = min(w, h).toFloat()
        padding   = boardSize * 0.035f
        cellSize  = (boardSize - padding * 5) / 4f
        offsetX   = (w - boardSize) / 2f
        offsetY   = (h - boardSize) / 2f
    }

    // ─── رسم ─────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBoard(canvas)
        drawCells(canvas)
    }

    private fun drawBoard(canvas: Canvas) {
        bgPaint.color = if (isDark)
            Color.parseColor("#141414")
        else
            Color.parseColor("#F0E6CC")
        val rect = RectF(offsetX, offsetY, offsetX + boardSize, offsetY + boardSize)
        canvas.drawRoundRect(rect, cornerRadius * 2, cornerRadius * 2, bgPaint)
    }

    private fun drawCells(canvas: Canvas) {
        for (i in 0..3) for (j in 0..3) {
            val value = cells[i][j]
            val cx    = offsetX + padding + j * (cellSize + padding) + cellSize / 2
            val cy    = offsetY + padding + i * (cellSize + padding) + cellSize / 2
            val scale = mergeAnims.find { it.row == i && it.col == j }?.scale ?: 1f
            drawCell(canvas, value, cx, cy, scale)
        }
    }

    private fun drawCell(canvas: Canvas, value: Int, cx: Float, cy: Float, scale: Float) {
        val half = cellSize / 2 * scale
        val rect = RectF(cx - half, cy - half, cx + half, cy + half)

        if (value != 0) {
            shadowPaint.color = Color.argb(40, 0, 0, 0)
            canvas.drawRoundRect(
                RectF(rect.left + 3, rect.top + 3, rect.right + 3, rect.bottom + 3),
                cornerRadius, cornerRadius, shadowPaint
            )
        }

        tilePaint.color = getTileColor(value)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tilePaint)

        if (value != 0) {
            val text = value.toString()
            val textSize = when {
                abs(value) >= 10000 -> cellSize * 0.28f
                abs(value) >= 1000  -> cellSize * 0.32f
                abs(value) >= 100   -> cellSize * 0.36f
                else                -> cellSize * 0.42f
            }
            textPaint.textSize = textSize
            textPaint.color    = getTileTextColor(value)
            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)
            canvas.drawText(text, cx, cy + bounds.height() / 2f, textPaint)
        }
    }

    // ─── رنگ تایل‌ها ─────────────────────────────────────────────
    // Canvas با theme attributes کار نمی‌کنه — رنگ‌ها از colors.xml می‌خونیم

    private fun getTileColor(value: Int): Int = if (isDark) {
        when (value) {
            0      -> Color.parseColor("#242424")
            2      -> Color.parseColor("#3D1A00")
            4      -> Color.parseColor("#6B2E00")
            8      -> Color.parseColor("#994400")
            16     -> Color.parseColor("#C05500")
            32     -> Color.parseColor("#E06200")
            64     -> Color.parseColor("#FF6B00")
            128    -> Color.parseColor("#FF8533")
            256    -> Color.parseColor("#FF9F66")
            512    -> Color.parseColor("#FFB899")
            1024   -> Color.parseColor("#FFD0BB")
            2048   -> Color.parseColor("#FFE8D9")
            -2     -> Color.parseColor("#1A0033")
            -4     -> Color.parseColor("#2D0057")
            -8     -> Color.parseColor("#45008A")
            -16    -> Color.parseColor("#6600B3")
            -32    -> Color.parseColor("#8800CC")
            -64    -> Color.parseColor("#9B00E0")
            -128   -> Color.parseColor("#AA22E8")
            -256   -> Color.parseColor("#BB44F0")
            -512   -> Color.parseColor("#CC66F5")
            -1024  -> Color.parseColor("#DD88F8")
            -2048  -> Color.parseColor("#EEAAFB")
            else   -> if (value > 0) Color.parseColor("#FFF2EE") else Color.parseColor("#F4CCFD")
        }
    } else {
        when (value) {
            0      -> Color.parseColor("#E8D9BB")
            2      -> Color.parseColor("#FFE5CC")
            4      -> Color.parseColor("#FFCB99")
            8      -> Color.parseColor("#FFB066")
            16     -> Color.parseColor("#FF9933")
            32     -> Color.parseColor("#FF7700")
            64     -> Color.parseColor("#E06200")
            128    -> Color.parseColor("#C05000")
            256    -> Color.parseColor("#A04000")
            512    -> Color.parseColor("#803300")
            1024   -> Color.parseColor("#602800")
            2048   -> Color.parseColor("#401800")
            -2     -> Color.parseColor("#E0EEFF")
            -4     -> Color.parseColor("#B8D4FF")
            -8     -> Color.parseColor("#8AB8FF")
            -16    -> Color.parseColor("#6699FF")
            -32    -> Color.parseColor("#4477EE")
            -64    -> Color.parseColor("#3355DD")
            -128   -> Color.parseColor("#2244CC")
            -256   -> Color.parseColor("#1133BB")
            -512   -> Color.parseColor("#0022AA")
            -1024  -> Color.parseColor("#001899")
            -2048  -> Color.parseColor("#001188")
            else   -> if (value > 0) Color.parseColor("#201000") else Color.parseColor("#000544")
        }
    }

    private fun getTileTextColor(value: Int): Int = if (isDark) {
        when {
            value > 0 && value <= 32   -> Color.parseColor("#FF9F66")
            value > 0                  -> Color.parseColor("#1A1A1A")
            value < 0 && value >= -32  -> Color.parseColor("#CC66F5")
            else                       -> Color.parseColor("#F5F5F5")
        }
    } else {
        when {
            value > 0 && value <= 16   -> Color.parseColor("#7A3A00")
            value > 0                  -> Color.parseColor("#FAF3E0")
            value < 0 && value >= -16  -> Color.parseColor("#1133BB")
            else                       -> Color.parseColor("#FAF3E0")
        }
    }

    // ─── انیمیشن merge ───────────────────────────────────────────

    private fun startMergeAnim(row: Int, col: Int) {
        val anim = TileAnim(row, col)
        mergeAnims.removeAll { it.row == row && it.col == col }
        mergeAnims.add(anim)
        ValueAnimator.ofFloat(1f, 1.2f, 1f).apply {
            duration     = 160
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                anim.scale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // ─── touch ───────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.rawX
                touchStartY = event.rawY
                movePending = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!movePending) return true
                val dx = touchStartX - event.rawX
                val dy = touchStartY - event.rawY
                val direction = when {
                    abs(dx) > 10 && abs(dx) > 2 * abs(dy) ->
                        if (dx > 0) GameEngine.Direction.LEFT else GameEngine.Direction.RIGHT
                    abs(dy) > 10 && abs(dy) > 2 * abs(dx) ->
                        if (dy > 0) GameEngine.Direction.UP else GameEngine.Direction.DOWN
                    else -> null
                }
                if (direction != null) {
                    movePending = false
                    onSwipe?.invoke(direction)
                }
            }
        }
        return true
    }

    // ─── کمکی ────────────────────────────────────────────────────

}
