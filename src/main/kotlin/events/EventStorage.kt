package events

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Storage for events recorded during testing.
 *
 * Provides centralized saving of all received [Event],
 * as well as management of their state (processed/unprocessed).
 *
 * Used for searching events by various criteria within test checks.
 */
object EventStorage {
    private val logger: Logger = LoggerFactory.getLogger(EventStorage::class.java)

    /** List of all recorded events. */
    private val events = mutableListOf<Event>()

    /** Set of event numbers that have already been processed. */
    private val matchedEvents = mutableSetOf<Int>()

    /**
     * Adds a list of new events to the storage.
     *
     * Before adding, checks uniqueness of the event number ([event_num]).
     * Duplicate events are ignored.
     *
     * @param newEvents List of new events to add.
     */
    fun addEvents(newEvents: List<Event>) {
        newEvents.forEach { event ->
            if (!eventExists(event.event_num)) {
                events.add(event)
                logger.info("Saved event: ${event.name}, Number: ${event.event_num}, Time: ${event.event_time}, Data: ${event.data}")
            }
        }
    }

    /**
     * Checks whether an event exists in the storage by its number.
     *
     * @param eventNumber Event number.
     * @return `true` if the event already exists, otherwise `false`.
     */
    private fun eventExists(eventNumber: Int): Boolean {
        return events.any { it.event_num == eventNumber }
    }

    /**
     * Marks an event as processed (matched) by its number.
     *
     * @param eventNum Event number to mark.
     */
    fun markEventAsMatched(eventNum: Int) {
        matchedEvents.add(eventNum)
    }

    /**
     * Checks whether an event has already been processed.
     *
     * @param eventNum Event number.
     * @return `true` if the event is already marked as processed, otherwise `false`.
     */
    fun isEventAlreadyMatched(eventNum: Int): Boolean {
        return matchedEvents.contains(eventNum)
    }

    /**
     * Gets a list of events starting from the specified index.
     *
     * Already processed events are excluded.
     *
     * @param index The index from which to get events.
     * @return List of new events, or an empty list if the index is outside the storage size.
     */
    fun getIndexEvents(index: Int): List<Event> {
        return if (index < events.size) {
            events.subList(index, events.size).filterNot { event ->
                isEventAlreadyMatched(event.event_num)
            }
        } else {
            emptyList()
        }
    }

    /**
     * Returns all recorded events.
     *
     * @return Copy of the list of all events.
     */
    fun getEvents(): List<Event> {
        return events.toList()
    }

    /**
     * Returns the last added event.
     *
     * @return The last event, or `null` if the storage is empty.
     */
    fun getLastEvent(): Event? {
        return events.lastOrNull()
    }

    /**
     * Clears the event storage and resets the list of processed events.
     */
    fun clear() {
        events.clear()
        matchedEvents.clear()
    }
}
