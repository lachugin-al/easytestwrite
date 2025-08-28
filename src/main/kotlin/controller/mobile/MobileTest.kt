package controller.mobile

import app.App
import app.config.AppConfig
import app.model.Platform
import com.google.common.collect.ImmutableList
import controller.element.PageElement
import controller.element.ScrollDirection
import controller.handler.AlertHandler
import dsl.testing.ExpectationContext
import dsl.testing.SkipConditionExtension
import dsl.testing.StepContext
import dsl.testing.TestingContext
import org.junit.jupiter.api.extension.ExtendWith
import events.Event
import events.EventData
import events.EventStorage
import io.appium.java_client.AppiumDriver
import io.appium.java_client.MobileElement
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.nativekey.AndroidKey
import io.appium.java_client.android.nativekey.KeyEvent
import io.appium.java_client.ios.IOSDriver
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.openqa.selenium.By
import org.openqa.selenium.interactions.Pause
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COEFFICIENT
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_SWIPE_COEFFICIENT
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION
import utils.LogCapture
import utils.TerminalUtils.runCommand
import utils.VideoRecorder
import org.junit.jupiter.api.TestInfo
import utils.DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
import utils.AnrWatcher
import utils.EmulatorManager
import utils.EmulatorManager.getSimulatorId
import utils.NumberParser
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration

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
open class MobileTest {
    private val logger: Logger = LoggerFactory.getLogger(MobileTest::class.java)
    protected lateinit var app: App
    protected lateinit var context: TestingContext
    protected val driver: AppiumDriver<MobileElement>
        get() = app.driver ?: throw IllegalStateException("Driver is not initialized")
    private val eventsFileStorage = EventStorage

    // Создаем собственную CoroutineScope для управления жизненным циклом корутин
    private val scope = CoroutineScope(Dispatchers.Default)
    private val jobs = mutableListOf<Deferred<*>>()

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

    /**
     * Найти элемент на экране по его [element] и кликнуть по нему
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     */
    fun StepContext.click(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).click()
    }

    /**
     * Найти элемент на экране, содержащий указанный текст, и кликнуть по нему.
     *
     * Метод построит локатор по стратегии `PageElement.Contains` на всех платформах (Android, iOS, Web)
     * и выполнит поиск элемента с возможностью скроллирования. Если элемент найден, выполняется клик.
     *
     * Метод полезен в ситуациях, когда необходимо кликнуть по элементу, содержащему определённую подстроку
     * (например, часть названия товара, кнопку с динамическим текстом и т.д.), без необходимости заранее
     * знать точное имя или локатор элемента.
     *
     * @param text текст, который точно соответствует в целевом элементе;
     * @param containsText текст, который должен содержаться в целевом элементе;
     * @param elementNumber номер найденного элемента, начиная с 1. Если null, будет использован первый найденный;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation максимальное время ожидания появления элемента, в секундах;
     * @param pollingInterval интервал между попытками поиска элемента, в миллисекундах;
     * @param scrollCount количество попыток скроллирования при неудачном поиске;
     * @param scrollCapacity доля экрана, на которую производится один скролл [0.0 - 1.0];
     * @param scrollDirection направление скроллирования (вниз, вверх и т.п.);
     *
     * @throws java.util.NoSuchElementException если элемент, содержащий указанный текст, не найден после всех попыток.
     */
    fun StepContext.click(
        text: String? = null,
        containsText: String? = null,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        require(!(text == null && containsText == null)) {
            "Необходимо указать либо 'text', либо 'contains'"
        }
        require(!(text != null && containsText != null)) {
            "Нельзя одновременно использовать 'text' и 'contains'"
        }
        val element = when {
            text != null -> PageElement(
                android = PageElement.ExactMatch(text),
                ios = PageElement.ExactMatch(text)
            )

            containsText != null -> PageElement(
                android = PageElement.Contains(containsText),
                ios = PageElement.Contains(containsText),
            )

            else -> error("Указан и не text и не containsText")
        }

        waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).click()
    }

    /**
     * Построить PageElement по item из события, у которого где-нибудь в data встречаются все пары из eventData.
     * Возвращает PageElement для дальнейшего использования (например, в методе click).
     *
     * @param eventPosition позиция события для обработки: "first" - первое найденное, "last" - последнее найденное
     */
    fun StepContext.pageElementMatchedEvent(
        eventName: String,
        eventData: String,
        timeoutEventExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION,
        eventPosition: String = "first"
    ): PageElement {
        val maxAttempts = (scrollCount.coerceAtLeast(0)) + 1
        var attempt = 0

        while (attempt < maxAttempts) {
            try {
                // Ждём событие по условиям
                "Ждём событие $eventName (попытка ${attempt + 1}/$maxAttempts)" {
                    checkHasEvent(eventName, eventData, timeoutEventExpectation)
                }

                // Получаем подходящее событие
                val matchedEvents = EventStorage.getEvents().filter {
                    it.name == eventName &&
                            it.data?.let { d ->
                                val json = Json.encodeToString(EventData.serializer(), d)
                                containsJsonData(json, eventData)
                            } ?: false
                }

                val matchedEvent = when (eventPosition.lowercase()) {
                    "last" -> matchedEvents.lastOrNull()
                    else -> matchedEvents.firstOrNull()
                }

                if (matchedEvent == null) {
                    // Если событие не найдено, попробуем проскроллить и повторить попытку
                    if (attempt < maxAttempts - 1) {
                        logger.info("Событие '$eventName' с фильтром '$eventData' не найдено. Скроллим (1 шаг) и пробуем снова...")
                        performScroll(
                            element = null,
                            scrollCount = 1,
                            scrollCapacity = scrollCapacity,
                            scrollDirection = scrollDirection
                        )
                        attempt++
                        continue
                    } else {
                        throw NoSuchElementException("Событие '$eventName' с фильтром '$eventData' не найдено после $maxAttempts попыток (со скроллом)")
                    }
                }

                // Извлекаем массив items из body → event → data
                val bodyObj = Json.parseToJsonElement(matchedEvent.data!!.body).jsonObject
                val itemsArr = bodyObj["event"]!!.jsonObject["data"]!!.jsonObject["items"]!!.jsonArray

                // Находим первый item, содержащий искомые пары, это будет первая карточка товара на странице
                val searchObj = Json.parseToJsonElement(eventData).jsonObject
                val matched = itemsArr.firstOrNull { itemElem ->
                    searchObj.all { (key, sv) ->
                        findKeyValueInTree(itemElem, key, sv)
                    }
                } ?: throw NoSuchElementException("В событии '$eventName' ни один item не соответствует $eventData")

                // Достаем у найденного item его поле "name"
                val itemName = matched.jsonObject["name"]!!.jsonPrimitive.content
                val positionTextCase = if (eventPosition.lowercase() == "last") "последнем" else "первом"
                logger.info("Найден подходящий товар: '$itemName' в $positionTextCase событии по фильтрам (eventName=$eventName, filter=$eventData, position=$eventPosition)")

                // Строим и возвращаем локатор
                return PageElement(
                    android = PageElement.Text(itemName),
                    ios = PageElement.Label(itemName)
                )
            } catch (t: Throwable) {
                // Если чек события не прошёл (например, по таймауту), пробуем проскроллить и повторить, если есть попытки
                if (attempt < maxAttempts - 1) {
                    logger.info("Не удалось найти событие '$eventName' на попытке ${attempt + 1}: ${t.message}. Скроллим (1 шаг) и пробуем снова...")
                    performScroll(
                        element = null,
                        scrollCount = 1,
                        scrollCapacity = scrollCapacity,
                        scrollDirection = scrollDirection
                    )
                    attempt++
                    continue
                } else {
                    // Попытки закончились — пробрасываем ошибку
                    throw NoSuchElementException("Событие '$eventName' с фильтром '$eventData' не найдено после $maxAttempts попыток (со скроллом). Последняя ошибка: ${t.message}")
                }
            }
        }

        // Теоретически недостижимо
        throw NoSuchElementException("Событие '$eventName' с фильтром '$eventData' не найдено")
    }

    /**
     * Нажать в области экрана по [x] [y]
     * @param x точка по [x] в области экрана;
     * @param y точка по [y] в области экрана;
     * @param timeoutBeforeExpectation максимальное количество секунд ожидания перед выполнением нажатия.
     * Если указан параметр [waitCondition], в течение этого времени будет ожидаться выполнение заданного условия.
     * Если [waitCondition] не задан, будет ожидаться стабилизация UI (отсутствие изменений в исходном коде страницы) в течение указанного времени.
     * Если значение равно 0, ожидание перед нажатием не производится.
     */
    fun StepContext.tapArea(
        x: Int,
        y: Int,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        waitCondition: (() -> Boolean)? = null
    ) {
        if (waitCondition != null) {
            WebDriverWait(driver, timeoutBeforeExpectation)
                .until { waitCondition.invoke() }
        } else if (timeoutBeforeExpectation > 0) {
            val beforeWait = WebDriverWait(driver, timeoutBeforeExpectation)

            // ожидание стабилизации страницы
            var previousPageSource = driver.pageSource
            beforeWait.until {
                val currentPageSource = driver.pageSource
                val isStable = currentPageSource == previousPageSource
                previousPageSource = currentPageSource
                isStable
            }
        }

        val pointerInput = PointerInput(PointerInput.Kind.TOUCH, "finger1")
        val sequence = Sequence(pointerInput, 0)
        val origin = PointerInput.Origin.viewport()

        sequence.addAction(pointerInput.createPointerMove(Duration.ZERO, origin, x, y))
        sequence.addAction(pointerInput.createPointerDown(0))
        sequence.addAction(pointerInput.createPointerUp(0))

        driver.perform(ImmutableList.of(sequence))
    }

    /**
     * Нажать в области [element] по его [x] [y]
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param x точка по [x] в области элемента;
     * @param y точка по [y] в области элемента;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     */
    fun StepContext.tapElementArea(
        element: PageElement?,
        elementNumber: Int? = null,
        x: Int,
        y: Int,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        waitForElements(
            element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).let { foundElement ->
            val elementLocation = foundElement.location
            val elementPointX = elementLocation.x + x
            val elementPointY = elementLocation.y + y

            tapArea(elementPointX, elementPointY)
        }
    }

    /**
     * Найти элемент на экране по его [element] и ввести [text]
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param text текст;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     */
    fun StepContext.typeText(
        element: PageElement?,
        elementNumber: Int? = null,
        text: String,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).sendKeys(text)
    }

    /**
     * Проверить виден ли [element] на экране
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     */
    fun ExpectationContext.checkVisible(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).isDisplayed
    }

    /**
     * Проверить, виден ли на экране элемент, содержащий указанный текст.
     *
     * Метод построит локатор по стратегии `PageElement.Contains` на всех платформах (Android, iOS, Web)
     * и выполнит поиск элемента с возможностью скроллирования. Если элемент найден, будет проверена его видимость.
     *
     * Метод полезен для DSL, когда известна только часть текста (например, кнопки, лейбла и т.п.),
     * а точный локатор не задан или изменяется динамически.
     *
     * @param text текст, который точно соответствует в целевом элементе;
     * @param containsText текст, который должен содержаться в элементе;
     * @param elementNumber порядковый номер совпавшего элемента (начиная с 1);
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation максимальное время ожидания элемента;
     * @param pollingInterval интервал между попытками;
     * @param scrollCount количество скроллов при отсутствии элемента;
     * @param scrollCapacity доля экрана, на которую происходит один скролл [0.0 - 1.0];
     * @param scrollDirection направление скроллирования;
     *
     * @throws java.util.NoSuchElementException если элемент не найден или не отображается.
     */
    fun ExpectationContext.checkVisible(
        text: String? = null,
        containsText: String? = null,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        require(!(text == null && containsText == null)) {
            "Необходимо указать либо 'text', либо 'contains'"
        }
        require(!(text != null && containsText != null)) {
            "Нельзя одновременно использовать 'text' и 'contains'"
        }
        val element = when {
            text != null -> PageElement(
                android = PageElement.ExactMatch(text),
                ios = PageElement.ExactMatch(text)
            )

            containsText != null -> PageElement(
                android = PageElement.Contains(containsText),
                ios = PageElement.Contains(containsText),
            )

            else -> error("Указан и не text и не containsText")
        }

        waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).isDisplayed
    }

    /**
     * Проверяет наличие события и его данные в EventFileStorage с указанной периодичностью в течение заданного времени.
     *
     * @param eventName Название события для поиска;
     * @param eventData Перечень данных в виде `Json String` или `Json File`, которые мы хотим проверить в событии;
     *                  Поддерживает шаблоны для значений:
     *                  - "*" - соответствует любому значению (wildcard)
     *                  - "" - соответствует только пустому значению
     *                  - "~value" - проверяет частичное совпадение (если 'value' является подстрокой значения)
     *                  - Любое другое значение - проверяется точное соответствие
     * {
     *   "items": [
     *     {
     *       "list_id": "CR",
     *       "tail_object": {
     *         "loc": "MAB",
     *         "loc_way": "CR"
     *       }
     *     }
     *   ]
     * }
     * @param timeoutExpectation Время в секундах, в течение которого будет происходить проверка;
     * @return true если событие найдено, false если нет.
     */
    fun ExpectationContext.checkHasEvent(
        eventName: String,
        eventData: String? = null,
        timeoutEventExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
    ) {
        // Вызов основной функции с уже подготовленной строкой JSON
        checkHasEventInternal(eventName, eventData, timeoutEventExpectation)
    }

    /**
     * Проверяет наличие события и его данные в EventFileStorage с указанной периодичностью в течение заданного времени.
     *
     * @param eventName Название события для поиска;
     * @param eventData Перечень данных в виде `Json String` или `Json File`, которые мы хотим проверить в событии;
     *                  Поддерживает шаблоны для значений:
     *                  - "*" - соответствует любому значению (wildcard)
     *                  - "" - соответствует только пустому значению
     *                  - "~value" - проверяет частичное совпадение (если 'value' является подстрокой значения)
     *                  - Любое другое значение - проверяется точное соответствие
     * {
     *   "items": [
     *     {
     *       "list_id": "CR",
     *       "tail_object": {
     *         "loc": "MAB",
     *         "loc_way": "CR"
     *       }
     *     }
     *   ]
     * }
     * @param timeoutExpectation Время в секундах, в течение которого будет происходить проверка;
     * @return true если событие найдено, false если нет.
     */
    fun ExpectationContext.checkHasEvent(
        eventName: String,
        eventData: File?,
        timeoutEventExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
    ) {
        val jsonData = eventData?.readText()
        checkHasEventInternal(eventName, jsonData, timeoutEventExpectation)
    }

    /**
     * Внутренняя функция ожидания события в EventStorage в течение заданного времени.
     *
     * @param eventName Название события, которое ожидается.
     * @param eventData Ожидаемое содержимое события в формате JSON (опционально).
     *                  Поддерживает шаблоны для значений:
     *                  - "*" - соответствует любому значению (wildcard)
     *                  - "" - соответствует только пустому значению
     *                  - "~value" - проверяет частичное совпадение (если 'value' является подстрокой значения)
     *                  - Любое другое значение - проверяется точное соответствие
     * @param timeoutExpectation Таймаут ожидания события в секундах.
     *
     * @throws Exception если событие не найдено за отведённое время.
     */
    private fun ExpectationContext.checkHasEventInternal(
        eventName: String,
        eventData: String? = null,
        timeoutEventExpectation: Long
    ) {
        val pollingInterval = 500L
        val timeoutInMillis = timeoutEventExpectation * 1000

        if (eventData != null) {
            println("Ожидание события '$eventName' с данными '$eventData'")
        } else {
            println("Ожидание события '$eventName'...")
        }

        runCatching {
            runBlocking {
                val result = withTimeoutOrNull(timeoutInMillis) {
                    while (true) {
                        // Получить все события из EventStorage
                        val allEvents = eventsFileStorage.getEvents()

                        // Перебрать события
                        for (event in allEvents) {
                            // Пропустить уже обработанные события
                            if (eventsFileStorage.isEventAlreadyMatched(event.event_num)) continue

                            // Проверка совпадения по названию события
                            if (event.name == eventName) {
                                if (eventData == null) {
                                    // Если дополнительные данные не требуются, отметить событие как найденное
                                    eventsFileStorage.markEventAsMatched(event.event_num)
                                    return@withTimeoutOrNull true
                                }

                                // Если ожидаются конкретные данные, сериализовать event.data в JSON
                                val eventDataJson = event.data?.let { Json.encodeToString(EventData.serializer(), it) }

                                // Проверить, содержатся ли в событии ожидаемые данные
                                if (eventDataJson != null && containsJsonData(eventDataJson, eventData)) {
                                    eventsFileStorage.markEventAsMatched(event.event_num)
                                    return@withTimeoutOrNull true
                                }
                            }
                        }

                        // Подождать перед следующей проверкой
                        delay(pollingInterval)
                    }
                } ?: false

                // Assert that the result is true and is of type Boolean
                try {
                    if (result as Boolean) {
                        println("Ожидаемое событие '$eventName' найдено.")
                    } else {
                        if (eventData != null) {
                            throw NoSuchElementException("Ожидаемое событие '$eventName' с данными '$eventData' не обнаружено за $timeoutEventExpectation секунд.")
                        } else {
                            throw NoSuchElementException("Ожидаемое событие '$eventName' не обнаружено за $timeoutEventExpectation секунд.")
                        }
                    }
                } catch (e: ClassCastException) {
                    throw Exception("Ошибка приведения результата проверки к Boolean: $result", e)
                }
            }
        }.getOrThrow()
    }

    /**
     * Проверяет, содержатся ли все ключи и значения из искомого JSON внутри JSON события.
     *
     * Поддерживает шаблоны для значений:
     * - "*" - соответствует любому значению (wildcard)
     * - "" - соответствует только пустому значению
     * - "~value" - проверяет частичное совпадение (если 'value' является подстрокой значения)
     * - Любое другое значение - проверяется точное соответствие
     *
     * @param eventJson JSON-строка события (сериализованная EventData).
     * @param searchJson JSON-строка с искомыми ключами и значениями для поиска.
     * @return true, если все ключи/значения из searchJson найдены в eventJson, иначе false.
     */
    private fun containsJsonData(eventJson: String, searchJson: String): Boolean {
        // Извлекаем поле "body" из eventJson и парсим его в JSON-объект
        val evDataObj = Json.parseToJsonElement(eventJson).jsonObject
        val bodyStr = evDataObj["body"]!!.jsonPrimitive.content
        val bodyObj = Json.parseToJsonElement(bodyStr).jsonObject

        // Переходим к узлу "event" → "data"
        val dataElement = bodyObj["event"]!!.jsonObject["data"]!!

        // Парсим искомые ключи и значения
        val searchObj = Json.parseToJsonElement(searchJson).jsonObject

        // Для каждой пары (ключ, значение) проверяем наличие соответствия в дереве dataElement
        return searchObj.all { (key, sv) ->
            findKeyValueInTree(dataElement, key, sv)
        }
    }

    /**
     * Рекурсивно обходит JsonElement и возвращает true,
     * если где-нибудь найдётся запись “ключ = key” и её значение matchJsonElement(…, searchValue)
     */
    private fun findKeyValueInTree(
        element: JsonElement,
        key: String,
        searchValue: JsonElement
    ): Boolean = when (element) {
        is JsonObject -> element.entries.any { (k, v) ->
            // либо тут совпадение по ключу+значению, либо идём глубже
            (k == key && matchJsonElement(v, searchValue))
                    || findKeyValueInTree(v, key, searchValue)
        }

        is JsonArray -> element.any { findKeyValueInTree(it, key, searchValue) }
        else -> false
    }

    /**
     * Рекурсивная функция для сопоставления элементов JSON.
     *
     * Поддерживается сопоставление:
     * - Примитивов с поддержкой шаблонов:
     *   - "*" - соответствует любому значению (wildcard)
     *   - "" - соответствует только пустому значению
     *   - "~value" - проверяет частичное совпадение (если 'value' является подстрокой значения)
     *   - Любое другое значение - проверяется точное соответствие
     * - Объектов (по ключам и значениям),
     * - Массивов (каждый элемент поиска должен быть найден в массиве события),
     * - Строк, содержащих вложенные сериализованные JSON-объекты.
     *
     * @param eventElement Элемент JSON, полученный из события.
     * @param searchElement Элемент JSON, который необходимо найти внутри события.
     * @return true, если элементы совпадают по структуре и содержимому, иначе false.
     */
    private fun matchJsonElement(eventElement: JsonElement, searchElement: JsonElement): Boolean {
        return when {
            // Сравнение примитивов с поддержкой шаблонов
            eventElement is JsonPrimitive && searchElement is JsonPrimitive -> {
                when {
                    searchElement.content == "*" -> true // Wildcard - соответствует любому значению
                    searchElement.content == "" -> eventElement.content.isEmpty() // Пустая строка - соответствует только пустому значению
                    searchElement.content.startsWith("~") -> eventElement.content.contains(
                        searchElement.content.substring(
                            1
                        )
                    ) // Частичное совпадение
                    else -> eventElement.content == searchElement.content // Точное соответствие
                }
            }

            // Сравнение вложенных JSON-строк: парсим строку и рекурсивно сравниваем
            eventElement is JsonPrimitive && eventElement.isString -> runCatching {
                matchJsonElement(
                    Json.parseToJsonElement(eventElement.content),
                    searchElement
                )
            }.getOrDefault(false)

            // Сравнение объектов: каждый ключ и значение должны соответствовать
            eventElement is JsonObject && searchElement is JsonObject ->
                searchElement.all { (k, sv) ->
                    eventElement[k]?.let { matchJsonElement(it, sv) } ?: false
                }

            // Сравнение массивов: каждый элемент из искомого массива должен быть найден в массиве события
            eventElement is JsonArray && searchElement is JsonArray ->
                searchElement.all { se ->
                    eventElement.any { ee -> matchJsonElement(ee, se) }
                }

            // Иные случаи: элементы не совпадают
            else -> false
        }
    }

    /**
     * Проверяет наличие события и данные в EventFileStorage с указанной периодичностью в течение заданного времени.
     * Проверку можно производить после действия, причем проверка будет производиться ассинхронно и не блокирует основной поток
     * Так как событие может придти позже после нескольких действий связанных между собой;
     *
     * @param eventName Название события для поиска;
     * @param eventData Перечень данных в виде `Json String` или `Json File`, которые мы хотим проверить в событии;
     *                  Поддерживает шаблоны для значений:
     *                  - "*" - соответствует любому значению (wildcard)
     *                  - "" - соответствует только пустому значению
     *                  - "~value" - проверяет частичное совпадение (если 'value' является подстрокой значения)
     *                  - Любое другое значение - проверяется точное соответствие
     * {
     *   "items": [
     *     {
     *       "list_id": "CR",
     *       "tail_object": {
     *         "loc": "MAB",
     *         "loc_way": "CR"
     *       }
     *     }
     *   ]
     * }
     * @param timeoutExpectation Время в секундах, в течение которого будет происходить проверка;
     * @return true если событие найдено, false если нет.
     *
     * В конце теста в функции `@AfterEach - tearDown()` необходимо вызвать функцию awaitAllEventChecks, для того, чтобы тест подождал завершения всех проверок, которые запустились и выполняются асинхронно.
     */
    fun ExpectationContext.checkHasEventAsync(
        eventName: String,
        eventData: String? = null,
        timeoutEventExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
    ) {
        checkHasEventAsyncInternal(eventName, eventData, timeoutEventExpectation)
    }

    /**
     * Проверяет наличие события и данные в EventFileStorage с указанной периодичностью в течение заданного времени.
     * Проверку можно производить после действия, причем проверка будет производиться ассинхронно и не блокирует основной поток
     * Так как событие может придти позже после нескольких действий связанных между собой;
     *
     * @param eventName Название события для поиска;
     * @param eventData Перечень данных в виде `Json String` или `Json File`, которые мы хотим проверить в событии;
     *                  Поддерживает шаблоны для значений:
     *                  - "*" - соответствует любому значению (wildcard)
     *                  - "" - соответствует только пустому значению
     *                  - "~value" - проверяет частичное совпадение (если 'value' является подстрокой значения)
     *                  - Любое другое значение - проверяется точное соответствие
     * {
     *   "items": [
     *     {
     *       "list_id": "CR",
     *       "tail_object": {
     *         "loc": "MAB",
     *         "loc_way": "CR"
     *       }
     *     }
     *   ]
     * }
     * @param timeoutExpectation Время в секундах, в течение которого будет происходить проверка;
     * @return true если событие найдено, false если нет.
     *
     * В конце теста в функции `@AfterEach - tearDown()` необходимо вызвать функцию awaitAllEventChecks, для того, чтобы тест подождал завершения всех проверок, которые запустились и выполняются асинхронно.
     */
    fun ExpectationContext.checkHasEventAsync(
        eventName: String,
        eventData: File?,
        timeoutEventExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
    ) {
        val jsonData = eventData?.readText()
        checkHasEventAsyncInternal(eventName, jsonData, timeoutEventExpectation)
    }

    /**
     * Асинхронная проверка наличия события в EventStorage.
     *
     * Проверка выполняется в фоне без блокировки основного потока теста.
     * Позволяет продолжать выполнение теста, пока происходит ожидание события в EventStorage.
     *
     * @param eventName Название события для поиска.
     * @param eventData Строка JSON с данными, которые должны присутствовать в событии (может быть null для проверки только по имени).
     *                  Поддерживает шаблоны для значений:
     *                  - "*" - соответствует любому значению (wildcard)
     *                  - "" - соответствует только пустому значению
     *                  - "~value" - проверяет частичное совпадение (если 'value' является подстрокой значения)
     *                  - Любое другое значение - проверяется точное соответствие
     * @param timeoutExpectation Время ожидания события в секундах.
     */
    private fun ExpectationContext.checkHasEventAsyncInternal(
        eventName: String,
        eventData: String? = null,
        timeoutEventExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
    ) {
        val pollingInterval = 500L
        val timeoutInMillis = timeoutEventExpectation * 1000

        val job = scope.async {
            val initialEventCount = eventsFileStorage.getEvents().size

            if (eventData != null) {
                println("Ожидание события '$eventName' с данными '$eventData'")
            } else {
                println("Ожидание события '$eventName'...")
            }

            val result = withTimeoutOrNull(timeoutInMillis) {
                while (true) {
                    // Получаем только новые события, появившиеся после начала ожидания
                    val newEvents = eventsFileStorage.getEvents().drop(initialEventCount)

                    for (event in newEvents) {
                        // Пропускаем события, которые уже были проверены
                        if (eventsFileStorage.isEventAlreadyMatched(event.event_num)) continue

                        // Проверяем совпадение имени события
                        if (event.name == eventName) {
                            if (eventData == null) {
                                eventsFileStorage.markEventAsMatched(event.event_num)
                                return@withTimeoutOrNull true
                            }

                            // Сериализуем EventData и проверяем наличие всех требуемых данных
                            val eventDataJson = event.data?.let { Json.encodeToString(EventData.serializer(), it) }
                            if (eventDataJson != null && containsJsonData(eventDataJson, eventData)) {
                                eventsFileStorage.markEventAsMatched(event.event_num)
                                return@withTimeoutOrNull true
                            }
                        }
                    }

                    // Делаем паузу перед следующей проверкой
                    delay(pollingInterval)
                }
            } ?: false

            // Проверяем результат выполнения задачи
            try {
                if (result as Boolean) {
                    println("Ожидаемое событие '$eventName' найдено.")
                } else {
                    if (eventData != null) {
                        assert(false) { "Событие '$eventName' с данными '$eventData' не было обнаружено за $timeoutEventExpectation секунд." }
                    } else {
                        assert(false) { "Событие '$eventName' не было обнаружено за $timeoutEventExpectation секунд." }
                    }
                }
            } catch (e: ClassCastException) {
                throw Exception("Невозможно привести результат ожидания к типу Boolean: $result", e)
            }
        }

        // Добавляем задачу в список для последующего ожидания
        jobs.add(job)
    }

    /**
     * Ожидание завершения всех запущенных асинхронных проверок событий.
     *
     * Этот метод необходимо вызывать в методе `@AfterEach` после выполнения теста,
     * чтобы гарантировать, что все фоновые проверки событий завершены перед окончанием теста.
     */
    fun awaitAllEventChecks() {
        runBlocking {
            jobs.forEach { job ->
                job.await()
            }
            jobs.clear()
        }
    }

    /**
     * Получить текст из [element] найденном на экране
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     *
     * @return String
     */
    fun getText(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): String {
        return waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).text.toString()
    }

    /**
     * Получить числовое значение из [element] найденного на экране
     * Универсальный парсер числовых значений из «грязного» текста (цены/суммы):
     * нормализуются спецпробелы, валютные/текстовые хвосты, определяется десятичный
     * разделитель, удаляются разделители тысяч.
     *
     * Примеры: "1 22323", "1232", "12.2323", "стоимость 12 р".
     *
     * @return Double or null
     */
    fun getNumber(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): Double? {
        val text = waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).text.toString()
        return NumberParser.parseNumber(text)
    }

    /**
     * Получить значение [attribute] из [element] найденном на экране
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     *
     * @return String
     */
    fun getAttributeValue(
        element: PageElement?,
        elementNumber: Int? = null,
        attribute: String,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): String {
        return waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).getAttribute(attribute)
    }

    /**
     * Найти элемент на экране по его [element]
     * @param element элемент;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     *
     * @return MobileElement
     */
    fun waitForElement(
        element: PageElement?,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): MobileElement {
        // Если timeoutBeforeExpectation > 0, ожидаем, пока страница не будет в стабильном состоянии
        if (timeoutBeforeExpectation > 0) {
            val beforeWait = WebDriverWait(driver, timeoutBeforeExpectation, pollingInterval)
            try {
                // Ожидаем, пока UI приложения станет стабильным, проверяя, перестал ли меняться исходный код страницы
                var previousPageSource = driver.pageSource
                beforeWait.until { d ->
                    try {
                        val currentPageSource = d.pageSource
                        val isStable = currentPageSource == previousPageSource
                        previousPageSource = currentPageSource

                        // Если исходный код страницы не изменился, считаем UI стабильным
                        // Но продолжаем проверку до истечения таймаута, чтобы убедиться, что он действительно стабилен
                        isStable
                    } catch (e: Exception) {
                        // Если возникла ошибка при получении исходного кода страницы, предполагаем, что UI стабилен
                        logger.debug("Ошибка при проверке стабильности страницы: ${e.message}")
                        true
                    }
                }
                logger.debug("UI выглядит стабильным после ожидания")
            } catch (e: Exception) {
                // Если ожидание не удалось, логируем ошибку, но продолжаем поиск элемента
                logger.warn("Ошибка при ожидании стабильности UI: ${e.message}")
            }
        }

        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        val pageElement = element?.get()
        var currentScroll = 0
        while (true) {
            try {
                return wait.until(ExpectedConditions.visibilityOfElementLocated(pageElement as By)) as MobileElement
            } catch (e: Exception) {
                // Если элемент не найден и передано scrollCount > 0
                if (scrollCount > 0 && currentScroll < scrollCount) {
                    performScroll(
                        scrollCount = 1,
                        scrollCapacity = scrollCapacity,
                        scrollDirection = scrollDirection
                    )
                    currentScroll++
                } else {
                    throw NoSuchElementException("Элемент '$pageElement' не найден за '$timeoutExpectation' секунд после '$currentScroll' скроллирований")
                }
            }
        }
    }

    /**
     * Найти все элементы на экране по его [element] вернуть конкретный из списка по его номеру
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     *
     * @return MobileElement
     */
    private fun waitForElements(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): MobileElement {
        // Если timeoutBeforeExpectation > 0, ожидаем, пока страница не будет в стабильном состоянии
        if (timeoutBeforeExpectation > 0) {
            val beforeWait = WebDriverWait(driver, timeoutBeforeExpectation, pollingInterval)
            try {
                // Ожидаем, пока UI приложения станет стабильным, проверяя, перестал ли меняться исходный код страницы
                var previousPageSource = driver.pageSource
                beforeWait.until { d ->
                    try {
                        val currentPageSource = d.pageSource
                        val isStable = currentPageSource == previousPageSource
                        previousPageSource = currentPageSource

                        // Если исходный код страницы не изменился, считаем UI стабильным
                        // Но продолжаем проверку до истечения таймаута, чтобы убедиться, что он действительно стабилен
                        isStable
                    } catch (e: Exception) {
                        // Если возникла ошибка при получении исходного кода страницы, предполагаем, что UI стабилен
                        logger.debug("Ошибка при проверке стабильности страницы: ${e.message}")
                        true
                    }
                }
                logger.debug("UI выглядит стабильным после ожидания")
            } catch (e: Exception) {
                // Если ожидание не удалось, логируем ошибку, но продолжаем поиск элемента
                logger.warn("Ошибка при ожидании стабильности UI: ${e.message}")
            }
        }

        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        var currentScroll = 0

        while (true) {
            // Получаем все локаторы для текущей платформы
            val locators = element?.getAll() ?: listOf(element?.get())
            var lastException: Exception? = null
            val attemptedLocators = mutableListOf<Any>()
            val failedLocators = mutableListOf<Any>()

            // Перебираем все локаторы
            for (locator in locators.filterNotNull()) {
                try {
                    val pageElement = locator
                    attemptedLocators.add(pageElement)
                    val elements =
                        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(pageElement as By)) as List<MobileElement>

                    val safeIndex = elementNumber ?: 1
                    if (safeIndex < 1 || safeIndex > elements.size) {
                        throw IndexOutOfBoundsException("Элемент $elementNumber вне допустимого диапазона")
                    }

                    // Логируем информацию о неудачных попытках, даже если текущий локатор успешен
                    if (failedLocators.isNotEmpty()) {
                        logger.info("Следующие локаторы ${failedLocators.joinToString(", ")} из списка ${attemptedLocators} были не найдены.")
                    }

                    return elements[safeIndex - 1]
                } catch (e: Exception) {
                    // Сохраняем последнее исключение
                    lastException = e
                    // Добавляем локатор в список неудачных
                    failedLocators.add(locator)
                    // Продолжаем перебор локаторов
                    continue
                }
            }

            // Если ни один локатор не сработал и передано scrollCount > 0
            if (scrollCount > 0 && currentScroll < scrollCount) {
                performScroll(
                    scrollCount = 1,
                    scrollCapacity = scrollCapacity,
                    scrollDirection = scrollDirection
                )
                currentScroll++
            } else {
                // Используем информацию о последнем исключении и используемых локаторах для более детального сообщения об ошибке
                val locatorsInfo = if (failedLocators.isNotEmpty()) {
                    "Следующие локаторы ${failedLocators.joinToString(", ")} из списка ${attemptedLocators} были не найдены."
                } else if (attemptedLocators.isNotEmpty()) {
                    "Попытались найти следующие элементы: ${attemptedLocators.joinToString(", ")}"
                } else {
                    "Не было найдено ни одного элемента"
                }

                val errorMessage = if (lastException != null) {
                    "Элементы не найдены за '$timeoutExpectation' секунд после '$currentScroll' скроллирований. $locatorsInfo. Причина: ${lastException.message}"
                } else {
                    "Элементы не найдены за '$timeoutExpectation' секунд после '$currentScroll' скроллирований. $locatorsInfo"
                }
                throw NoSuchElementException(errorMessage)
            }
        }
    }

    /**
     * Найти элемент на экране по его [element] либо вернуть null
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, в течение которых ожидается стабилизация UI (отсутствие изменений в исходном коде страницы) перед началом поиска элемента;
     * @param timeoutExpectation количество секунд в течение которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллирования экрана;
     *
     * @exception java.util.NoSuchElementException элемент не найден
     *
     * @return MobileElement or null
     */
    private fun waitForElementOrNull(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): MobileElement? {
        return try {
            waitForElements(
                element = element,
                elementNumber = elementNumber,
                timeoutBeforeExpectation = timeoutBeforeExpectation,
                timeoutExpectation = timeoutExpectation,
                pollingInterval = pollingInterval,
                scrollCount = scrollCount,
                scrollCapacity = scrollCapacity,
                scrollDirection = scrollDirection
            )
        } catch (e: NoSuchElementException) {
            null
        }
    }

    /**
     * Выполнить скроллирование вниз
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     */
    fun StepContext.scrollDown(
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element = null, scrollCount, scrollCapacity, ScrollDirection.Down)

    /**
     * Выполнить скроллирование вверх
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     */
    fun StepContext.scrollUp(
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element = null, scrollCount, scrollCapacity, ScrollDirection.Up)

    /**
     * Выполнить скроллирование вправо
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     */
    fun StepContext.scrollRight(
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element = null, scrollCount, scrollCapacity, ScrollDirection.Right)

    /**
     * Выполнить скроллирование влево
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     */
    fun StepContext.scrollLeft(
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element = null, scrollCount, scrollCapacity, ScrollDirection.Left)

    /**
     * Выполнить свайп вниз
     * @param element элемент;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     */
    fun StepContext.swipeDown(
        element: PageElement?,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Down)

    /**
     * Выполнить свайп вверх
     * @param element элемент;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     */
    fun StepContext.swipeUp(
        element: PageElement?,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Up)

    /**
     * Выполнить свайп вправо
     * @param element элемент;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     */
    fun StepContext.swipeRight(
        element: PageElement?,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Right)

    /**
     * Выполнить свайп влево
     * @param element элемент;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     */
    fun StepContext.swipeLeft(
        element: PageElement?,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Left)

    /**
     * Функция выполняющее скроллирование и свайп по направлениям
     */
    private fun performScroll(
        element: PageElement? = null,
        scrollCount: Int,
        scrollCapacity: Double,
        scrollDirection: ScrollDirection
    ) {
        assert(scrollCapacity > 0.0 && scrollCapacity <= 1.0) {
            "scrollCapacity имеет значение $scrollCapacity, но может принимать значения от 0.0 до 1.0"
        }
        if (element != null) {
            val el = waitForElements(element)
            val elLocation = el.location
            val size = el.size
            repeat(scrollCount) {
                when (scrollDirection) {
                    ScrollDirection.Right, ScrollDirection.Left -> {
                        val isScrollRight = scrollDirection is ScrollDirection.Right
                        val width = size.width * scrollCapacity
                        val centerY = el.center.y
                        val startX = if (isScrollRight) {
                            elLocation.x + (width * DEFAULT_SWIPE_COEFFICIENT)
                        } else {
                            elLocation.x + (width * (1 - DEFAULT_SWIPE_COEFFICIENT))
                        }.toInt()
                        val endX = if (isScrollRight) {
                            elLocation.x + (width * (1 - DEFAULT_SWIPE_COEFFICIENT))
                        } else {
                            elLocation.x + (width * DEFAULT_SWIPE_COEFFICIENT)
                        }.toInt()
                        touchAndMoveHorizontal(centerY, startX, endX)
                    }

                    ScrollDirection.Down, ScrollDirection.Up -> {
                        val isScrollDown = scrollDirection is ScrollDirection.Down
                        val height = size.height * scrollCapacity
                        val centerX = el.center.x
                        val startY = if (isScrollDown) {
                            elLocation.y + (height * DEFAULT_SWIPE_COEFFICIENT)
                        } else {
                            elLocation.y + (height * (1 - DEFAULT_SWIPE_COEFFICIENT))
                        }.toInt()
                        val endY = if (isScrollDown) {
                            elLocation.y + (height * (1 - DEFAULT_SWIPE_COEFFICIENT))
                        } else {
                            elLocation.y + (height * DEFAULT_SWIPE_COEFFICIENT)
                        }.toInt()
                        touchAndMoveVertical(centerX, startY, endY)
                    }
                }
            }
        } else {
            repeat(scrollCount) {
                when (scrollDirection) {
                    ScrollDirection.Right, ScrollDirection.Left -> {
                        val isScrollRight = scrollDirection is ScrollDirection.Right
                        val size = driver.manage().window().size
                        val width = size.width * scrollCapacity
                        val centerY = size.height / 2
                        val startX = if (isScrollRight) {
                            width * DEFAULT_SCROLL_COEFFICIENT
                        } else {
                            width * (1 - DEFAULT_SCROLL_COEFFICIENT)
                        }.toInt()
                        val endX = (if (isScrollRight) 0 else width).toInt()
                        touchAndMoveHorizontal(centerY, startX, endX)
                    }

                    ScrollDirection.Down, ScrollDirection.Up -> {
                        val isScrollDown = scrollDirection is ScrollDirection.Down
                        val size = driver.manage().window().size
                        val height = size.height * scrollCapacity
                        val centerX = size.width / 2
                        val startY = if (isScrollDown) {
                            height * DEFAULT_SCROLL_COEFFICIENT
                        } else {
                            height * (1 - DEFAULT_SCROLL_COEFFICIENT)
                        }.toInt()
                        val endY = (if (isScrollDown) 0 else height).toInt()
                        touchAndMoveVertical(centerX, startY, endY)
                    }
                }
            }
        }
    }

    /**
     * Функция для скроллирования экрана по вертикали
     */
    private fun touchAndMoveVertical(center: Int, start: Int, end: Int) {
        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger1")
        val swipe = Sequence(finger, 0)
        val movement = Duration.ofMillis(500)
        val hold = Duration.ofMillis(500)

        swipe.addAction(
            finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                center,
                start
            )
        )
        swipe.addAction(finger.createPointerDown(0))
        swipe.addAction(Pause(finger, hold))
        swipe.addAction(
            finger.createPointerMove(
                movement,
                PointerInput.Origin.viewport(),
                center,
                end
            )
        )
        swipe.addAction(finger.createPointerUp(0))

        driver.perform(ImmutableList.of(swipe))
    }

    /**
     * Функция для скроллирования экрана по горизонтали
     */
    private fun touchAndMoveHorizontal(center: Int, start: Int, end: Int) {
        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger1")
        val swipe = Sequence(finger, 0)
        val movement = Duration.ofMillis(500)
        val hold = Duration.ofMillis(500)

        swipe.addAction(
            finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                start,
                center
            )
        )
        swipe.addAction(finger.createPointerDown(0))
        swipe.addAction(Pause(finger, hold))
        swipe.addAction(
            finger.createPointerMove(
                movement,
                PointerInput.Origin.viewport(),
                end,
                center
            )
        )
        swipe.addAction(finger.createPointerUp(0))

        driver.perform(ImmutableList.of(swipe))
    }

    /**
     * Открыть диплинк на мобильном устройстве в зависимости от платформы.
     *
     * @param deeplink Строка диплинка, который необходимо открыть.
     *
     * Для Android:
     *  - Используется Mobile Command `mobile:deepLink` через Appium.
     *
     * Для iOS:
     *  - Открытие диплинка происходит через симулятор с помощью `xcrun simctl openurl`,
     *  - Вспомогательная страница запускается через локальный web-сервер.
     *
     * @throws IllegalArgumentException если платформа не поддерживается.
     */
    fun StepContext.openDeeplink(deeplink: String) {
        when (AppConfig.getPlatform()) {
            // Для Android вызываем команду мобильного диплинка через Appium
            Platform.ANDROID -> driver.executeScript(
                "mobile:deepLink",
                mapOf(
                    "url" to deeplink,
                    "package" to AppConfig.getAppPackage()
                )
            )

            // Для iOS: сначала пробуем стандартный способ через Appium, затем (при ошибке или отсутствии bundleId) эмулируем через симулятор
            Platform.IOS -> {
                val bundleId = AppConfig.getBundleId().trim()
                if (bundleId.isNotEmpty()) {
                    try {
                        driver.executeScript(
                            "mobile:deepLink",
                            mapOf(
                                "url" to deeplink,
                                "bundleId" to bundleId
                            )
                        )
                        return
                    } catch (e: Exception) {
                        logger.warn("iOS deepLink via Appium failed, falling back to simulator: ${e.message}")
                    }
                }

                val encodedUrl: String = URLEncoder.encode(deeplink, StandardCharsets.UTF_8)
                val listCommand = listOf(
                    "xcrun",
                    "simctl",
                    "openurl",
                    getSimulatorId(AppConfig.getIosDeviceName()).toString(),
                    app.webServer.getHostingUrl() + "src/main/resources/deeplink.html?url=" + encodedUrl
                )

                // Выполняем команду открытия URL через симулятор
                runCommand(listCommand, "Нет возможность открыть deeplink")

                // Ждём появления элемента "deeplink" на экране симулятора и нажимаем по нему
                waitForElement(
                    PageElement(
                        android = null,
                        ios = By.id("deeplink")
                    ), timeoutExpectation = 15
                ).click()
            }

            else -> throw IllegalArgumentException("Неподдерживаемая платформа")
        }
    }

    /**
     * DSL-метод для работы с системными алертами из контекста шага.
     *
     * @param accept если true — нажать «Accept» (accept), иначе — «Cancel» (dismiss).
     * @param timeoutExpectation сколько секунд ждать появления алерта.
     * @param pollingInterval Частота опроса элемента в миллисекундах.
     *
     * Пример:
     * ```
     * "Обработка системного алерта" {
     *     alert(accept = true)
     * }
     * ```
     */
    fun StepContext.alert(
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL
    ): AlertHandler {
        return AlertHandler(driver, timeoutExpectation, pollingInterval)
    }

    /**
     * Универсальный метод для отправки нативных команд (например, нажатий клавиш) на мобильное устройство.
     *
     * Используйте этот метод для передачи платформенно-зависимых команд в рамках тестовых сценариев.
     *
     * @param androidKey Клавиша Android (например, AndroidKey.BACK, AndroidKey.ENTER).
     *                   Передаётся для платформы Android, для остальных платформ параметр игнорируется.
     * @param iosKey Строка-код клавиши для iOS (например, "\n" для Enter).
     *               Передаётся для iOS, для остальных платформ параметр игнорируется.
     *
     * @throws IllegalArgumentException если не передан необходимый параметр для указанной платформы.
     * @throws UnsupportedOperationException если метод вызван для неподдерживаемой платформы (например, Web).
     */
    fun StepContext.performNativeAction(
        androidKey: AndroidKey? = null,
        iosKey: String? = null
    ) {
        when (AppConfig.getPlatform()) {
            Platform.ANDROID -> {
                val androidDriver = driver as? AndroidDriver ?: error("Driver не AndroidDriver")
                if (androidKey != null) {
                    androidDriver.pressKey(KeyEvent(androidKey))
                } else {
                    throw IllegalArgumentException("Необходимо передать параметр androidKey для Android-платформы")
                }
            }

            Platform.IOS -> {
                val iosDriver = driver as? IOSDriver ?: error("Driver не IOSDriver")
                if (iosKey != null) {
                    iosDriver.keyboard.pressKey(iosKey)
                } else {
                    throw IllegalArgumentException("Необходимо передать параметр iosKey для iOS-платформы")
                }
            }
        }
    }

    /**
     * Выполняет нативное нажатие клавиши Enter (или Return) на мобильном устройстве.
     *
     * Использует платформо-зависимые ключи для Android и iOS:
     * - Android: AndroidKey.ENTER
     * - iOS: "\n"
     *
     * @throws IllegalArgumentException если платформа не поддерживается или драйвер не инициализирован.
     */
    fun StepContext.tapEnter() {
        performNativeAction(
            androidKey = AndroidKey.ENTER,
            iosKey = "\n"
        )
    }

    // Функционал, выполняемый перед каждым тестом
    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        // Очистка логов перед началом теста
        LogCapture.clearLogs()
        // Инициализация системы захвата логов
        LogCapture.initialize()
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
        LogCapture.attachLogsToAllureReport()

        // Закрытие приложения
        closeApp()
    }

    /**
     * Закрывает приложение.
     * Этот метод может быть вызван из внешних классов, например, из SkipConditionExtension,
     * когда тест пропускается из-за аннотации @Skip.
     */
    fun closeApp() {
        app.close()
    }
}
