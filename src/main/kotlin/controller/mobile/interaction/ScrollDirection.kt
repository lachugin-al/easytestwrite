package controller.mobile.interaction

/**
 * Enumeration of scroll directions on mobile devices and web interfaces.
 *
 * Used to control the swipe direction when scrolling a list of elements
 * in UI automation tests.
 *
 * Implemented as a [sealed class] to ensure type safety
 * and allow convenient future extensions.
 */
sealed class ScrollDirection {

    /** Scroll down (vertical). */
    object Down : ScrollDirection()

    /** Scroll up (vertical). */
    object Up : ScrollDirection()

    /** Scroll right (horizontal). */
    object Right : ScrollDirection()

    /** Scroll left (horizontal). */
    object Left : ScrollDirection()
}
