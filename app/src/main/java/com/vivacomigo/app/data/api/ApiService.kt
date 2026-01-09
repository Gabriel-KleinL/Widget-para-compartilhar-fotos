package com.vivacomigo.app.data.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ============== HEALTH ==============

    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    // ============== AUTHENTICATION ==============

    @POST("api/auth/register-simple")
    suspend fun registerSimple(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login-simple")
    suspend fun loginSimple(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/login-code")
    suspend fun loginWithCode(@Body request: LoginCodeRequest): Response<AuthResponse>

    // ============== USERS ==============

    @GET("api/users/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<ApiUser>

    @GET("api/users/{id}")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<ApiUser>

    @GET("api/users/pairing-code/{code}")
    suspend fun getUserByPairingCode(
        @Header("Authorization") token: String,
        @Path("code") pairingCode: String
    ): Response<ApiUser>

    @POST("api/users/pair")
    suspend fun pairWithPartner(
        @Header("Authorization") token: String,
        @Body request: PairRequest
    ): Response<ApiUser>

    @DELETE("api/users/unpair")
    suspend fun unpairPartner(@Header("Authorization") token: String): Response<ApiUser>

    @PUT("api/users/fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body request: FcmTokenRequest
    ): Response<MessageResponse>

    // ============== PHOTOS ==============

    @Multipart
    @POST("api/photos")
    suspend fun uploadPhoto(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("receiver_id") receiverId: String
    ): Response<PhotoUploadResponse>

    @GET("api/photos/latest")
    suspend fun getLatestPhoto(@Header("Authorization") token: String): Response<ApiPhoto>

    @GET("api/photos")
    suspend fun getPhotos(@Header("Authorization") token: String): Response<List<ApiPhoto>>

    @GET("api/photos/{id}/image")
    suspend fun getPhotoImage(
        @Header("Authorization") token: String,
        @Path("id") photoId: String
    ): Response<ResponseBody>
}
