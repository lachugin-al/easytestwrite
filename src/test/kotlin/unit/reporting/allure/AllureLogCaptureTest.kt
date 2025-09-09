package unit.reporting.allure

import ch.qos.logback.classic.Logger
import io.mockk.mockkStatic
import io.mockk.verify
import io.qameta.allure.Allure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reporting.allure.AllureLogCapture

/**
 * Unit tests for [AllureLogCapture].
 *
 * Verifies:
 * - Attaching buffered logs to the Allure report and clearing the buffer afterwards
 * - Retrieving accumulated logs and clearing them
 */
class AllureLogCaptureTest {

    @BeforeEach
    fun setUp() {
        AllureLogCapture.initialize()
        AllureLogCapture.clearLogs()
    }

    @AfterEach
    fun tearDown() {
        AllureLogCapture.clearLogs()
    }

    @Test
    fun `attachLogsToAllureReport should attach and clear when logs present`() {
        mockkStatic(Allure::class)
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.info("Hello World")

        AllureLogCapture.attachLogsToAllureReport()
        verify(exactly = 1) {
            Allure.addAttachment(
                match { it.contains("Logs", ignoreCase = true) },
                "text/plain",
                any<java.io.InputStream>(),
                "txt"
            )
        }
    }

    @Test
    fun `getLogs returns content and clearLogs empties queue`() {
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.warn("Warning message")

        val logs = AllureLogCapture.getLogs()
        assert(logs.contains("Warning message"))

        AllureLogCapture.clearLogs()
        assert(AllureLogCapture.getLogs().isEmpty())
    }
}
