package com.vivacomigo.app.data.api

import com.google.gson.annotations.SerializedName

// ============== AUTH ==============

data class RegisterRequest(
    @SerializedName("display_name")
    val displayName: String
)

data class LoginRequest(
    @SerializedName("display_name")
    val displayName: String
)

data class LoginCodeRequest(
    @SerializedName("pairing_code")
    val pairingCode: String
)

data class AuthResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("user")
    val user: ApiUser
)

// ============== USER ==============

data class ApiUser(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("pairing_code")
    val pairingCode: String,
    @SerializedName("partner_id")
    val partnerId: String?,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

data class PairRequest(
    @SerializedName("partner_code")
    val partnerCode: String
)

data class FcmTokenRequest(
    @SerializedName("fcmToken")
    val fcmToken: String
)

// ============== PHOTO ==============

data class ApiPhoto(
    @SerializedName("id")
    val id: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("receiver_id")
    val receiverId: String,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("seen")
    val seen: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class PhotoUploadResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("sender_id")
    val senderId: String,
    @SerializedName("receiver_id")
    val receiverId: String,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("seen")
    val seen: Boolean,
    @SerializedName("created_at")
    val createdAt: String?
)

// ============== GENERIC ==============

data class ErrorResponse(
    @SerializedName("error")
    val error: String
)

data class HealthResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("message")
    val message: String
)

data class MessageResponse(
    @SerializedName("message")
    val message: String
)
