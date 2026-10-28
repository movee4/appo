package com.example.model

enum class RuleType {
    EXACT_BLACKLIST,
    REGEX_BLACKLIST,
    EXACT_WHITELIST,
    REGEX_WHITELIST
}

data class DomainRule(
    val id: String,
    val domain: String,
    val type: RuleType,
    val isEnabled: Boolean = true,
    val comment: String = "",
    val dateAdded: String = "2026-10-28"
)

data class BlocklistSource(
    val id: String,
    val url: String,
    val enabled: Boolean = true,
    val comment: String = "",
    val domainCount: Long = 0,
    val dateAdded: String = "",
    val lastUpdateStatus: String = "SUCCESS"
)

sealed interface GravityUpdateState {
    data object Idle : GravityUpdateState
    data class InProgress(val message: String = "جاري تنزيل وتحديث قوائم الجاذبية (Updating Gravity)...") : GravityUpdateState
    data class Completed(val success: Boolean, val message: String, val domainsCount: Long = 0) : GravityUpdateState
}
