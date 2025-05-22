package com.example.marketmate.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo ?: return false
        @Suppress("DEPRECATION")
        networkInfo.isConnected
    }
}
