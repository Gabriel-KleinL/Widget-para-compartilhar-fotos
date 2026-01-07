package com.vivacomigo.app.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val pairing_code: String = "",
    val partner_id: String? = null,
    val display_name: String = ""
) {
    // Propriedades computadas para compatibilidade com código existente
    val pairingCode: String
        get() = pairing_code
    
    val partnerId: String?
        get() = partner_id
    
    val displayName: String
        get() = display_name
}
