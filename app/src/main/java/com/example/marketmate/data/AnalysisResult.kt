package com.example.marketmate.data

// Enum for different analysis results
enum class AnalysisResultType {
    SUITABLE,
    NOT_SUITABLE,
    ERROR
}

// Data class for analysis result
data class AnalysisResult(
    val type: AnalysisResultType,
    val message: String,
    val confidence: Float? = null
)