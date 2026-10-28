package com.example.model

data class NasStatus(
    val ip: String = "192.168.1.100",
    val mac: String = "00:11:32:8A:2B:1C",
    val name: String = "NAS Server",
    val isOnline: Boolean = true,
    val latencyMs: Long = 2,
    val openPorts: List<Int> = listOf(80, 53, 5000, 22),
    val lastCheckedTimestamp: Long = System.currentTimeMillis()
)

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val message: String,
    val latencyMs: Long = 0,
    val httpStatusCode: Int = 0,
    val detectedApiVersion: String = "Unknown",
    val sid: String = "",
    val details: String = ""
)
