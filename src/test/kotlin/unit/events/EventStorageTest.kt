package unit.events

import events.Event
import events.EventData
import events.EventStorage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests for [EventStorage].
 *
 * Tests the core functionality of the event storage system:
 * - Adding events
 * - Marking events as matched
 * - Retrieving events
 * - Clearing the storage
 */
class EventStorageTest {

    @BeforeEach
    fun setUp() {
        // Clear storage before each test
        EventStorage.clear()
    }

    @AfterEach
    fun tearDown() {
        // Clear storage after each test
        EventStorage.clear()
    }

    @Test
    fun `test add events`() {
        // Create test events
        val event1 = createTestEvent(1, "test_event_1")
        val event2 = createTestEvent(2, "test_event_2")

        // Add events to storage
        EventStorage.addEvents(listOf(event1, event2))

        // Verify that events were added
        val events = EventStorage.getEvents()
        assertEquals(2, events.size)
        assertTrue(events.any { it.event_num == 1 && it.name == "test_event_1" })
        assertTrue(events.any { it.event_num == 2 && it.name == "test_event_2" })
    }

    @Test
    fun `test add duplicate events`() {
        // Create test events with the same event number
        val event1 = createTestEvent(1, "test_event_1")
        val event2 = createTestEvent(1, "test_event_duplicate")

        // Add events to storage
        EventStorage.addEvents(listOf(event1))
        EventStorage.addEvents(listOf(event2))

        // Verify that only the first event was added
        val events = EventStorage.getEvents()
        assertEquals(1, events.size)
        assertEquals("test_event_1", events[0].name)
    }

    @Test
    fun `test mark event as matched`() {
        // Create and add test events
        val event1 = createTestEvent(1, "test_event_1")
        val event2 = createTestEvent(2, "test_event_2")
        EventStorage.addEvents(listOf(event1, event2))

        // Mark event as matched
        EventStorage.markEventAsMatched(1)

        // Verify matching flags
        assertTrue(EventStorage.isEventAlreadyMatched(1))
        assertFalse(EventStorage.isEventAlreadyMatched(2))
    }

    @Test
    fun `test get index events`() {
        // Create and add test events
        val events = (1..5).map { createTestEvent(it, "test_event_$it") }
        EventStorage.addEvents(events)

        // Mark some events as matched
        EventStorage.markEventAsMatched(2)
        EventStorage.markEventAsMatched(4)

        // Retrieve events starting from index 0
        val indexEvents = EventStorage.getIndexEvents(0)

        // Verify only unmatched events are returned
        assertEquals(3, indexEvents.size)
        assertTrue(indexEvents.any { it.event_num == 1 })
        assertFalse(indexEvents.any { it.event_num == 2 }) // Matched; must be excluded
        assertTrue(indexEvents.any { it.event_num == 3 })
        assertFalse(indexEvents.any { it.event_num == 4 }) // Matched; must be excluded
        assertTrue(indexEvents.any { it.event_num == 5 })
    }

    @Test
    fun `test get last event`() {
        // Create and add test events
        val event1 = createTestEvent(1, "test_event_1")
        val event2 = createTestEvent(2, "test_event_2")

        // Add events to storage
        EventStorage.addEvents(listOf(event1, event2))

        // Verify the last event
        val lastEvent = EventStorage.getLastEvent()
        assertNotNull(lastEvent)
        assertEquals(2, lastEvent?.event_num)
        assertEquals("test_event_2", lastEvent?.name)
    }

    @Test
    fun `test clear storage`() {
        // Create and add test events
        val events = (1..3).map { createTestEvent(it, "test_event_$it") }
        EventStorage.addEvents(events)

        // Mark one event as matched
        EventStorage.markEventAsMatched(2)

        // Clear the storage
        EventStorage.clear()

        // Verify storage is empty and matched flags are reset
        assertTrue(EventStorage.getEvents().isEmpty())
        assertFalse(EventStorage.isEventAlreadyMatched(2))
    }

    /**
     * Helper method to create a test event.
     */
    private fun createTestEvent(eventNum: Int, eventName: String): Event {
        return Event(
            event_time = Instant.now().toString(),
            event_num = eventNum,
            name = eventName,
            data = EventData(
                uri = "/test/uri",
                remoteAddress = "127.0.0.1:8000",
                headers = mapOf("Content-Type" to listOf("application/json")),
                query = "param=value",
                body = "{\"test\": \"data\"}"
            )
        )
    }
}
