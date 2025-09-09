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

    // Public DSL wrappers
    /**
     * Verifies the presence of an event and its data in EventFileStorage with a given polling interval
     * during the specified time.
     *
     * @param eventName The name of the event to search for;
     * @param eventData A list of data provided as `Json String` or `Json File` that we want to verify in the event;
     *                  Supports value patterns:
     *                  - "*" - matches any value (wildcard)
     *                  - "" - matches only an empty value
     *                  - "~value" - checks partial match (if 'value' is a substring of the value)
     *                  - Any other value - checks exact equality
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
     * @param timeoutExpectation Time in seconds during which the verification will be performed;
     * @return true if the event is found, false otherwise.
     */
    fun ExpectationContext.checkHasEvent(
        eventName: String,
        eventData: String? = null,
        timeoutEventExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
    ) {
        // Call the core function with the prepared JSON string
        checkHasEventInternal(eventName, eventData, timeoutEventExpectation)
    }

    /**
     * Verifies the presence of an event and its data in EventFileStorage with a given polling interval
     * during the specified time.
     *
     * @param eventName The name of the event to search for;
     * @param eventData A list of data provided as `Json String` or `Json File` that we want to verify in the event;
     *                  Supports value patterns:
     *                  - "*" - matches any value (wildcard)
     *                  - "" - matches only an empty value
     *                  - "~value" - checks partial match (if 'value' is a substring of the value)
     *                  - Any other value - checks exact equality
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
     * @param timeoutExpectation Time in seconds during which the verification will be performed;
     * @return true if the event is found, false otherwise.
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
     * Internal function that waits for an event in EventStorage for the specified time.
     *
     * @param eventName The name of the event expected.
     * @param eventData The expected event payload in JSON format (optional).
     *                  Supports value patterns:
     *                  - "*" - matches any value (wildcard)
     *                  - "" - matches only an empty value
     *                  - "~value" - checks partial match (if 'value' is a substring of the value)
     *                  - Any other value - checks exact equality
     * @param timeoutExpectation Timeout for waiting for the event in seconds.
     *
     * @throws Exception if the event is not found within the allotted time.
     */
    private fun ExpectationContext.checkHasEventInternal(
        eventName: String,
        eventData: String? = null,
        timeoutEventExpectation: Long
    ) {
        val pollingInterval = 500L
        val timeoutInMillis = timeoutEventExpectation * 1000

        if (eventData != null) {
            println("Waiting for event '$eventName' with data '$eventData'")
        } else {
            println("Waiting for event '$eventName'...")
        }

        runCatching {
            runBlocking {
                val result = withTimeoutOrNull(timeoutInMillis) {
                    while (true) {
                        // Get all events from EventStorage
                        val allEvents = eventsFileStorage.getEvents()

                        // Iterate over events
                        for (event in allEvents) {
                            // Skip already matched events
                            if (eventsFileStorage.isEventAlreadyMatched(event.event_num)) continue

                            // Check event name match
                            if (event.name == eventName) {
                                if (eventData == null) {
                                    // If no additional data required, mark the event as matched
                                    eventsFileStorage.markEventAsMatched(event.event_num)
                                    return@withTimeoutOrNull true
                                }

                                // If specific data is expected, serialize event.data to JSON
                                val eventDataJson = event.data?.let { Json.encodeToString(EventData.serializer(), it) }

                                // Check whether the event contains the expected data
                                if (eventDataJson != null && containsJsonData(eventDataJson, eventData)) {
                                    eventsFileStorage.markEventAsMatched(event.event_num)
                                    return@withTimeoutOrNull true
                                }
                            }
                        }

                        // Wait before the next check
                        delay(pollingInterval)
                    }
                } ?: false

                // Assert that the result is true and of type Boolean
                try {
                    if (result as Boolean) {
                        println("Expected event '$eventName' found.")
                    } else {
                        if (eventData != null) {
                            throw NoSuchElementException("Expected event '$eventName' with data '$eventData' was not found within $timeoutEventExpectation seconds.")
                        } else {
                            throw NoSuchElementException("Expected event '$eventName' was not found within $timeoutEventExpectation seconds.")
                        }
                    }
                } catch (e: ClassCastException) {
                    throw Exception("Failed to cast verification result to Boolean: $result", e)
                }
            }
        }.getOrThrow()
    }

    /**
     * Verifies the presence of an event and its data in EventFileStorage with a given polling interval
     * during the specified time. The check can be performed after an action and runs asynchronously without
     * blocking the main thread, since the event may arrive later after several related actions.
     *
     * @param eventName The name of the event to search for;
     * @param eventData A list of data provided as `Json String` or `Json File` that we want to verify in the event;
     *                  Supports value patterns:
     *                  - "*" - matches any value (wildcard)
     *                  - "" - matches only an empty value
     *                  - "~value" - checks partial match (if 'value' is a substring of the value)
     *                  - Any other value - checks exact equality
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
     * @param timeoutExpectation Time in seconds during which the verification will be performed;
     * @return true if the event is found, false otherwise.
     *
     * At the end of the test in `@AfterEach - tearDown()`, you must call `awaitAllEventChecks`
     * to ensure the test waits for all asynchronous verifications to finish.
     */
    fun ExpectationContext.checkHasEventAsync(
        eventName: String,
        eventData: String? = null,
        timeoutEventExpectation: Long = DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
    ) {
        checkHasEventAsyncInternal(eventName, eventData, timeoutEventExpectation)
    }

    /**
     * Verifies the presence of an event and its data in EventFileStorage with a given polling interval
     * during the specified time. The check can be performed after an action and runs asynchronously without
     * blocking the main thread, since the event may arrive later after several related actions.
     *
     * @param eventName The name of the event to search for;
     * @param eventData A list of data provided as `Json String` or `Json File` that we want to verify in the event;
     *                  Supports value patterns:
     *                  - "*" - matches any value (wildcard)
     *                  - "" - matches only an empty value
     *                  - "~value" - checks partial match (if 'value' is a substring of the value)
     *                  - Any other value - checks exact equality
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
     * @param timeoutExpectation Time in seconds during which the verification will be performed;
     * @return true if the event is found, false otherwise.
     *
     * At the end of the test in `@AfterEach - tearDown()`, you must call `awaitAllEventChecks`
     * to ensure the test waits for all asynchronous verifications to finish.
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
     * Asynchronous check for the presence of an event in EventStorage.
     *
     * The check runs in the background without blocking the main test thread.
     * Allows the test to continue while waiting for the event in EventStorage.
     *
     * @param eventName The name of the event to search for.
     * @param eventData JSON string with data that must be present in the event (can be null to check by name only).
     *                  Supports value patterns:
     *                  - "*" - matches any value (wildcard)
     *                  - "" - matches only an empty value
     *                  - "~value" - checks partial match (if 'value' is a substring of the value)
     *                  - Any other value - checks exact equality
     * @param timeoutExpectation Time to wait for the event in seconds.
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
                println("Waiting for event '$eventName' with data '$eventData'")
            } else {
                println("Waiting for event '$eventName'...")
            }

            val result = withTimeoutOrNull(timeoutInMillis) {
                while (true) {
                    // Get only new events that appeared after the wait started
                    val newEvents = eventsFileStorage.getEvents().drop(initialEventCount)

                    for (event in newEvents) {
                        // Skip events that have already been checked
                        if (eventsFileStorage.isEventAlreadyMatched(event.event_num)) continue

                        // Check event name
                        if (event.name == eventName) {
                            if (eventData == null) {
                                eventsFileStorage.markEventAsMatched(event.event_num)
                                return@withTimeoutOrNull true
                            }

                            // Serialize EventData and verify the presence of required data
                            val eventDataJson = event.data?.let { Json.encodeToString(EventData.serializer(), it) }
                            if (eventDataJson != null && containsJsonData(eventDataJson, eventData)) {
                                eventsFileStorage.markEventAsMatched(event.event_num)
                                return@withTimeoutOrNull true
                            }
                        }
                    }

                    // Pause before the next check
                    delay(pollingInterval)
                }
            } ?: false

            // Validate the task result
            try {
                if (result as Boolean) {
                    println("Expected event '$eventName' found.")
                } else {
                    if (eventData != null) {
                        assert(false) { "Event '$eventName' with data '$eventData' was not found within $timeoutEventExpectation seconds." }
                    } else {
                        assert(false) { "Event '$eventName' was not found within $timeoutEventExpectation seconds." }
                    }
                }
            } catch (e: ClassCastException) {
                throw Exception("Cannot cast await result to Boolean: $result", e)
            }
        }

        // Add the task to the list for later awaiting
        jobs.add(job)
    }

    /**
     * Waits for all launched asynchronous event checks to complete.
     *
     * This method must be called in the `@AfterEach` method after the test is executed
     * to ensure that all background event checks are finished before the test ends.
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
 * DSL: build a PageElement by an item from an event and auto-scroll until a match is found.
 * Relies on EventVerifier + UiElementWaiting + UiScrollGestures.
 */
interface EventDrivenUi : EventVerifier, UiElementFinding, UiScrollGestures {
    /**
     * Build a PageElement using an item from an event whose data contains all pairs from eventData somewhere.
     * Returns a PageElement for further use (e.g., in the click method).
     *
     * @param eventPosition which event to use: "first" - the first found, "last" - the last found
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
                // Wait for the event by conditions
                "Waiting for event $eventName (attempt ${attempt + 1}/$maxAttempts)" {
                    checkHasEvent(eventName, eventData, timeoutEventExpectation)
                }

                // Obtain the matching event
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
                    // If the event is not found, try to scroll and retry
                    if (attempt < maxAttempts - 1) {
                        logger.info("Event '$eventName' with filter '$eventData' not found. Scrolling (1 step) and retrying...")
                        performScroll(
                            element = null,
                            scrollCount = 1,
                            scrollCapacity = scrollCapacity,
                            scrollDirection = scrollDirection
                        )
                        attempt++
                        continue
                    } else {
                        throw NoSuchElementException("Event '$eventName' with filter '$eventData' was not found after $maxAttempts attempts (with scrolling)")
                    }
                }

                // Extract the items array from body → event → data
                val bodyObj = Json.parseToJsonElement(matchedEvent.data!!.body).jsonObject
                val itemsArr = bodyObj["event"]!!.jsonObject["data"]!!.jsonObject["items"]!!.jsonArray

                // Find the first item that contains the desired pairs — this will be the first product card on the page
                val searchObj = Json.parseToJsonElement(eventData).jsonObject
                val matched = itemsArr.firstOrNull { itemElem ->
                    searchObj.all { (key, sv) ->
                        findKeyValueInTree(itemElem, key, sv)
                    }
                } ?: throw NoSuchElementException("No item in event '$eventName' matches $eventData")

                // Get the found item's "name" field
                val itemName = matched.jsonObject["name"]!!.jsonPrimitive.content
                val positionTextCase = if (eventPosition.lowercase() == "last") "last" else "first"
                logger.info("Matching product found: '$itemName' in the $positionTextCase event by filters (eventName=$eventName, filter=$eventData, position=$eventPosition)")

                // Build and return the locator
                return PageElement(
                    android = PageElement.Text(itemName),
                    ios = PageElement.Label(itemName)
                )
            } catch (t: Throwable) {
                // If the event check failed (e.g., due to timeout), try to scroll and retry if attempts remain
                if (attempt < maxAttempts - 1) {
                    logger.info("Failed to find event '$eventName' on attempt ${attempt + 1}: ${t.message}. Scrolling (1 step) and retrying...")
                    performScroll(
                        element = null,
                        scrollCount = 1,
                        scrollCapacity = scrollCapacity,
                        scrollDirection = scrollDirection
                    )
                    attempt++
                    continue
                } else {
                    // Attempts exhausted — propagate the error
                    throw NoSuchElementException("Event '$eventName' with filter '$eventData' was not found after $maxAttempts attempts (with scrolling). Last error: ${t.message}")
                }
            }
        }

        // Theoretically unreachable
        throw NoSuchElementException("Event '$eventName' with filter '$eventData' was not found")
    }
}
