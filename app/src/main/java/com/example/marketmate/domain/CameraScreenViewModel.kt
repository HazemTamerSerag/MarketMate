package com.example.marketmate.domain

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class CameraScreenViewModel : ViewModel() {
    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()

    private val _isSheetVisible = MutableStateFlow(false)
    val isSheetVisible: StateFlow<Boolean> = _isSheetVisible

    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    fun onTakePhoto(bitmap: Bitmap) {
        _bitmaps.value += bitmap
    }

    fun showSheet() {
        _isSheetVisible.value = true
    }

    fun hideSheet() {
        _isSheetVisible.value = false
    }

    fun setLanguage(language: String, activity: Activity) {
        _selectedLanguage.value = language
        saveLanguageToPreferences(activity, language)
        updateLocale(activity, language)
        hideSheet()
        activity.recreate()
    }

    private fun saveLanguageToPreferences(context: Context, language: String) {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("language", language).apply()
    }

    private fun updateLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
