package controller.mobile.events

import kotlinx.serialization.json.*

internal object JsonMatchers {

    /**
     * Checks whether all keys and values from the search JSON are contained within the event JSON.
     *
     * Supports value patterns:
     * - "*" - matches any value (wildcard)
     * - "" - matches only an empty value
     * - "~value" - checks partial match (if 'value' is a substring of the actual value)
     * - Any other value - exact equality is required
     *
     * @param eventJson JSON string of the event (serialized EventData).
     * @param searchJson JSON string with the keys and values to search for.
     * @return true if all keys/values from searchJson are found in eventJson, otherwise false.
     */
    fun containsJsonData(eventJson: String, searchJson: String): Boolean {
        // Extract the "body" field from eventJson and parse it into a JSON object
        val evDataObj = Json.parseToJsonElement(eventJson).jsonObject
        val bodyStr = evDataObj["body"]!!.jsonPrimitive.content
        val bodyObj = Json.parseToJsonElement(bodyStr).jsonObject

        // Navigate to node "event" → "data"
        val dataElement = bodyObj["event"]!!.jsonObject["data"]!!

        // Parse search keys and values
        val searchObj = Json.parseToJsonElement(searchJson).jsonObject

        // For each (key, value) pair, check for a corresponding match in the dataElement tree
        return searchObj.all { (key, sv) ->
            findKeyValueInTree(dataElement, key, sv)
        }
    }

    /**
     * Recursively traverses a JsonElement and returns true
     * if a record “key = key” is found anywhere and its value matches matchJsonElement(…, searchValue).
     */
    fun findKeyValueInTree(
        element: JsonElement,
        key: String,
        searchValue: JsonElement
    ): Boolean = when (element) {
        is JsonObject -> element.entries.any { (k, v) ->
            // either a match by key+value here, or go deeper
            (k == key && matchJsonElement(v, searchValue))
                    || findKeyValueInTree(v, key, searchValue)
        }

        is JsonArray -> element.any { findKeyValueInTree(it, key, searchValue) }
        else -> false
    }

    /**
     * Recursive function to match JSON elements.
     *
     * Matching supports:
     * - Primitives with pattern support:
     *   - "*" - matches any value (wildcard)
     *   - "" - matches only an empty value
     *   - "~value" - checks partial match (if 'value' is a substring of the value)
     *   - Any other value - exact equality is required
     * - Objects (by keys and values),
     * - Arrays (each element in the search array must be found in the event array),
     * - Strings containing nested serialized JSON objects.
     *
     * @param eventElement JSON element obtained from the event.
     * @param searchElement JSON element that must be found within the event.
     * @return true if elements match by structure and content, otherwise false.
     */
    fun matchJsonElement(eventElement: JsonElement, searchElement: JsonElement): Boolean {
        return when {
            // Primitive comparison with pattern support
            eventElement is JsonPrimitive && searchElement is JsonPrimitive -> {
                when {
                    searchElement.content == "*" -> true // Wildcard — matches any value
                    searchElement.content == "" -> eventElement.content.isEmpty() // Empty string — matches only an empty value
                    searchElement.content.startsWith("~") -> eventElement.content.contains(
                        searchElement.content.substring(
                            1
                        )
                    ) // Partial match
                    else -> eventElement.content == searchElement.content // Exact match
                }
            }

            // Comparing nested JSON strings: parse the string and compare recursively
            eventElement is JsonPrimitive && eventElement.isString -> runCatching {
                matchJsonElement(
                    Json.parseToJsonElement(eventElement.content),
                    searchElement
                )
            }.getOrDefault(false)

            // Object comparison: every key and value must match
            eventElement is JsonObject && searchElement is JsonObject ->
                searchElement.all { (k, sv) ->
                    eventElement[k]?.let { matchJsonElement(it, sv) } ?: false
                }

            // Array comparison: every element from the search array must be found in the event array
            eventElement is JsonArray && searchElement is JsonArray ->
                searchElement.all { se ->
                    eventElement.any { ee -> matchJsonElement(ee, se) }
                }

            // Other cases: elements do not match
            else -> false
        }
    }
}
