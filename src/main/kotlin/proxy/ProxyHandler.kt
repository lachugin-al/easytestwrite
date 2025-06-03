package proxy

import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket

/**
 * ProxyHandler - Обрабатывает клиентские подключения и перенаправляет запросы
 */
object ProxyHandler {

    // Обрабатывает запросы клиента
    fun handleClient(clientSocket: Socket) {
        clientSocket.use { socket ->
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            println("Received request: $requestLine")

            // Проверка типа запроса: если это CONNECT, устанавливаем туннель
            if (requestLine.startsWith("CONNECT")) {
                handleConnectRequest(socket, requestLine)
            } else {
                handleHttpRequest(socket, requestLine)
            }
        }
    }

    // Обработка HTTP-запросов
    private fun handleHttpRequest(socket: Socket, requestLine: String) {
        val rawUrl = requestLine.split(" ")[1]
        val url = if (rawUrl.startsWith("/")) rawUrl.substring(1) else rawUrl

        val request = Request.Builder()
            .url(url)  // Предполагается, что это HTTPS-запрос
            .build()

        try {
            HttpClientProvider.client.newCall(request).execute().use { response ->
                sendResponse(socket.getOutputStream(), response)
            }
        } catch (e: Exception) {
            println("Error handling client request: ${e.message}")
            socket.getOutputStream().use { output ->
                output.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".toByteArray())
            }
        }
    }

    // Обработка CONNECT-запросов для установки HTTPS-туннеля
    private fun handleConnectRequest(socket: Socket, requestLine: String) {
        val target = requestLine.split(" ")[1]
        val host = target.split(":")[0]
        val port = target.split(":")[1].toInt()

        println("Handling CONNECT request to $host on port $port") // Логируем данные CONNECT запроса

        try {
            // Устанавливаем соединение с целевым сервером
            val targetSocket = Socket(host, port)
            socket.getOutputStream().use { output ->
                output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                output.flush()

                // Логирование успешного подключения
                println("Connection established with $host:$port")

                // Перенаправляем трафик между клиентом и целевым сервером
                val targetInput = targetSocket.getInputStream()
                val targetOutput = targetSocket.getOutputStream()
                val clientInput = socket.getInputStream()
                val clientOutput = socket.getOutputStream()

                // Запуск потоков для обмена данными в обоих направлениях
                val clientToTarget = Thread { forwardStreamData(clientInput, targetOutput) }
                val targetToClient = Thread { forwardStreamData(targetInput, clientOutput) }
                clientToTarget.start()
                targetToClient.start()

                clientToTarget.join()
                targetToClient.join()
            }
        } catch (e: Exception) {
            println("Error handling CONNECT request to $host:$port - ${e.message}")
            socket.getOutputStream().use { output ->
                output.write("HTTP/1.1 500 Internal Server Error\r\n\r\n".toByteArray())
            }
        }
    }

    // Метод для пересылки данных между потоками
    private fun forwardStreamData(inputStream: java.io.InputStream, outputStream: OutputStream) {
        try {
            inputStream.copyTo(outputStream)
            outputStream.flush()
        } catch (e: Exception) {
            println("Stream forwarding error: ${e.message}")
        }
    }

    // Отправка HTTP-ответа клиенту
    private fun sendResponse(outputStream: OutputStream, response: Response) {
        outputStream.use { out ->
            out.write("HTTP/1.1 ${response.code} ${response.message}\r\n".toByteArray())
            for ((name, value) in response.headers) {
                out.write("$name: $value\r\n".toByteArray())
            }
            out.write("\r\n".toByteArray())
            out.write(response.body?.bytes() ?: ByteArray(0))
        }
    }
}