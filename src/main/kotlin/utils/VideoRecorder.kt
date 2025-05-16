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
 *
 * Позволяет записывать видео выполнения тестов на мобильных устройствах Android и iOS.
 * Поддерживает настройку размера, качества и битрейта видео через конфигурационные параметры.
 * Записанные видео сохраняются в указанную директорию и могут быть прикреплены к отчету Allure.
 */
object VideoRecorder {
    private val logger = LoggerFactory.getLogger(VideoRecorder::class.java)

    private var currentVideoPath: String? = null
    private var isRecording = false

    /**
     * Запускает запись видео для текущего теста.
     *
     * Метод инициализирует запись видео с использованием соответствующих команд Appium
     * в зависимости от платформы (Android или iOS). Создает директорию для сохранения видео,
     * если она не существует, и генерирует уникальное имя файла на основе имени теста и временной метки.
     *
     * @param driver Экземпляр AppiumDriver для взаимодействия с мобильным устройством
     * @param testName Имя теста, которое будет использовано в названии файла видеозаписи
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
     * Метод останавливает запись видео, декодирует полученный Base64-закодированный контент,
     * сохраняет видеофайл в указанную директорию и, если требуется, прикрепляет видео к отчету Allure.
     * Имя файла формируется на основе имени теста и временной метки, сгенерированной при старте записи.
     *
     * @param driver Экземпляр AppiumDriver для взаимодействия с мобильным устройством
     * @param testName Имя теста, используется для логирования
     * @param attachToAllure Флаг, указывающий, нужно ли прикреплять видео к отчету Allure (по умолчанию true)
     * @return True, если запись успешно остановлена и сохранена, иначе false
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
     * Метод обращается к AppConfig для получения значения параметра video.recording.enabled,
     * который может быть установлен в config.properties или передан через командную строку.
     *
     * @return True, если запись видео включена в конфигурации, иначе false
     */
    fun isEnabled(): Boolean = AppConfig.isVideoRecordingEnabled()

    /**
     * Проверяет, настроен ли размер видео с нестандартным значением.
     *
     * Метод проверяет, отличается ли текущее значение параметра video.recording.size
     * от стандартного значения "1280x720". Используется для определения,
     * были ли применены пользовательские настройки размера видео.
     *
     * @return True, если размер видео отличается от стандартного значения, иначе false
     */
    fun isVideoSizeConfigured(): Boolean = AppConfig.getVideoRecordingSize() != "1280x720"

    /**
     * Проверяет, настроено ли качество видео с нестандартным значением.
     *
     * Метод проверяет, отличается ли текущее значение параметра video.recording.quality
     * от стандартного значения 70. Используется для определения,
     * были ли применены пользовательские настройки качества видео.
     *
     * @return True, если качество видео отличается от стандартного значения, иначе false
     */
    fun isVideoQualityConfigured(): Boolean = AppConfig.getVideoRecordingQuality() != 70
}
