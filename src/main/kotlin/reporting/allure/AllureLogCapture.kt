package reporting.allure

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import io.qameta.allure.Allure
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Utility class for capturing logs from all threads and attaching them to Allure reports.
 */
object AllureLogCapture {
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val memoryAppender = MemoryAppender()
    private var initialized = false

    /**
     * Initializes the log capture system by adding a memory appender to the root logger.
     * Should be called once at application startup.
     */
    fun initialize() {
        if (initialized) return

        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        val rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME)

        memoryAppender.context = loggerContext
        memoryAppender.start()
        rootLogger.addAppender(memoryAppender)

        initialized = true
    }

    /**
     * Returns all captured logs as a single string.
     */
    fun getLogs(): String {
        return logQueue.joinToString("\n")
    }

    /**
     * Clears all captured logs.
     */
    fun clearLogs() {
        logQueue.clear()
    }

    /**
     * Attaches the current logs to the Allure report and clears them afterwards.
     */
    fun attachLogsToAllureReport() {
        val logs = getLogs()
        if (logs.isNotEmpty()) {
            Allure.addAttachment(
                "Console Logs",
                "text/plain",
                ByteArrayInputStream(logs.toByteArray(StandardCharsets.UTF_8)),
                "txt"
            )
            clearLogs()
        }
    }

    /**
     * Custom Logback appender that stores logging events in memory.
     */
    private class MemoryAppender : AppenderBase<ILoggingEvent>() {
        override fun append(event: ILoggingEvent) {
            val formattedLog = "${event.timeStamp} [${event.threadName}] ${event.level} ${event.loggerName} - ${event.formattedMessage}"
            logQueue.add(formattedLog)

            // Add exception details if present
            event.throwableProxy?.let { throwable ->
                logQueue.add(throwable.message ?: "")
                throwable.stackTraceElementProxyArray.forEach { stackTraceElement ->
                    logQueue.add("    at $stackTraceElement")
                }
            }
        }
    }
}
