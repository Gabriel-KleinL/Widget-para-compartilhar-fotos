package com.vivacomigo.app.data.model

data class Photo(
    val id: String = "",
    val sender_id: String = "",
    val receiver_id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val seen: Boolean = false
) {
    // Propriedades computadas para compatibilidade com código existente
    val senderId: String
        get() = sender_id
    
    val receiverId: String
        get() = receiver_id
    
    // imageUrl não é mais necessário pois vamos trabalhar com BLOB diretamente
    val imageUrl: String
        get() = "" // Será gerenciado pelo PhotoRepository
}
