package com.sadegh4j.signed2048.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.sadegh4j.signed2048.R
import com.sadegh4j.signed2048.model.WinTarget
import androidx.core.net.toUri

class MenuBottomSheet : BottomSheetDialogFragment() {

    var onThemeModeChange: ((Int) -> Unit)? = null
    var onWinTargetChange: ((WinTarget) -> Unit)? = null

    private var themeMode: Int = AppCompatDelegate.MODE_NIGHT_YES
    private var currentWinTarget: WinTarget = WinTarget.T2048

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_menu, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ─── سوئیچ تم ───
        val switchTheme = view.findViewById<MaterialSwitch>(R.id.switchTheme)
        switchTheme.isChecked = (themeMode == AppCompatDelegate.MODE_NIGHT_YES)
        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
            onThemeModeChange?.invoke(mode)
            dismiss()
        }

        // ─── هدف بازی ───
        val radioGroup = view.findViewById<RadioGroup>(R.id.rgWinTarget)
        radioGroup.check(
            when (currentWinTarget) {
                WinTarget.T1024  -> R.id.rb1024
                WinTarget.T2048  -> R.id.rb2048
                WinTarget.T4096 -> R.id.rb4096
            }
        )
        radioGroup.setOnCheckedChangeListener { _, id ->
            val target = when (id) {
                R.id.rb1024  -> WinTarget.T1024
                R.id.rb4096 -> WinTarget.T4096
                else          -> WinTarget.T2048
            }
            onWinTargetChange?.invoke(target)
        }

        // ─── امتیاز به برنامه ───
        view.findViewById<LinearLayout>(R.id.llRating).setOnClickListener {


            try {
                val packageName = requireContext().packageName
                val uri = "bazaar://details?id=$packageName".toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                try {
                    val packageName = requireContext().packageName
                    val fallbackUri = "https://cafebazaar.ir/app/$packageName".toUri()
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                    startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    Toast.makeText(requireContext(), "لطفاً کافه بازار یا یک مرورگر نصب کنید", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "خطا در باز کردن کافه بازار", Toast.LENGTH_SHORT).show()
            }



            /*try {
                // در Fragment از requireContext() یا requireActivity() استفاده می‌کنیم
                val packageName = requireContext().packageName
                val uri = "myket://comment?id=$packageName".toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                try {
                    val packageName = requireContext().packageName
                    val fallbackUri = "https://myket.ir/app/$packageName".toUri()
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                    startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    Toast.makeText(requireContext(), "لطفاً مایکت یا یک مرورگر نصب کنید", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "خطا در باز کردن مایکت", Toast.LENGTH_SHORT).show()
            }*/


        }

        // ─── اشتراک‌گذاری ───
        view.findViewById<LinearLayout>(R.id.llScore).setOnClickListener {
            val score = arguments?.getInt(ARG_SCORE, 0) ?: 0
            val best  = arguments?.getInt(ARG_BEST, 0)  ?: 0
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
                putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.share_text, score, best)
                )
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_chooser)))
            dismiss()
        }

        // ─── ارتباط ───
        view.findViewById<LinearLayout>(R.id.llContact).setOnClickListener {
            ContactDialog().show(parentFragmentManager, "contact")
            dismiss()
        }

        // ─── درباره ───
        view.findViewById<LinearLayout>(R.id.llAbout).setOnClickListener {
            AboutDialog().show(parentFragmentManager, "about")
            dismiss()
        }
    }

    companion object {
        private const val ARG_SCORE      = "score"
        private const val ARG_BEST       = "best"
        private const val ARG_THEME_MODE = "theme_mode"
        private const val ARG_TARGET     = "win_target"

        fun newInstance(
            score: Int,
            best: Int,
            themeMode: Int,
            target: WinTarget
        ) = MenuBottomSheet().apply {
            this.themeMode        = themeMode
            this.currentWinTarget = target
            arguments = Bundle().apply {
                putInt(ARG_SCORE,      score)
                putInt(ARG_BEST,       best)
                putInt(ARG_THEME_MODE, themeMode)
                putString(ARG_TARGET,  target.name)
            }
        }
    }
}
