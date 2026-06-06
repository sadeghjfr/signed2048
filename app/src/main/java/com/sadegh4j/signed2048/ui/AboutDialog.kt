package com.sadegh4j.signed2048.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.sadegh4j.signed2048.R
import androidx.core.graphics.drawable.toDrawable

class AboutDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_about, null)
        return Dialog(requireContext()).apply {
            setContentView(view)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            view.findViewById<android.widget.TextView>(R.id.btnOk).setOnClickListener {
                dismiss()
            }
        }
    }


}
