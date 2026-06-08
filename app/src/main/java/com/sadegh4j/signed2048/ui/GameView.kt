package com.sadegh4j.signed2048.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
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
    private val tilePaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gameTypeface by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.resources.getFont(com.sadegh4j.signed2048.R.font.black_ops)
        } else {
            Typeface.createFromAsset(context.assets, "fonts/telma.ttf")
        }
    }
    private val textPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val emptyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 1.5f
        color       = Color.argb(30, 0, 0, 0)
    }

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

        val (startColor, endColor) = getTileGradientColors(value)
        tilePaint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            startColor, endColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, tilePaint)
        tilePaint.shader = null
        // border شیشه‌ای برای تایل خالی لایت
        if (value == 0 && !isDark) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, emptyBorderPaint)
        }

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
            textPaint.typeface = gameTypeface
            val bounds = Rect()
            textPaint.getTextBounds(text, 0, text.length, bounds)
            canvas.drawText(text, cx, cy + bounds.height() / 2f, textPaint)
        }
    }

    // ─── رنگ تایل‌ها ─────────────────────────────────────────────
    // Canvas با theme attributes کار نمی‌کنه — رنگ‌ها از colors.xml می‌خونیم

    private fun getTileGradientColors(value: Int): Pair<Int, Int> = if (isDark) {
        when (value) {

            0      -> gc("#303030", "#080808")
            2      -> gc("#000000", "#210800")
            4      -> gc("#210800", "#4D1600")
            8      -> gc("#4D1600", "#7A2800")
            16     -> gc("#7A2800", "#A83B00")
            32     -> gc("#A83B00", "#D64F00")
            64     -> gc("#D64F00", "#FF6600")
            128    -> gc("#FF6600", "#FF8833")
            256    -> gc("#FF8833", "#FFA866")
            512    -> gc("#FFA866", "#FFC799")
            1024   -> gc("#FFC799", "#FFE0C2")
            2048   -> gc("#FFE0C2", "#FFF0E0")
            4096   -> gc("#FFF0E0", "#FFFFFF")
            8192   -> gc("#FFFFFF", "#FFFFFF")


            -2      -> gc("#000000", "#001321")
            -4      -> gc("#001321", "#002642")
            -8      -> gc("#002642", "#003C66")
            -16     -> gc("#003C66", "#005691")
            -32     -> gc("#005691", "#0073C2")
            -64     -> gc("#0073C2", "#0088FF")
            -128    -> gc("#0088FF", "#33A0FF")
            -256    -> gc("#33A0FF", "#66B8FF")
            -512    -> gc("#66B8FF", "#99D1FF")
            -1024   -> gc("#99D1FF", "#CCE8FF")
            -2048   -> gc("#CCE8FF", "#EBF5FF")
            -4096   -> gc("#EBF5FF", "#FFFFFF")
            -8192   -> gc("#FFFFFF", "#FFFFFF")

            else   -> if (value > 0) gc("#FFFFFF", "#000A18") else gc("#CCFFFF", "#001A1A")
        }
    } else {
        when (value) {


            0      -> gc("#FFFFFF", "#D8C8B8", 0.6f)
            2      -> gc("#FFFFFF", "#FFF0E0")
            4      -> gc("#FFF0E0", "#FFE0C2")
            8      -> gc("#FFE0C2", "#FFC799")
            16     -> gc("#FFC799", "#FFA866")
            32     -> gc("#FFA866", "#FF8833")
            64     -> gc("#FF8833", "#FF6600")
            128    -> gc("#FF6600", "#D64F00")
            256    -> gc("#D64F00", "#A83B00")
            512    -> gc("#A83B00", "#7A2800")
            1024   -> gc("#7A2800", "#4D1600")
            2048   -> gc("#4D1600", "#210800")
            4096   -> gc("#210800", "#000000")
            8192   -> gc("#000000", "#000000")


            -2     -> gc("#FFFFFF", "#EBF5FF")
            -4     -> gc("#EBF5FF", "#CCE8FF")
            -8     -> gc("#CCE8FF", "#99D1FF")
            -16    -> gc("#99D1FF", "#66B8FF")
            -32    -> gc("#66B8FF", "#33A0FF")
            -64    -> gc("#33A0FF", "#0088FF")
            -128   -> gc("#0088FF", "#0073C2")
            -256   -> gc("#0073C2", "#005691")
            -512   -> gc("#005691", "#003C66")
            -1024  -> gc("#003C66", "#002642")
            -2048  -> gc("#002642", "#001321")
            -4096  -> gc("#001321", "#000000")
            -8192  -> gc("#000000", "#000000")

            else   -> if (value > 0) gc("#220000", "#050000") else gc("#060008", "#010002")
        }
    }

    private fun gc(hex1: String, hex2: String, alpha: Float = 1f): Pair<Int, Int> {
        val a = (255 * alpha).toInt()
        fun parse(hex: String): Int {
            val c = Color.parseColor(hex)
            return if (alpha < 1f) Color.argb(a, Color.red(c), Color.green(c), Color.blue(c)) else c
        }
        return parse(hex1) to parse(hex2)
    }

    private fun getTileTextColor(value: Int): Int = if (isDark) {
        when {
            value == 0           -> Color.TRANSPARENT
            value in 2..64       -> Color.parseColor("#FFF0E0")
            value in 128..8192       -> Color.parseColor("#210800")

            value > 0            -> Color.parseColor("#FFFFFF")

            value in -64..-2       -> Color.parseColor("#FFF0E0")
            value in -8192..-128       -> Color.parseColor("#210800")
            else                 -> Color.parseColor("#FFFFFF")
        }
    } else {
        when {
            value == 0           -> Color.TRANSPARENT
            value in 2..64        -> Color.parseColor("#210800")
            value in 128..8192       -> Color.parseColor("#FFF0E0")
            value > 0            -> Color.parseColor("#FFFFFF")


            value in -64..-2        -> Color.parseColor("#210800")
            value in -8192..-128       -> Color.parseColor("#FFF0E0")

            else                 -> Color.parseColor("#FFFFFF")
        }
    }


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