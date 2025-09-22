package app.appium.server

import app.config.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Duration

/**
 * Manages lifecycle of a local Appium server process for test runs.
 *
 * Responsibilities:
 * - Start Appium before all tests using host/port from AppConfig.getAppiumUrl().
 * - Verify server health via /status endpoint.
 * - Monitor server during the test session and auto-restart on failure.
 * - Stop the server after all tests finish (only if it was started by this manager).
 */
object AppiumServerManager {
    private val logger = LoggerFactory.getLogger(AppiumServerManager::class.java)

    @Volatile private var process: Process? = null
    @Volatile private var startedByUs: Boolean = false
    @Volatile private var shuttingDown: Boolean = false

    // Monitor scope
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    // Polling/health-check settings
    private const val DEFAULT_POLL_INTERVAL_MS = 3000L
    private const val FAILURE_THRESHOLD = 3
    private val START_TIMEOUT: Duration = Duration.ofSeconds(30)

    /**
     * Ensure Appium is running and start monitoring it.
     * Safe to call multiple times.
     */
    @Synchronized
    fun ensureStartedAndMonitored() {
        val url = AppConfig.getAppiumUrl()
        val host = url.host
        val port = if (url.port > 0) url.port else url.defaultPort

        // If an instance already responds on the configured URL, adopt it and start monitor
        if (isHealthy(url)) {
            logger.info("Detected running Appium at $host:$port, will monitor its health")
            startedByUs = false
            startMonitoring(url)
            return
        }

        // Otherwise start a new instance
        startProcess(host, port)
        waitUntilHealthy(url, START_TIMEOUT)
        startMonitoring(url)
    }

    /**
     * Stop monitoring and gracefully stop the process if we started it.
     */
    @Synchronized
    fun shutdown() {
        shuttingDown = true
        try {
            monitorJob?.cancel()
            monitorJob = null
        } catch (e: Exception) {
            logger.warn("Failed to cancel monitor job: ${e.message}")
        }

        // Stop only if we started it
        if (startedByUs) {
            val p = process
            process = null
            if (p != null && p.isAlive) {
                logger.info("Stopping Appium server (started by test framework)")
                runCatching { p.destroy() }
                runBlocking {
                    withTimeoutOrNull(5000) {
                        while (p.isAlive) delay(100)
                    }
                }
                if (p.isAlive) {
                    logger.warn("Appium didn't exit in time, destroying forcibly")
                    runCatching { p.destroyForcibly() }
                }
            }
        } else {
            logger.info("Appium server was not started by the framework, leaving it running")
        }
        startedByUs = false
        shuttingDown = false
    }

    @Synchronized
    private fun startProcess(host: String, port: Int) {
        logger.info("Starting Appium server on $host:$port …")
        val command = buildList {
            // Run via shell to let OS resolve proper appium shim (appium / appium.cmd)
            addAll(shellPrefix())
            add("appium --address $host --port $port")
        }
        val builder = ProcessBuilder(command)
            .redirectErrorStream(true)

        val p = builder.start()
        process = p
        startedByUs = true

        // Attach async log reader (non-blocking)
        monitorScope.launch(Dispatchers.IO) {
            BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    logger.debug("[appium] $line")
                }
            }
        }
    }

    private fun shellPrefix(): List<String> =
        if (System.getProperty("os.name").lowercase().contains("win")) listOf("cmd", "/c")
        else listOf("bash", "-lc")

    private fun waitUntilHealthy(url: URL, timeout: Duration) {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            if (isHealthy(url)) {
                logger.info("Appium server is up at $url")
                return
            }
            Thread.sleep(500)
        }
        // If not healthy by timeout and we started it — terminate process to avoid zombie
        if (startedByUs) {
            process?.let { if (it.isAlive) it.destroyForcibly() }
        }
        error("Failed to start Appium server at $url within $timeout")
    }

    private fun isHealthy(baseUrl: URL): Boolean {
        val base = baseUrl.toString().trimEnd('/')
        val endpoints = listOf("/status", "/sessions", "/")
        for (ep in endpoints) {
            val url = URI(base + ep).toURL()
            try {
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("Accept", "application/json")
                val code = conn.responseCode
                if (code in 200..299) {
                    runCatching { conn.inputStream.use { /* consume */ } }
                    conn.disconnect()
                    return true
                } else {
                    runCatching { conn.errorStream?.close() }
                    conn.disconnect()
                }
            } catch (_: Exception) {
                // Try next endpoint
            }
        }
        return false
    }

    private fun startMonitoring(url: URL) {
        if (monitorJob?.isActive == true) return
        monitorJob = monitorScope.launch {
            var failures = 0
            while (isActive && !shuttingDown) {
                val alive = process?.isAlive ?: false
                val healthy = isHealthy(url)
                if (!alive || !healthy) failures++ else failures = 0

                if (failures >= FAILURE_THRESHOLD) {
                    logger.warn("Appium server health failures detected (count=$failures). Attempting restart…")
                    // Try to stop existing
                    process?.let { p ->
                        runCatching { p.destroy() }
                        withContext(Dispatchers.IO) {
                            withTimeoutOrNull(3000) {
                                while (p.isAlive) delay(100)
                            }
                        }
                        if (p.isAlive) runCatching { p.destroyForcibly() }
                    }
                    // Start fresh
                    val host = url.host
                    val port = if (url.port > 0) url.port else url.defaultPort
                    startProcess(host, port)
                    try {
                        waitUntilHealthy(url, START_TIMEOUT)
                        failures = 0
                    } catch (e: Exception) {
                        logger.error("Failed to restart Appium: ${e.message}")
                    }
                }

                delay(DEFAULT_POLL_INTERVAL_MS)
            }
        }
    }
}