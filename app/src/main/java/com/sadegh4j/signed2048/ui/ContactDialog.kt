package com.sadegh4j.signed2048.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.sadegh4j.signed2048.R
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.google.android.material.card.MaterialCardView

class ContactDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_contact, null)

        // جیمیل
        view.findViewById<MaterialCardView>(R.id.llGmail).setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = "mailto:sadeghjfr22@gmail.com".toUri()
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.contact_subject))
                }
                startActivity(intent)
            } catch (e: Exception) {
                // fallback — مستقیم Gmail باز می‌کنه
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data =
                            "https://mail.google.com/mail/?view=cm&to=sadeghjfr22@gmail.com".toUri()
                    }
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.no_email_app), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // تلگرام
        view.findViewById<MaterialCardView>(R.id.llTelegram).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://t.me/sadegh4j".toUri())
            startActivity(intent)
        }

        // بستن
        view.findViewById<MaterialButton>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        return Dialog(requireContext()).apply {
            setContentView(view)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }
}