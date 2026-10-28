package com.example.model

data class PiHoleConfig(
    val host: String = "192.168.1.100",
    val port: Int = 80,
    val useHttps: Boolean = false,
    val apiPassword: String = "",
    val sid: String = "",
    val nasHost: String = "192.168.1.100",
    val nasMacAddress: String = "00:11:32:8A:2B:1C",
    val nasDeviceName: String = "Synology NAS (Pi-hole Host)",
    val isDemoMode: Boolean = false,
    val autoRefreshSeconds: Int = 5
) {
    val baseUrl: String
        get() {
            val scheme = if (useHttps) "https" else "http"
            val portPart = if ((useHttps && port == 443) || (!useHttps && port == 80)) "" else ":$port"
            return "$scheme://$host$portPart"
        }

    val adminWebUrl: String
        get() = "$baseUrl/admin"

    val apiDocsUrl: String
        get() = "$baseUrl/api/docs"
}
