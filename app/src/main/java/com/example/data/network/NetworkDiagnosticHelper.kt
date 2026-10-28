package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkDiagnosticHelper {

    suspend fun pingHost(host: String, port: Int = 80, timeoutMs: Int = 2000): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val latency = System.currentTimeMillis() - start
                Pair(true, latency.coerceAtLeast(1L))
            }
        } catch (e: Exception) {
            // Fallback to InetAddress isReachable
            try {
                val reachable = InetAddress.getByName(host).isReachable(timeoutMs)
                val latency = System.currentTimeMillis() - start
                Pair(reachable, if (reachable) latency.coerceAtLeast(1L) else -1L)
            } catch (e2: Exception) {
                Pair(false, -1L)
            }
        }
    }

    suspend fun probePort(host: String, port: Int, timeoutMs: Int = 1000): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendWakeOnLan(macAddressStr: String, broadcastIp: String = "255.255.255.255"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddressStr.replace(":", "").replace("-", "").replace(".", "")
            if (cleanMac.length != 12) {
                return@withContext Result.failure(IllegalArgumentException("Invalid MAC address length (expected 12 hex chars)"))
            }

            val macBytes = ByteArray(6)
            for (i in 0 until 6) {
                macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }

            // Magic Packet structure: 6 bytes 0xFF, followed by 16 repetitions of 6-byte target MAC
            val bytes = ByteArray(6 + 16 * macBytes.size)
            for (i in 0 until 6) {
                bytes[i] = 0xff.toByte()
            }
            var step = 6
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, bytes, step, macBytes.size)
                step += macBytes.size
            }

            val address = InetAddress.getByName(broadcastIp)
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val packet9 = DatagramPacket(bytes, bytes.size, address, 9)
                socket.send(packet9)
                val packet7 = DatagramPacket(bytes, bytes.size, address, 7)
                socket.send(packet7)
            }

            Result.success("Wake-on-LAN magic packet sent to $macAddressStr")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
