package utils

import controller.mobile.interaction.ScrollDirection

/**
 * Constants for configuring timeouts, scrolling, and element searching in tests.
 */

/**
 * Number of seconds to wait before starting element search.
 *
 * Used as an artificial delay before starting verification.
 */
const val DEFAULT_TIMEOUT_BEFORE_EXPECTATION: Long = 0

/**
 * Maximum number of seconds during which element search is performed.
 *
 * After exceeding the timeout, a [NoSuchElementException] will be thrown.
 */
const val DEFAULT_TIMEOUT_EXPECTATION: Long = 10

/**
 * Maximum number of seconds during which event verification is performed.
 *
 * After exceeding the timeout, an [Exception] will be thrown.
 */
const val DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION: Long = 15

/**
 * Coefficient for calculating offsets from the edge of the screen when scrolling.
 *
 * The value defines what portion of the screen will be covered by the swipe during scrolling.
 */
const val DEFAULT_SCROLL_COEFFICIENT = 0.75

/**
 * Coefficient for calculating offsets from the element size when performing a swipe.
 *
 * Used for more precise swipes on smaller elements.
 */
const val DEFAULT_SWIPE_COEFFICIENT = 0.95

/**
 * Number of allowed scroll attempts when trying to find an element.
 *
 * A value of 0 means the search is performed only on the current screen without scrolling.
 */
const val DEFAULT_SCROLL_COUNT = 0

/**
 * Portion of the screen that will be scrolled in one swipe operation.
 *
 * A value of 1.0 means a full page scroll.
 */
const val DEFAULT_SCROLL_CAPACITY = 1.0

/**
 * Polling interval for checking element state while waiting for it to appear (in milliseconds).
 */
const val DEFAULT_POLLING_INTERVAL = 1000L

/**
 * Default scroll direction when searching for elements.
 *
 * Used for auto-scrolling if the element is not found on the first screen.
 */
val DEFAULT_SCROLL_DIRECTION = ScrollDirection.Down
