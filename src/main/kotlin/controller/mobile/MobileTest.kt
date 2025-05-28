package controller.mobile

import app.App
import app.config.AppConfig
import app.model.Platform
import com.google.common.collect.ImmutableList
import controller.element.PageElement
import controller.element.ScrollDirection
import controller.handler.AlertHandler
import dsl.testing.ExpectationContext
import dsl.testing.StepContext
import dsl.testing.TestingContext
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
import org.junit.jupiter.api.AfterEach
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
import utils.TerminalUtils
import utils.TerminalUtils.runCommand
import utils.VideoRecorder
import org.junit.jupiter.api.TestInfo
import utils.DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
import utils.AnrWatcher
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
open class MobileTest {
    private val logger: Logger = LoggerFactory.getLogger(MobileTest::class.java)
    protected var app: App = App().launch()
    protected var context: TestingContext = TestingContext(driver)
    protected val driver: AppiumDriver<MobileElement>
        get() = app.driver ?: throw IllegalStateException("Driver is not initialized")
    private val eventsFileStorage = EventStorage

    // Create your own CoroutineScope for coroutine lifecycle management
    private val scope = CoroutineScope(Dispatchers.Default)
    private val jobs = mutableListOf<Deferred<*>>()

    /**
     * Найти элемент на экране по его [element] и кликнуть по нему
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
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
     * Расширение для StepContext: клик по первому item из события,
     * у которого где-нибудь в data встречаются все пары из eventData.
     */
    fun StepContext.click(
        eventName: String,
        eventData: String,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = 3,
        scrollCapacity: Double = 0.7,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        var matchedEvent: Event? = null

        for (attempt in 1..scrollCount) {
            "Ждём событие $eventName (попытка $attempt)" {
                runCatching {
                    checkHasEvent(eventName, eventData, timeoutExpectation)
                }.onFailure {
                    if (attempt < scrollCount) {
                        logger.warn("Попытка $attempt неуспешна: событие не найдено")
                    } else {
                        logger.error("После всех попыток событие '$eventName' с фильтром '$eventData' не найдено")
                    }
                }
            }

            matchedEvent = EventStorage.getEvents()
                .firstOrNull {
                    it.name == eventName &&
                            it.data?.let { d ->
                                val json = Json.encodeToString(EventData.serializer(), d)
                                containsJsonData(json, eventData)
                            } ?: false
                }

            if (matchedEvent != null) {
                break
            }

            if (attempt < scrollCount) {
                logger.info("Выполняем скролл")
                performScroll(
                    element = null,
                    scrollCount = 1,
                    scrollCapacity = scrollCapacity,
                    scrollDirection = scrollDirection
                )
            }
        }

        val ev = matchedEvent
            ?: throw NoSuchElementException("Событие '$eventName' с данными '$eventData' не найдено после $scrollCount скроллов")

        // Извлечь массив items из body → event → data
        val bodyObj = Json.parseToJsonElement(ev.data!!.body).jsonObject
        val itemsArr = bodyObj["event"]!!.jsonObject["data"]!!.jsonObject["items"]!!.jsonArray

        // Найти первый item, содержащий искомые пары
        val searchObj = Json.parseToJsonElement(eventData).jsonObject
        val matched = itemsArr.firstOrNull { itemElem ->
            searchObj.all { (key, sv) ->
                findKeyValueInTree(itemElem, key, sv)
            }
        }
        if (matched == null) {
            throw NoSuchElementException("В событии '$eventName' ни один item не соответствует $eventData")
        }

        // Достать у найденного item его поле "name"
        val itemName = matched.jsonObject["name"]!!.jsonPrimitive.content
        logger.info("Найден подходящий товар: '$itemName' по фильтрам (eventName=$eventName, filter=$eventData)")

        // Построить локатор и выполнить обычный click с ожиданиями и скроллом
        val locator = PageElement(
            android = PageElement.Text(itemName),
            ios = PageElement.Label(itemName),
            web = null
        )

        click(
            element = locator,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        )
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
     * @param timeoutBeforeExpectation задержка перед началом поиска, в секундах;
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
     * Нажать в области экрана по [x] [y]
     * @param x точка по [x] в области экрана;
     * @param y точка по [y] в области экрана;
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     */
    fun StepContext.tapArea(
        x: Int,
        y: Int,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION
    ) {
        Thread.sleep(timeoutBeforeExpectation * 1_000)
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
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
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
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
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
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
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
     * @param timeoutBeforeExpectation задержка перед началом ожидания;
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
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
    ) {
        // Вызов основной функции с уже подготовленной строкой JSON
        checkHasEventInternal(eventName, eventData, timeoutExpectation)
    }

    /**
     * Проверяет наличие события и его данные в EventFileStorage с указанной периодичностью в течение заданного времени.
     *
     * @param eventName Название события для поиска;
     * @param eventData Перечень данных в виде `Json String` или `Json File`, которые мы хотим проверить в событии;
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
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
    ) {
        val jsonData = eventData?.readText()
        checkHasEventInternal(eventName, jsonData, timeoutExpectation)
    }

    /**
     * Внутренняя функция ожидания события в EventStorage в течение заданного времени.
     *
     * @param eventName Название события, которое ожидается.
     * @param eventData Ожидаемое содержимое события в формате JSON (опционально).
     * @param timeoutExpectation Таймаут ожидания события в секундах.
     *
     * @throws Exception если событие не найдено за отведённое время.
     */
    private fun ExpectationContext.checkHasEventInternal(
        eventName: String,
        eventData: String? = null,
        timeoutExpectation: Long
    ) {
        val pollingInterval = 500L
        val timeoutInMillis = timeoutExpectation * 1000

        if (eventData != null) {
            println("Ожидание события '$eventName' с данными '$eventData'")
        } else {
            println("Ожидание события '$eventName'...")
        }

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
                        assert(false) { "Ожидаемое событие '$eventName' с данными '$eventData' не обнаружено за $timeoutExpectation секунд." }
                    } else {
                        assert(false) { "Ожидаемое событие '$eventName' не обнаружено за $timeoutExpectation секунд." }
                    }
                }
            } catch (e: ClassCastException) {
                throw Exception("Ошибка приведения результата проверки к Boolean: $result", e)
            }
        }
    }

    /**
     * Проверяет, содержатся ли все ключи и значения из искомого JSON внутри JSON события.
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
     * - Примитивов (по принципу contains),
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
            // Сравнение примитивов: проверка на вхождение строки
            eventElement is JsonPrimitive && searchElement is JsonPrimitive ->
                eventElement.content.contains(searchElement.content)

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
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION
    ) {
        checkHasEventAsyncInternal(eventName, eventData, timeoutExpectation)
    }

    /**
     * Проверяет наличие события и данные в EventFileStorage с указанной периодичностью в течение заданного времени.
     * Проверку можно производить после действия, причем проверка будет производиться ассинхронно и не блокирует основной поток
     * Так как событие может придти позже после нескольких действий связанных между собой;
     *
     * @param eventName Название события для поиска;
     * @param eventData Перечень данных в виде `Json String` или `Json File`, которые мы хотим проверить в событии;
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
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION
    ) {
        val jsonData = eventData?.readText()
        checkHasEventAsyncInternal(eventName, jsonData, timeoutExpectation)
    }

    /**
     * Асинхронная проверка наличия события в EventStorage.
     *
     * Проверка выполняется в фоне без блокировки основного потока теста.
     * Позволяет продолжать выполнение теста, пока происходит ожидание события в EventStorage.
     *
     * @param eventName Название события для поиска.
     * @param eventData Строка JSON с данными, которые должны присутствовать в событии (может быть null для проверки только по имени).
     * @param timeoutExpectation Время ожидания события в секундах.
     */
    private fun ExpectationContext.checkHasEventAsyncInternal(
        eventName: String,
        eventData: String? = null,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION
    ) {
        val pollingInterval = 500L
        val timeoutInMillis = timeoutExpectation * 1000

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
                        assert(false) { "Событие '$eventName' с данными '$eventData' не было обнаружено за $timeoutExpectation секунд." }
                    } else {
                        assert(false) { "Событие '$eventName' не было обнаружено за $timeoutExpectation секунд." }
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
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
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
     * Получить цену из [element] найденном на экране
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
     *
     * @exception java.util.NoSuchElementException если возвращаемое значение будет null
     *
     * @return Int or null
     */
    fun getPrice(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): Int? {
        return waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).text.toString().filter { it.isDigit() }.toIntOrNull()
    }

    /**
     * Получить значение [attribute] из [element] найденном на экране
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
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
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
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
        Thread.sleep(timeoutBeforeExpectation * 1_000)
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
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
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
        Thread.sleep(timeoutBeforeExpectation * 1_000)
        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        val pageElement = element?.get()
        var currentScroll = 0
        while (true) {
            try {
                val elements =
                    wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(pageElement as By)) as List<MobileElement>

                val safeIndex = elementNumber ?: 1
                if (safeIndex < 1 || safeIndex > elements.size) {
                    throw IndexOutOfBoundsException("Элемент $elementNumber вне допустимого диапазона")
                }

                return elements[safeIndex - 1]
            } catch (e: Exception) {
                // Если элементы не найдены и передано scrollCount > 0
                if (scrollCount > 0 && currentScroll < scrollCount) {
                    performScroll(
                        scrollCount = 1,
                        scrollCapacity = scrollCapacity,
                        scrollDirection = scrollDirection
                    )
                    currentScroll++
                } else {
                    throw NoSuchElementException("Элементы '$pageElement' не найдены за '$timeoutExpectation' секунд после '$currentScroll' скроллирований")
                }
            }
        }
    }

    /**
     * Найти элемент на экране по его [element] либо вернуть null
     * @param element элемент;
     * @param elementNumber номер найденного элемента начиная с 1;
     * @param timeoutBeforeExpectation количество секунд, до того как будет производиться поиск элемента;
     * @param timeoutExpectation количество секунд в течении которого производится поиск элемента;
     * @param pollingInterval частота опроса элемента в миллисекундах;
     * @param scrollCount количество скроллирований до элемента, если элемент не найден на текущей странице;
     * @param scrollCapacity модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
     * @param scrollDirection направление скроллировая экрана;
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

            // Для iOS эмулируем открытие диплинка через симулятор
            Platform.IOS -> {
                val encodedUrl: String = URLEncoder.encode(deeplink, StandardCharsets.UTF_8)
                val listCommand = listOf(
                    "xcrun",
                    "simctl",
                    "openurl",
                    TerminalUtils.getSimulatorId(AppConfig.getIosDeviceName()).toString(),
                    app.webServer.getHostingUrl() + "src/main/resources/deeplink.html?url=" + encodedUrl
                )

                // Выполняем команду открытия URL через симулятор
                runCommand(listCommand, "Нет возможность открыть deeplink")

                // Ждём появления элемента "deeplink" на экране симулятора и нажимаем по нему
                waitForElement(
                    PageElement(
                        android = null,
                        ios = By.id("deeplink"),
                        web = null
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

            Platform.WEB -> {
                throw UnsupportedOperationException("Нативные действия не поддерживаются на платформе WEB")
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
        app.close()
    }
}
