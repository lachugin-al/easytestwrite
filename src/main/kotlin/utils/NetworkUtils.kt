package utils

import java.io.IOException
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.SocketException
import java.net.InetAddress
import java.util.Enumeration

/**
 * Утилитарный объект для работы с сетевыми интерфейсами и портами.
 *
 * Предоставляет функции для получения локального IP-адреса устройства,
 * определения активного сетевого интерфейса и поиска свободного TCP-порта.
 */
object NetworkUtils {

    // Тестовые провайдеры (инжектируются в unit-тестах)
    internal data class NetworkInfo(
        val isLoopback: Boolean,
        val isUp: Boolean,
        val isVirtual: Boolean,
        val displayName: String,
        val addresses: List<InetAddress>
    )

    internal var networkInfoProvider: (() -> List<NetworkInfo>)? = null

    // Базовые провайдеры (используются для сборки NetworkInfo в проде)
    internal var networkInterfacesProvider: () -> Enumeration<NetworkInterface> = {
        NetworkInterface.getNetworkInterfaces()
    }

    internal var inetAddressesProvider: (NetworkInterface) -> List<InetAddress> = { iface ->
        iface.interfaceAddresses.mapNotNull { it?.address }
    }

    /**
     * Получает локальный IP-адрес машины.
     *
     * Перебирает все сетевые интерфейсы и выбирает первый доступный site-local IP-адрес
     * (обычно это адрес из диапазона 192.168.x.x или 10.x.x.x).
     *
     * @throws SocketException В случае ошибок работы с сетевыми интерфейсами.
     * @return Строка с локальным IP-адресом или `null`, если не удалось найти.
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

        // Возвращаем null, если нет подходящего интерфейса
        return null
    }

    /**
     * Определяет активный физический сетевой интерфейс.
     *
     * Ищет интерфейсы с именами, начинающимися на "en" (Ethernet) или "utun" (VPN),
     * и которые находятся в активном состоянии.
     *
     * @throws IOException Если не найден ни один подходящий сетевой интерфейс.
     * @return Название сетевого интерфейса (например, "Wi-Fi").
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
            throw IOException("Активный физический сетевой интерфейс не найден")
        }

        val interfaces: Enumeration<NetworkInterface> = networkInterfacesProvider()

        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()

            // Фильтруем интерфейсы, имя которых начинается с "en" (например, en0, en1), и которые не являются loopback или виртуальными
            if (!iface.isLoopback && iface.isUp && !iface.isVirtual &&
                (iface.displayName.startsWith("en") || iface.displayName.startsWith("utun"))) {

                // Если интерфейс начинается с "en" или "utun", возвращаем "Wi-Fi"
                return "Wi-Fi"
            }
        }

        throw IOException("Активный физический сетевой интерфейс не найден")
    }

    /**
     * Ищет свободный локальный TCP-порт.
     *
     * Использует механизм ServerSocket с портом 0 для автоматического выбора доступного порта системой.
     *
     * @param defaultPort Порт по умолчанию, который будет возвращён в случае ошибки поиска.
     * @return Свободный порт или порт по умолчанию при возникновении ошибки.
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