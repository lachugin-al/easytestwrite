package utils

import app.config.AppConfig
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import io.qameta.allure.Allure
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Служебный класс для записи видео во время выполнения тестов.
 * 
 * Этот класс предоставляет функциональность для запуска и остановки записи видео
 * для мобильной автоматизации тестирования. Он поддерживает настраиваемые размер и качество видео,
 * и может быть включен/отключен через параметры конфигурации.
 */
object VideoRecorder {
    private val logger = LoggerFactory.getLogger(VideoRecorder::class.java)

    private var currentVideoPath: String? = null
    private var isRecording = false

    /**
     * Запускает запись видео для текущего теста.
     * 
     * @param driver Экземпляр AppiumDriver
     * @param testName Имя записываемого теста
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

            // Создаем директорию для вывода, если она не существует
            val outputDir = File(AppConfig.getVideoRecordingOutputDir())
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            // Генерируем уникальное имя файла на основе имени теста и временной метки
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val safeTestName = testName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            currentVideoPath = "${AppConfig.getVideoRecordingOutputDir()}/${safeTestName}_$timestamp.mp4"

            // Начинаем запись с указанным размером, качеством и битрейтом
            val options = mapOf(
                "videoSize" to AppConfig.getVideoRecordingSize(),
                "videoQuality" to AppConfig.getVideoRecordingQuality().toString(),
                "timeLimit" to "1800", // максимум 30 минут
                "forceRestart" to "true",
                "bitRate" to AppConfig.getVideoRecordingBitrate().toString() // Используем настраиваемый битрейт для уменьшения размера файла
            )

            driver.executeScript("mobile:startMediaProjectionRecording", options)
            isRecording = true
            logger.info("Запись видео начата с размером: ${AppConfig.getVideoRecordingSize()}, качеством: ${AppConfig.getVideoRecordingQuality()}, битрейтом: ${AppConfig.getVideoRecordingBitrate() / 1000} Kbps")
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
     * @param testName Имя записываемого теста
     * @param attachToAllure Прикреплять ли видео к отчёту Allure
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

            // Останавливаем запись и получаем видео в формате base64
            val base64Video = driver.executeScript("mobile:stopMediaProjectionRecording") as String
            isRecording = false

            // Декодируем и сохраняем видео
            val videoBytes = java.util.Base64.getDecoder().decode(base64Video)
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
