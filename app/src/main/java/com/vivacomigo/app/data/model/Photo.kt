package com.vivacomigo.app.data.model

data class Photo(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val seen: Boolean = false
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "senderId" to senderId,
        "receiverId" to receiverId,
        "imageUrl" to imageUrl,
        "timestamp" to timestamp,
        "seen" to seen
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Photo = Photo(
            id = map["id"] as? String ?: "",
            senderId = map["senderId"] as? String ?: "",
            receiverId = map["receiverId"] as? String ?: "",
            imageUrl = map["imageUrl"] as? String ?: "",
            timestamp = map["timestamp"] as? Long ?: 0L,
            seen = map["seen"] as? Boolean ?: false
        )
    }
}
