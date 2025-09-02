package controller.mobile.events

import controller.mobile.element.PageElement
import controller.mobile.interaction.ScrollDirection
import controller.mobile.events.JsonMatchers.containsJsonData
import controller.mobile.events.JsonMatchers.findKeyValueInTree
import controller.mobile.interaction.UiElementFinding
import controller.mobile.interaction.UiScrollGestures
import dsl.testing.ExpectationContext
import dsl.testing.StepContext
import events.EventData
import events.EventStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2

interface EventVerifier {
    val eventsFileStorage: EventStorage
    val scope: CoroutineScope
    val jobs: MutableList<Deferred<*>>

    // Публичные DSL-обёртки
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


}

/**
 * DSL: построение PageElement по item из события и автоскролл до совпадения.
 * Опирается на EventVerifier + UiElementWaiting + UiScrollGestures.
 */
interface EventDrivenUi : EventVerifier, UiElementFinding, UiScrollGestures {
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
}
