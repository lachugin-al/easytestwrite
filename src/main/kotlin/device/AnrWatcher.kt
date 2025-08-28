package device

import io.appium.java_client.MobileElement
import io.appium.java_client.android.AndroidDriver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.openqa.selenium.NoSuchElementException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Утилита для мониторинга и автоматической обработки ANR (Application Not Responding) диалогов в Android.
 *
 * ANR Watcher запускается в отдельном потоке и периодически проверяет наличие диалогов ANR.
 * При обнаружении диалога, он автоматически нажимает кнопку "Подождать" или "Закрыть".
 *
 * Рекомендуется запускать ANR Watcher один раз в начале всех тестов с помощью аннотации @BeforeAll.
 */
object AnrWatcher {
    private val logger: Logger = LoggerFactory.getLogger(AnrWatcher::class.java)
    private var job: Job? = null

    /**
     * Запускает ANR Watcher для мониторинга ANR диалогов.
     *
     * @param driver AndroidDriver для взаимодействия с приложением
     * @param intervalMillis интервал проверки в миллисекундах
     */
    fun start(driver: AndroidDriver<MobileElement>, intervalMillis: Long = 2000L) {
        if (job != null) {
            logger.info("ANR Watcher уже запущен")
            return  // уже запущен
        }

        logger.info("Запуск ANR Watcher с интервалом $intervalMillis мс")
        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val pageSource = driver.pageSource
                    if (pageSource.contains("не отвечает", ignoreCase = true)) {
                        logger.info("Обнаружено ANR-окно. Пытаемся нажать 'Подождать' или 'Закрыть'.")

                        try {
                            val waitButton = driver.findElementByAndroidUIAutomator(
                                "new UiSelector().textContains(\"Подождать\")"
                            )
                            waitButton.click()
                            logger.info("Нажата кнопка 'Подождать'")
                        } catch (e: NoSuchElementException) {
                            try {
                                val closeButton = driver.findElementByAndroidUIAutomator(
                                    "new UiSelector().textContains(\"Закрыть приложение\")"
                                )
                                closeButton.click()
                                logger.info("Нажата кнопка 'Закрыть'")
                            } catch (ignored: NoSuchElementException) {
                                logger.warn("Не удалось найти кнопки 'Подождать' или 'Закрыть'")
                            }
                        }
                    }

                    delay(intervalMillis)
                } catch (e: CancellationException) {
                    // Игнорируем CancellationException, так как это ожидаемое поведение при отмене корутины
                    logger.debug("Корутина ANR Watcher была отменена: ${e.message}")
                } catch (e: Exception) {
                    logger.error("Ошибка в корутине ANR Watcher: ${e.message}", e)
                    delay(intervalMillis)
                }
            }
        }
    }

    /**
     * Останавливает ANR Watcher.
     */
    fun stop() {
        try {
            job?.cancel()
            job = null
            logger.info("ANR Watcher остановлен")
        } catch (e: Exception) {
            // Игнорируем JobCancellationException, так как это ожидаемое поведение при отмене корутины
            logger.debug("Исключение при остановке ANR Watcher: ${e.message}")
        }
    }
}