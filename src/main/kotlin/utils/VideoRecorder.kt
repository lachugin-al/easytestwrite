package utils

import app.config.AppConfig
import app.model.Platform
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import io.appium.java_client.screenrecording.CanRecordScreen
import io.qameta.allure.Allure
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Универсальный класс для записи видео во время выполнения мобильных тестов.
 */
object VideoRecorder {
    private val logger = LoggerFactory.getLogger(VideoRecorder::class.java)

    private var currentVideoPath: String? = null
    private var isRecording = false

    /**
     * Запускает запись видео для текущего теста.
     *
     * @param driver Экземпляр AppiumDriver
     * @param testName Имя теста
     * @return True, если запись успешно запущена, иначе false
     */
    fun startRecording(driver: AppiumDriver<MobileElement>, testName: String): Boolean {
        if (!AppConfig.isVideoRecordingEnabled()) {
            logger.info("Запись видео отключена. Пропускаем.")
            return false
        }

        if (isRecording) {
            logger.warn("Запись видео уже выполняется. Игнорируем запрос на запуск.")
            return false
        }

        try {
            logger.info("Запуск записи видео для теста: $testName")
            val outputDir = File(AppConfig.getVideoRecordingOutputDir())
            if (!outputDir.exists()) outputDir.mkdirs()

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val safeTestName = testName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            currentVideoPath = "${AppConfig.getVideoRecordingOutputDir()}/${safeTestName}_$timestamp.mp4"

            when (AppConfig.getPlatform()) {
                Platform.ANDROID -> {
                    // Используем mobile:startMediaProjectionRecording для Android
                    val androidOptions = mapOf(
                        "videoSize" to AppConfig.getVideoRecordingSize(),
                        "videoQuality" to AppConfig.getVideoRecordingQuality().toString(),
                        "timeLimit" to "1800",
                        "forceRestart" to "true",
                        "bitRate" to AppConfig.getVideoRecordingBitrate().toString()
                    )
                    driver.executeScript("mobile:startMediaProjectionRecording", androidOptions)
                    logger.info(
                        "Запись видео на Android начата с размером: ${AppConfig.getVideoRecordingSize()}, " +
                                "качеством: ${AppConfig.getVideoRecordingQuality()}, " +
                                "битрейтом: ${AppConfig.getVideoRecordingBitrate() / 1000} Kbps"
                    )
                }
                Platform.IOS -> {
                    // Используем startRecordingScreen для iOS через интерфейс CanRecordScreen
                    if (driver is CanRecordScreen) {
                        driver.startRecordingScreen()
                        logger.info("Запись видео на iOS начата через startRecordingScreen()")
                    } else {
                        logger.error("Драйвер не поддерживает запись экрана для iOS (не реализует CanRecordScreen)")
                        return false
                    }
                }
                else -> {
                    logger.error("Запись видео не поддерживается для платформы: ${AppConfig.getPlatform()}")
                    return false
                }
            }

            isRecording = true
            return true
        } catch (e: Exception) {
            logger.error("Не удалось начать запись видео: ${e.message}", e)
            return false
        }
    }

    /**
     * Останавливает текущую запись видео и прикрепляет её к отчёту Allure.
     *
     * @param driver Экземпляр AppiumDriver
     * @param testName Имя теста
     * @param attachToAllure Прикреплять ли видео к Allure
     * @return True, если запись успешно остановлена, иначе false
     */
    fun stopRecording(
        driver: AppiumDriver<MobileElement>,
        testName: String,
        attachToAllure: Boolean = true
    ): Boolean {
        if (!isRecording) {
            logger.warn("Запись видео не выполняется. Игнорируем запрос на остановку.")
            return false
        }

        try {
            logger.info("Остановка записи видео для теста: $testName")

            val base64Video: String = when (AppConfig.getPlatform()) {
                Platform.ANDROID -> {
                    driver.executeScript("mobile:stopMediaProjectionRecording") as String
                }
                Platform.IOS -> {
                    if (driver is CanRecordScreen) {
                        driver.stopRecordingScreen()
                    } else {
                        logger.error("Драйвер не поддерживает запись экрана для iOS (не реализует CanRecordScreen)")
                        isRecording = false
                        return false
                    }
                }
                else -> {
                    logger.error("Остановка записи видео не поддерживается для платформы: ${AppConfig.getPlatform()}")
                    isRecording = false
                    return false
                }
            }

            isRecording = false

            // Декодируем и сохраняем видео
            val videoBytes = Base64.getDecoder().decode(base64Video)
            val videoPath = currentVideoPath ?: "${AppConfig.getVideoRecordingOutputDir()}/unknown_test.mp4"
            Files.write(Paths.get(videoPath), videoBytes)
            logger.info("Видео сохранено в: $videoPath")

            // Прикрепляем к отчету Allure, если запрошено
            if (attachToAllure) {
                Allure.addAttachment(
                    "Запись теста",
                    "video/mp4",
                    ByteArrayInputStream(videoBytes),
                    "mp4"
                )
                logger.info("Видео прикреплено к отчету Allure")
            }

            return true
        } catch (e: Exception) {
            logger.error("Не удалось остановить запись видео: ${e.message}", e)
            isRecording = false
            return false
        }
    }

    /**
     * Проверяет, включена ли запись видео в текущей конфигурации.
     *
     * @return True, если запись видео включена, иначе false
     */
    fun isEnabled(): Boolean = AppConfig.isVideoRecordingEnabled()

    /**
     * Проверяет, настроен ли размер видео с нестандартным значением.
     *
     * @return True, если размер видео настроен, иначе false
     */
    fun isVideoSizeConfigured(): Boolean = AppConfig.getVideoRecordingSize() != "1280x720"

    /**
     * Проверяет, настроено ли качество видео с нестандартным значением.
     *
     * @return True, если качество видео настроено, иначе false
     */
    fun isVideoQualityConfigured(): Boolean = AppConfig.getVideoRecordingQuality() != 70
}
