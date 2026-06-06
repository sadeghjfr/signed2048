package com.sadegh4j.signed2048.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sadegh4j.signed2048.data.PreferencesManager

class GameViewModelFactory(
    private val prefs: PreferencesManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java))
            return GameViewModel(prefs) as T
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
