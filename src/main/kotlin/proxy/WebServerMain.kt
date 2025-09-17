package proxy

// WebServer for debug mode
fun main() {
    val server = WebServer()
    server.start()

    println("WebServer started at ${server.getServerUrl()}")
    println("Press Enter to stop the server...")
    readlnOrNull()

    server.close()
}
