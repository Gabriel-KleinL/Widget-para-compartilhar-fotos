package com.vivacomigo.app.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val pairingCode: String = "",
    val partnerId: String? = null,
    val displayName: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "email" to email,
        "pairingCode" to pairingCode,
        "partnerId" to partnerId,
        "displayName" to displayName
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): User = User(
            id = map["id"] as? String ?: "",
            email = map["email"] as? String ?: "",
            pairingCode = map["pairingCode"] as? String ?: "",
            partnerId = map["partnerId"] as? String,
            displayName = map["displayName"] as? String ?: ""
        )
    }
}
