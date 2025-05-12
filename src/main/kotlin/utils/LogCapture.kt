package utils

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
 * Служебный класс для захвата логов со всех потоков и прикрепления их к отчетам Allure.
 */
object LogCapture {
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val memoryAppender = MemoryAppender()
    private var initialized = false

    /**
     * Инициализирует систему захвата логов путем добавления аппендера памяти к корневому логгеру.
     * Должен вызываться один раз при запуске приложения.
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
     * Получает все захваченные логи в виде одной строки.
     */
    fun getLogs(): String {
        return logQueue.joinToString("\n")
    }

    /**
     * Очищает все захваченные логи.
     */
    fun clearLogs() {
        logQueue.clear()
    }

    /**
     * Прикрепляет текущие логи к отчету Allure и очищает логи.
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
     * Пользовательский аппендер Logback, который хранит события логирования в памяти.
     */
    private class MemoryAppender : AppenderBase<ILoggingEvent>() {
        override fun append(event: ILoggingEvent) {
            val formattedLog = "${event.timeStamp} [${event.threadName}] ${event.level} ${event.loggerName} - ${event.formattedMessage}"
            logQueue.add(formattedLog)

            // Добавляем информацию об исключении, если оно присутствует
            event.throwableProxy?.let { throwable ->
                logQueue.add(throwable.message ?: "")
                throwable.stackTraceElementProxyArray.forEach { stackTraceElement ->
                    logQueue.add("    at $stackTraceElement")
                }
            }
        }
    }
}
