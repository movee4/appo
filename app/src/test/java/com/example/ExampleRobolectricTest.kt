package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BlockingStatus
import com.example.model.HistoryDataPoint
import com.example.model.NetworkClient
import com.example.model.PiHoleConfig
import com.example.model.PiHoleStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Network Commander", appName)
    }

    @Test
    fun `test pihole config url generation`() {
        val configHttp = PiHoleConfig(host = "192.168.1.100", port = 80, useHttps = false)
        assertEquals("http://192.168.1.100", configHttp.baseUrl)
        assertEquals("http://192.168.1.100/admin", configHttp.adminWebUrl)
        assertEquals("http://192.168.1.100/api/docs", configHttp.apiDocsUrl)

        val configCustomPort = PiHoleConfig(host = "192.168.1.100", port = 8080, useHttps = false)
        assertEquals("http://192.168.1.100:8080", configCustomPort.baseUrl)

        val configHttps = PiHoleConfig(host = "pihole.local", port = 443, useHttps = true)
        assertEquals("https://pihole.local", configHttps.baseUrl)
    }

    @Test
    fun `test network client display name resolution`() {
        val clientWithNick = NetworkClient(ip = "192.168.1.50", hostname = "tv-box", customNickname = "Living Room TV")
        assertEquals("Living Room TV", clientWithNick.displayName)

        val clientWithHostname = NetworkClient(ip = "192.168.1.60", hostname = "nas-server", customNickname = "")
        assertEquals("nas-server", clientWithHostname.displayName)

        val clientWithIpOnly = NetworkClient(ip = "192.168.1.70", hostname = "unknown", customNickname = "")
        assertEquals("192.168.1.70", clientWithIpOnly.displayName)
    }

    @Test
    fun `test pihole stats and 24h history model`() {
        val history = listOf(
            HistoryDataPoint(timestamp = 1000L, hourLabel = "12:00", totalQueries = 1200, blockedQueries = 300),
            HistoryDataPoint(timestamp = 2000L, hourLabel = "13:00", totalQueries = 1500, blockedQueries = 350)
        )
        val stats = PiHoleStats(
            blockingStatus = BlockingStatus.ENABLED,
            totalQueries = 28410,
            queriesBlocked = 7102,
            percentBlocked = 25.0f,
            activeClients = 14,
            history24h = history
        )
        assertEquals(BlockingStatus.ENABLED, stats.blockingStatus)
        assertEquals(28410L, stats.totalQueries)
        assertEquals(14, stats.activeClients)
        assertEquals(2, stats.history24h.size)
    }

    @Test
    fun `test blocklist source and rule types`() {
        val source = com.example.model.BlocklistSource(
            id = "1",
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
            enabled = true,
            comment = "StevenBlack Adlist",
            domainCount = 184520L
        )
        assertEquals(184520L, source.domainCount)
        assertTrue(source.enabled)

        val rule = com.example.model.DomainRule(
            id = "10",
            domain = "ads.example.com",
            type = com.example.model.RuleType.EXACT_BLACKLIST
        )
        assertEquals(com.example.model.RuleType.EXACT_BLACKLIST, rule.type)
        assertEquals("ads.example.com", rule.domain)
    }
}
