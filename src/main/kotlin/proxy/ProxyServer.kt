package proxy

import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * ProxyServer - основной класс для запуска сервера
 * Настраивает сервер и делегирует обработку клиентских запросов ProxyHandler
 */
class ProxyServer : AutoCloseable {
    private val logger: Logger = LoggerFactory.getLogger(ProxyServer::class.java)

    private val host: String
    private val port: Int

    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private var proxyThread: Thread? = null

    /**
     * Создает экземпляр прокси-сервера с указанными параметрами.
     * 
     * @param host IP-адрес, на котором будет запущен сервер
     * @param port Порт, на котором будет запущен сервер
     */
    constructor(host: String = "127.0.0.1", port: Int = 9090) {
        this.host = host
        this.port = port
    }

    /**
     * Запускает прокси-сервер.
     */
    fun start() {
        if (running.get()) {
            logger.info("Proxy server is already running")
            return
        }

        try {
            serverSocket = ServerSocket()
            serverSocket?.bind(InetSocketAddress(host, port))
            running.set(true)

            proxyThread = Thread {
                logger.info("Proxy server is running on $host:$port")
                while (running.get() && !Thread.currentThread().isInterrupted) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        Thread { ProxyHandler.handleClient(clientSocket) }.start()
                    } catch (e: Exception) {
                        if (running.get()) {
                            logger.error("Error accepting client connection: ${e.message}")
                        }
                    }
                }
            }

            proxyThread?.start()
        } catch (e: Exception) {
            logger.error("Failed to start proxy server: ${e.message}")
            close()
        }
    }

    /**
     * Останавливает прокси-сервер.
     */
    override fun close() {
        if (!running.getAndSet(false)) {
            return
        }

        try {
            serverSocket?.close()
            proxyThread?.interrupt()
            proxyThread?.join(5000) // Wait up to 5 seconds for the thread to terminate
            logger.info("Proxy server stopped")
        } catch (e: Exception) {
            logger.error("Error stopping proxy server: ${e.message}")
        } finally {
            serverSocket = null
            proxyThread = null
        }
    }
}
