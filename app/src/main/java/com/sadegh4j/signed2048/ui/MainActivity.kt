package com.sadegh4j.signed2048.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sadegh4j.signed2048.R
import com.sadegh4j.signed2048.data.PreferencesManager
import com.sadegh4j.signed2048.model.GameState
import com.sadegh4j.signed2048.viewmodel.GameViewModel
import com.sadegh4j.signed2048.viewmodel.GameViewModelFactory
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: GameViewModel
    private lateinit var gameView: GameView
    private lateinit var titleScore: TextView
    private lateinit var titleRecord: TextView
    private lateinit var txtScore: TextView
    private lateinit var txtBest: TextView
    private lateinit var tvTitle: TextView
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnUndo: MaterialButton
    private lateinit var btnMenu: MaterialButton

    private var gameOverShowing = false
    private var gameWonShowing = false

    // ─── Lifecycle ────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // edge-to-edge — استاندارد API 35+
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        // inset handling — محتوا زیر status bar نره
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootView)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setupViewModel()
        bindViews()
        setupBackHandler()
        setupListeners()
        observeState()
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveOnExit()
    }

    // ─── Setup ────────────────────────────────────────────────────

    private fun setupViewModel() {
        val factory = GameViewModelFactory(PreferencesManager(this))
        viewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]
    }

    private fun bindViews() {
        gameView = findViewById(R.id.gameView)
        titleScore = findViewById(R.id.titleScore)
        titleRecord = findViewById(R.id.titleRecord)
        txtScore = findViewById(R.id.txtScore)
        txtBest = findViewById(R.id.txtBest)
        tvTitle = findViewById(R.id.tvTitle)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnUndo = findViewById(R.id.btnUndo)
        btnMenu = findViewById(R.id.btnMenu)
        applyFont()

    }

    private fun applyFont() {
        val typeface =
            resources.getFont(R.font.black_ops)
        titleScore.typeface  = typeface
        titleRecord.typeface  = typeface
        tvTitle.typeface  = typeface
        txtScore.typeface = typeface
        txtBest.typeface  = typeface
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.saveOnExit()
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("خروج")
                    .setMessage("از بازی خارج می‌شوید؟")
                    .setPositiveButton("خروج") { _, _ -> finishAffinity() }
                    .setNegativeButton("ادامه", null)
                    .show()
            }
        })
    }

    private fun setupListeners() {
        btnRefresh.setOnClickListener { confirmReset() }
        btnUndo.setOnClickListener { viewModel.undo() }
        btnMenu.setOnClickListener { showMenu() }

        gameView.onSwipe = { direction ->
            viewModel.onSwipe(direction)
        }
    }

    // ─── Observe ─────────────────────────────────────────────────

    private fun observeState() {
        viewModel.state.observe(this, ::renderState)

        viewModel.canUndo.observe(this) { can ->
            btnUndo.alpha = if (can) 1f else 0.35f
            btnUndo.isEnabled = can
        }
    }

    private fun renderState(state: GameState) {
        val isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        gameView.updateCells(state.cells, isDark)

        val formatter = DecimalFormat("#,###")

        txtBest.text  = formatBigNumber(state.best.toLong())
        txtScore.text = formatBigNumber(state.score.toLong())


        if (state.isGameOver && !gameOverShowing) {
            gameOverShowing = true
            showGameOverDialog()
        }
        if (state.isGameWon && !gameWonShowing) {
            gameWonShowing = true
            showGameWonDialog()
        }
        if (!state.isGameOver) gameOverShowing = false
        // فقط موقعی ریست میشه که بازی جدید شروع شده باشه
        if (!state.isGameWon && state.cells.all { row -> row.all { it == 0 || it == 2 || it == 4 } })
            gameWonShowing = false
    }


    fun formatBigNumber(number: Long): String {
        if (number < 1000) return number.toString()

        val suffixes = arrayOf("", "K", "M", "B", "T")
        val exp = (Math.log10(number.toDouble()) / 3).toInt()

        // محاسبه عدد خلاصه شده با یک رقم اعشار
        val shortened = number / Math.pow(10.0, (exp * 3).toDouble())

        // اگر رقم اعشار صفر بود (مثلا 145.0)، اعشار را حذف میکند تا بشود 145M
        return if (shortened % 1 == 0.0) {
            String.format("%.0f%s", shortened, suffixes[exp])
        } else {
            String.format("%.1f%s", shortened, suffixes[exp])
        }
    }

    // ─── Dialogs ─────────────────────────────────────────────────

    private fun confirmReset() {
        MaterialAlertDialogBuilder(this)
            .setTitle("بازی جدید")
            .setMessage("بازی فعلی پاک می‌شود. ادامه می‌دهید؟")
            .setPositiveButton("بله") { _, _ -> viewModel.resetGame() }
            .setNegativeButton("خیر", null)
            .show()
    }

    private fun showGameOverDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("بازی تموم شد!")
            .setMessage("هیچ حرکتی ممکن نیست.\nامتیاز: ${viewModel.currentScore()}")
            .setPositiveButton("بازی مجدد") { _, _ -> viewModel.resetGame() }
            .setCancelable(false)
            .show()
    }

    private fun showGameWonDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("🎉 بردی!")
            .setMessage("به ${viewModel.currentWinTarget().label} رسیدی!\nامتیاز: ${viewModel.currentScore()}")
            .setPositiveButton("بازی مجدد") { _, _ -> viewModel.resetGame() }
            .setCancelable(false)
            .show()
    }

    private fun showMenu() {
        val state = viewModel.state.value ?: return
        MenuBottomSheet.newInstance(
            score = state.score,
            best = state.best,
            themeMode = viewModel.currentThemeMode(),
            target = state.winTarget
        ).apply {
            onThemeModeChange = { viewModel.setThemeMode(it) }
            onWinTargetChange = { viewModel.setWinTarget(it) }
        }.show(supportFragmentManager, "menu")
    }
}