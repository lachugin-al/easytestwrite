package unit.utils

import controller.mobile.interaction.ScrollDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_COEFFICIENT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_SWIPE_COEFFICIENT
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION

class ConstantsTest {

    @Test
    fun `verify numeric constants`() {
        assertEquals(0L, DEFAULT_TIMEOUT_BEFORE_EXPECTATION)
        assertEquals(10L, DEFAULT_TIMEOUT_EXPECTATION)
        assertEquals(15L, DEFAULT_TIMEOUT_EVENT_CHECK_EXPECTATION)
        assertEquals(0.75, DEFAULT_SCROLL_COEFFICIENT)
        assertEquals(0.95, DEFAULT_SWIPE_COEFFICIENT)
        assertEquals(0, DEFAULT_SCROLL_COUNT)
        assertEquals(1.0, DEFAULT_SCROLL_CAPACITY)
        assertEquals(1000L, DEFAULT_POLLING_INTERVAL)
    }

    @Test
    fun `verify default scroll direction`() {
        assertEquals(ScrollDirection.Down, DEFAULT_SCROLL_DIRECTION)
    }
}
