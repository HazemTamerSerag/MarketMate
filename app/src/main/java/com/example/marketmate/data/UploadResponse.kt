package com.example.marketmate.data

data class UploadResponse(
    val audio_file: String,
    val image_ID: String,
    val prediction: String
)