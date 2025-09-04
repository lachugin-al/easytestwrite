package controller.mobile.base

import app.App
import app.config.AppConfig
import app.model.Platform
import controller.mobile.alert.AlertActions
import controller.mobile.core.AppContext
import controller.mobile.deeplink.DeeplinkOpener
import controller.mobile.events.EventDrivenUi
import controller.mobile.events.EventVerifier
import controller.mobile.interaction.UiClickActions
import controller.mobile.interaction.UiElementFinding
import controller.mobile.interaction.UiScrollGestures
import controller.mobile.interaction.UiTapActions
import controller.mobile.interaction.UiGetValueActions
import controller.mobile.interaction.UiTypingActions
import controller.mobile.interaction.UiVisibilityChecks
import controller.mobile.nativeactions.NativeActions
import dsl.testing.TestingContext
import events.EventStorage
import io.appium.java_client.MobileElement
import io.appium.java_client.android.AndroidDriver
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reporting.allure.AllureLogCapture
import reporting.artifacts.video.VideoRecorder
import org.junit.jupiter.api.TestInfo
import device.AnrWatcher
import device.EmulatorManager
import dsl.testing.SkipConditionExtension
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Базовый контроллер для мобильных тестов.
 *
 * Основной класс, который содержит общую инфраструктуру для работы с драйверами Appium/Web,
 * взаимодействия с UI-элементами, отправки действий пользователя, проверки событий в EventStorage,
 * а также вспомогательных операций для скроллирования и свайпов на экране.
 *
 * Данный класс должен наследоваться всеми тестовыми классами.
 */
@ExtendWith(SkipConditionExtension::class)
open class MobileTest:
    AppContext,
    UiElementFinding,
    UiScrollGestures,
    UiTapActions,
    UiClickActions,
    UiGetValueActions,
    UiVisibilityChecks,
    UiTypingActions,
    EventVerifier,
    EventDrivenUi,
    DeeplinkOpener,
    NativeActions,
    AlertActions
{

    // ---- инфраструктура и зависимости ----
    override val logger: Logger = LoggerFactory.getLogger(MobileTest::class.java)

    override lateinit var app: App
    protected lateinit var context: TestingContext

    // Хранилище событий
    override val eventsFileStorage = EventStorage

    // CoroutineScope для управления жизненным циклом корутин
    override val scope = CoroutineScope(Dispatchers.Default)
    override val jobs = mutableListOf<Deferred<*>>()

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MobileTest::class.java)
        private var emulatorStarted = false

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            // Проверяем, включен ли автозапуск эмулятора
            if (AppConfig.isEmulatorAutoStartEnabled()) {
                // Запуск эмулятора перед всеми тестами
                logger.info("Запуск эмулятора перед всеми тестами")
                emulatorStarted = EmulatorManager.startEmulator()
                if (!emulatorStarted) {
                    logger.error("Не удалось запустить эмулятор")
                }
            } else {
                logger.info("Автозапуск эмулятора отключен в настройках")
            }
        }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            // Проверяем, включено ли автовыключение эмулятора
            if (AppConfig.isEmulatorAutoShutdownEnabled()) {
                // Остановка эмулятора после всех тестов
                logger.info("Остановка эмулятора после всех тестов")
                EmulatorManager.stopEmulator()
            } else {
                logger.info("Автовыключение эмулятора отключено в настройках")
            }
        }
    }

    // ---- жизненный цикл ----
// Функционал, выполняемый перед каждым тестом
    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        // Очистка логов перед началом теста
        AllureLogCapture.clearLogs()
        // Инициализация системы захвата логов
        AllureLogCapture.initialize()
        // Очистка хранилища событий перед началом теста
        EventStorage.clear()

        // Проверяем, был ли успешно запущен эмулятор, если он требуется
        if (AppConfig.getPlatform() == Platform.ANDROID &&
            AppConfig.isEmulatorAutoStartEnabled() &&
            !emulatorStarted
        ) {
            logger.warn("Эмулятор не был успешно запущен в setUpAll(), пробуем запустить снова")
            emulatorStarted = EmulatorManager.startEmulator()
            if (!emulatorStarted) {
                logger.error("Не удалось запустить эмулятор перед тестом")
                throw RuntimeException("Не удалось инициализировать Android-драйвер. Проверьте запущен ли эмулятор.")
            }
        }

        // Гарантируем Wi‑Fi на эмуляторе Android перед каждым тестом
        if (AppConfig.getPlatform() == Platform.ANDROID) {
            val needEnsure = AppConfig.isEmulatorAutoStartEnabled() || (EmulatorManager.getEmulatorId() != null)
            if (needEnsure) {
                val wifiOk = EmulatorManager.ensureAndroidWifiConnectivity()
                if (!wifiOk) {
                    logger.error("Не удалось обеспечить использование Wi‑Fi на Android эмуляторе перед тестом")
                    throw RuntimeException("Wi‑Fi не настроен на эмуляторе. Остановите тест или проверьте окружение.")
                }
            }
        }

        // Инициализация приложения
        app = App().launch()
        context = TestingContext(driver)

        // Запуск записи видео
        val testName = testInfo.displayName
        VideoRecorder.startRecording(driver, testName)

        // Запуск AnrWatcher для Android
        if (AppConfig.getPlatform() == Platform.ANDROID) {
            val androidDriver = driver as? AndroidDriver<MobileElement>
            androidDriver?.let { AnrWatcher.start(it) }
        }
    }

    // Функционал, выполняемый после каждого теста
    @AfterEach
    fun tearDown(testInfo: TestInfo) {
        // Ожидание завершения всех проверок событий
        awaitAllEventChecks()

        // Остановка записи видео
        val testName = testInfo.displayName
        VideoRecorder.stopRecording(driver, testName)

        // Остановка AnrWatcher для Android
        if (AppConfig.getPlatform() == Platform.ANDROID) {
            AnrWatcher.stop()
        }

        // Прикрепление логов к отчету Allure
        AllureLogCapture.attachLogsToAllureReport()

        // Закрытие приложения
        closeApp()
    }

    /**
     * Закрывает приложение.
     * Этот метод может быть вызван из внешних классов, например, из SkipConditionExtension,
     * когда тест пропускается из-за аннотации @Skip.
     */
    fun closeApp() {
        if (this::app.isInitialized) {
            app.close()
        } else {
            logger.debug("closeApp() called, but 'app' is not initialized; skipping close.")
        }
    }









}