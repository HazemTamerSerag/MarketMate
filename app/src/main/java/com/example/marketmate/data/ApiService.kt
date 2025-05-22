package com.example.marketmate.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("api/upload-image")
    suspend fun uploadImage(
        @Part("device_ID") deviceId: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<UploadResponse>
}
interface FeedbackApiService {
    @Multipart
    @POST("api/submit-feedback")
    suspend fun submitFeedback(
        @Part("device_ID") deviceId: RequestBody,
        @Part voiceMessage: MultipartBody.Part
    ): Response<FeedbackResponse>
}