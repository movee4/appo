package com.example.model

enum class BlockingStatus {
    ENABLED,
    DISABLED,
    PAUSED
}

data class HistoryDataPoint(
    val timestamp: Long,
    val hourLabel: String,
    val totalQueries: Int,
    val blockedQueries: Int
)

data class PiHoleStats(
    val blockingStatus: BlockingStatus = BlockingStatus.ENABLED,
    val pauseRemainingSeconds: Int = 0,
    val totalQueries: Long = 0,
    val queriesBlocked: Long = 0,
    val percentBlocked: Float = 0f,
    val domainsOnBlocklist: Long = 0,
    val uniqueDomains: Long = 0,
    val queriesForwarded: Long = 0,
    val queriesCached: Long = 0,
    val activeClients: Int = 0,
    val totalClients: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val latencyMs: Long = 0,
    val piHoleVersion: String = "v6.0 / FTL v6.0",
    val gravityLastUpdate: String = "Today, 03:00 AM",
    val queryTypeDistribution: Map<String, Int> = emptyMap(),
    val upstreamServers: List<UpstreamServerStat> = emptyList(),
    val history24h: List<HistoryDataPoint> = emptyList()
)

data class UpstreamServerStat(
    val name: String,
    val ip: String,
    val percentage: Float,
    val queryCount: Long
)
