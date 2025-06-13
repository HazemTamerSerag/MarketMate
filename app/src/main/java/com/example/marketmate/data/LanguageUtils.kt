package com.example.marketmate.data

import android.content.Context
import java.util.Locale

object LanguageUtils {
    fun applySavedLanguage(context: Context) {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val language = sharedPreferences.getString("language", "en") ?: "en"
        updateLocale(context, language)
    }

    fun updateLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val configuration = context.resources.configuration
        configuration.setLocale(locale)

        context.resources.updateConfiguration(
            configuration,
            context.resources.displayMetrics
        )
    }

    fun saveLanguage(context: Context, language: String) {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("language", language).apply()
    }
}