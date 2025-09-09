package unit.utils

import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import utils.NetworkUtils
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.*

/**
 * Unit tests for [NetworkUtils].
 *
 * Verifies:
 * - Resolving local/site-local addresses
 * - Determining the active network interface
 * - Obtaining a free TCP port with fallback on error
 */
class NetworkUtilsTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun <T> enumerationOf(vararg items: T): Enumeration<T> = object : Enumeration<T> {
        private var index = 0
        override fun hasMoreElements(): Boolean = index < items.size
        override fun nextElement(): T = items[index++]
    }

    @Test
    fun `getLocalAddress returns site-local ip when available`() {
        val original = NetworkUtils.networkInfoProvider
        try {
            val inet = mockk<InetAddress>()
            every { inet.isSiteLocalAddress } returns true
            every { inet.hostAddress } returns "10.0.0.2"

            NetworkUtils.networkInfoProvider = {
                listOf(
                    NetworkUtils.NetworkInfo(
                        isLoopback = false,
                        isUp = true,
                        isVirtual = false,
                        displayName = "en0",
                        addresses = listOf(inet)
                    )
                )
            }

            val ip = NetworkUtils.getLocalAddress()
            assertEquals("10.0.0.2", ip)
        } finally {
            NetworkUtils.networkInfoProvider = original
        }
    }

    @Test
    fun `getLocalAddress returns null when no site-local address found`() {
        val original = NetworkUtils.networkInfoProvider
        try {
            val inet = mockk<InetAddress>()
            every { inet.isSiteLocalAddress } returns false
            every { inet.hostAddress } returns "203.0.113.5"

            NetworkUtils.networkInfoProvider = {
                listOf(
                    NetworkUtils.NetworkInfo(
                        isLoopback = false,
                        isUp = true,
                        isVirtual = false,
                        displayName = "en0",
                        addresses = listOf(inet)
                    )
                )
            }

            val ip = NetworkUtils.getLocalAddress()
            assertNull(ip)
        } finally {
            NetworkUtils.networkInfoProvider = original
        }
    }

    @Test
    fun `getActiveNetworkInterface returns Wi-Fi when active en interface exists`() {
        val original = NetworkUtils.networkInfoProvider
        try {
            NetworkUtils.networkInfoProvider = {
                listOf(
                    NetworkUtils.NetworkInfo(
                        isLoopback = false,
                        isUp = true,
                        isVirtual = false,
                        displayName = "en0",
                        addresses = emptyList()
                    )
                )
            }

            val name = NetworkUtils.getActiveNetworkInterface()
            assertEquals("Wi-Fi", name)
        } finally {
            NetworkUtils.networkInfoProvider = original
        }
    }

    @Test
    fun `getActiveNetworkInterface throws when not found`() {
        val original = NetworkUtils.networkInfoProvider
        try {
            NetworkUtils.networkInfoProvider = { emptyList() }
            assertThrows(IOException::class.java) {
                NetworkUtils.getActiveNetworkInterface()
            }
        } finally {
            NetworkUtils.networkInfoProvider = original
        }
    }

    @Test
    fun `getFreePort returns system-assigned port when available`() {
        val original = NetworkUtils.serverSocketFactory
        try {
            val mockSocket = mockk<ServerSocket>(relaxed = true)
            every { mockSocket.localPort } returns 55555
            NetworkUtils.serverSocketFactory = { _: Int -> mockSocket }
            val port = NetworkUtils.getFreePort()
            assertEquals(55555, port)
        } finally {
            NetworkUtils.serverSocketFactory = original
        }
    }

    @Test
    fun `getFreePort returns default on exception`() {
        val original = NetworkUtils.serverSocketFactory
        try {
            NetworkUtils.serverSocketFactory = { _: Int -> throw IOException("boom") }
            val port = NetworkUtils.getFreePort(defaultPort = 8123)
            assertEquals(8123, port)
        } finally {
            NetworkUtils.serverSocketFactory = original
        }
    }
}
