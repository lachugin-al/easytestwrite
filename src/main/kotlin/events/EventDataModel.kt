package events

import kotlinx.serialization.Serializable

/**
 * Event model recorded during testing.
 *
 * Used for serializing and storing data about network requests
 * sent by the application during test execution.
 *
 * All events are recorded in the [Event] format and can be used
 * to validate outgoing data and reproduce network behavior.
 *
 * @property event_time Timestamp of the event (e.g., Instant.now().toString()).
 * @property event_num Unique event number within a single testing session.
 * @property name Event name (e.g., HTTP method or logical request name).
 * @property data Event details, including request body and metadata [EventData].
 */
@Serializable
data class Event(
    val event_time: String,
    val event_num: Int,
    val name: String,
    val data: EventData? = null
)

/**
 * Detailed information about the network request associated with an [Event].
 *
 * The model stores the entire request payload,
 * including the request URI, sender IP address, headers, query string, and body.
 *
 * @property uri Request path without the domain name (e.g., "/m/batch").
 * @property remoteAddress Client address that sent the request (e.g., "192.168.1.2:53427").
 * @property headers Collection of HTTP request headers.
 * @property query Query string, if present.
 * @property body Request body in JSON format (string).
 */
@Serializable
data class EventData(
    val uri: String,
    val remoteAddress: String,
    val headers: Map<String, List<String>>,
    val query: String? = null,
    val body: String
)
