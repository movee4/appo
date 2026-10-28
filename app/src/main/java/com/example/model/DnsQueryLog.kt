package com.example.model

enum class QueryStatus {
    BLOCKED_GRAVITY,
    BLOCKED_BLACKLIST,
    BLOCKED_REGEX,
    FORWARDED,
    CACHED,
    UNKNOWN
}

data class DnsQueryLog(
    val id: String,
    val timestamp: Long,
    val formattedTime: String,
    val formattedFullDate: String = "",
    val domain: String,
    val clientIp: String,
    val clientName: String,
    val queryType: String, // A, AAAA, PTR, HTTPS, TXT, SRV
    val status: QueryStatus,
    val statusText: String,
    val reply: String,
    val responseTimeMs: Double = 1.2,
    val upstreamServer: String = "Cloudflare (1.1.1.1)",
    val dnssecStatus: String = "SECURE",
    val blockReason: String = ""
)
