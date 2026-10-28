package com.example.model

data class NetworkClient(
    val ip: String,
    val mac: String = "",
    val hostname: String = "",
    val customNickname: String = "",
    val totalQueries: Long = 0,
    val blockedQueries: Long = 0,
    val percentBlocked: Float = 0f,
    val isOnline: Boolean = true,
    val vendor: String = "Generic Device",
    val lastSeenFormatted: String = "Just now"
) {
    val displayName: String
        get() = when {
            customNickname.isNotBlank() -> customNickname
            hostname.isNotBlank() && hostname != "unknown" -> hostname
            else -> ip
        }
}
