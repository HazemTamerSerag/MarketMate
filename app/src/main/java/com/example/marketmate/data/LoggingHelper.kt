package com.example.marketmate.data
import android.util.Log

object LoggingHelper {
    fun logStart(tag: String = "AppStart", message: String = "التطبيق بدأ التشغيل ✅") {
        Log.d(tag, message)
    }

    fun logCameraScreenOpened() {
        Log.d("CameraScreen", "تم فتح شاشة الكاميرا 📷")
    }

    fun logInternetStatus(isConnected: Boolean) {
        Log.d("PredictionMode", "الاتصال بالإنترنت: $isConnected")
    }

    fun logPhotoTaken() {
        Log.d("CameraScreen", "تم التقاط صورة 📸")
    }

    fun logPredictionMode(useApi: Boolean) {
        if (useApi) {
            Log.d("PredictionFlow", "جاري إرسال الصورة إلى السيرفر 🌐")
        } else {
            Log.d("PredictionFlow", "هيتم تحليل الصورة محليًا باستخدام TFLite 🧠")
        }
    }

    fun logApiCallStart() {
        Log.d("API_CALL", "رفع الصورة إلى السيرفر...")
    }

    fun logApiResponse(label: String, audioUrl: String) {
        Log.d("API_CALL", "استقبلنا الرد من السيرفر ✅")
        Log.d("API_RESPONSE", "التصنيف: $label - الملف الصوتي: $audioUrl")
    }

    fun logApiError(message: String?) {
        Log.e("API_ERROR", "فشل الاتصال بالسيرفر: $message")
    }

    fun logTFLiteStart() {
        Log.d("TFLite", "بدأ تصنيف الصورة باستخدام TFLite")
    }

    fun logTFLiteResult(label: String) {
        Log.d("TFLite", "التصنيف المحلي: $label")
    }

    fun logAudioPlaying() {
        Log.d("AUDIO", "تشغيل الملف الصوتي 🔊")
    }
}