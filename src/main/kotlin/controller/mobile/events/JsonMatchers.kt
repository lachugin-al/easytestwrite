package controller.mobile.events

import kotlinx.serialization.json.*

internal object JsonMatchers {

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
    fun containsJsonData(eventJson: String, searchJson: String): Boolean {
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
    fun findKeyValueInTree(
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
    fun matchJsonElement(eventElement: JsonElement, searchElement: JsonElement): Boolean {
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

}