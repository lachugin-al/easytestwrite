package utils

import controller.mobile.interaction.ScrollDirection
import java.io.IOException
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.SocketException
import java.net.InetAddress
import java.util.Enumeration

/**
 * Utility object for working with network interfaces and ports.
 *
 * Provides functions for obtaining the local IP address of the machine,
 * determining the active network interface, and finding a free TCP port.
 */
object NetworkUtils {

    // Test providers (injected in unit tests)
    internal data class NetworkInfo(
        val isLoopback: Boolean,
        val isUp: Boolean,
        val isVirtual: Boolean,
        val displayName: String,
        val addresses: List<InetAddress>
    )

    internal var networkInfoProvider: (() -> List<NetworkInfo>)? = null

    // Base providers (used for building NetworkInfo in production)
    internal var networkInterfacesProvider: () -> Enumeration<NetworkInterface> = {
        NetworkInterface.getNetworkInterfaces()
    }

    internal var inetAddressesProvider: (NetworkInterface) -> List<InetAddress> = { iface ->
        iface.interfaceAddresses.mapNotNull { it?.address }
    }

    /**
     * Gets the local IP address of the machine.
     *
     * Iterates over all network interfaces and selects the first available site-local IP address
     * (usually an address from the range 192.168.x.x or 10.x.x.x).
     *
     * @throws SocketException In case of errors working with network interfaces.
     * @return A string with the local IP address or `null` if none could be found.
     */
    @Throws(SocketException::class)
    fun getLocalAddress(): String? {
        networkInfoProvider?.let { provider ->
            for (ni in provider.invoke()) {
                if (!ni.isLoopback && ni.isUp && !ni.isVirtual) {
                    for (inetAddress in ni.addresses) {
                        if (inetAddress.isSiteLocalAddress) {
                            return inetAddress.hostAddress
                        }
                    }
                }
            }
            return null
        }

        val interfaces = networkInterfacesProvider()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            if (!iface.isLoopback && iface.isUp && !iface.isVirtual) {
                val addresses = inetAddressesProvider(iface)
                for (inetAddress in addresses) {
                    if (inetAddress.isSiteLocalAddress) {
                        return inetAddress.hostAddress
                    }
                }
            }
        }

        // Return null if no suitable interface is found
        return null
    }

    /**
     * Determines the active physical network interface.
     *
     * Looks for interfaces whose names start with "en" (Ethernet) or "utun" (VPN),
     * and which are in an active state.
     *
     * @throws IOException If no suitable active network interface is found.
     * @return The name of the network interface (e.g., "Wi-Fi").
     */
    @Throws(SocketException::class)
    fun getActiveNetworkInterface(): String {
        networkInfoProvider?.let { provider ->
            for (ni in provider.invoke()) {
                if (!ni.isLoopback && ni.isUp && !ni.isVirtual &&
                    (ni.displayName.startsWith("en") || ni.displayName.startsWith("utun"))) {
                    return "Wi-Fi"
                }
            }
            throw IOException("Active physical network interface not found")
        }

        val interfaces: Enumeration<NetworkInterface> = networkInterfacesProvider()

        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()

            // Filter interfaces whose name starts with "en" (e.g., en0, en1) and which are not loopback or virtual
            if (!iface.isLoopback && iface.isUp && !iface.isVirtual &&
                (iface.displayName.startsWith("en") || iface.displayName.startsWith("utun"))) {

                // If the interface starts with "en" or "utun", return "Wi-Fi"
                return "Wi-Fi"
            }
        }

        throw IOException("Active physical network interface not found")
    }

    /**
     * Finds a free local TCP port.
     *
     * Uses the ServerSocket mechanism with port 0 for automatic system selection of an available port.
     *
     * @param defaultPort The default port that will be returned if port lookup fails.
     * @return A free port or the default port if an error occurs.
     */
    internal var serverSocketFactory: (Int) -> ServerSocket = { p -> ServerSocket(p) }

    fun getFreePort(defaultPort: Int = 8000): Int {
        return try {
            serverSocketFactory(0).use { socket ->
                socket.localPort
            }
        } catch (e: IOException) {
            e.printStackTrace()
            defaultPort
        }
    }
}
