package controller.mobile.interaction

import com.google.common.collect.ImmutableList
import controller.mobile.element.PageElement
import dsl.testing.StepContext
import org.openqa.selenium.interactions.Pause
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COEFFICIENT
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SWIPE_COEFFICIENT
import java.time.Duration

interface UiScrollGestures: UiElementFinding {

    /**
     * Function that performs scroll and swipe in given directions.
     */
    override fun performScroll(
        element: PageElement?,
        scrollCount: Int,
        scrollCapacity: Double,
        scrollDirection: ScrollDirection
    ) {
        assert(scrollCapacity > 0.0 && scrollCapacity <= 1.0) {
            "scrollCapacity is $scrollCapacity, but must be within 0.0 to 1.0"
        }
        if (element != null) {
            val el = waitForElements(element)
            val elLocation = el.location
            val size = el.size
            repeat(scrollCount) {
                when (scrollDirection) {
                    ScrollDirection.Right, ScrollDirection.Left -> {
                        val isScrollRight = scrollDirection is ScrollDirection.Right
                        val width = size.width * scrollCapacity
                        val centerY = el.center.y
                        val startX = if (isScrollRight) {
                            elLocation.x + (width * DEFAULT_SWIPE_COEFFICIENT)
                        } else {
                            elLocation.x + (width * (1 - DEFAULT_SWIPE_COEFFICIENT))
                        }.toInt()
                        val endX = if (isScrollRight) {
                            elLocation.x + (width * (1 - DEFAULT_SWIPE_COEFFICIENT))
                        } else {
                            elLocation.x + (width * DEFAULT_SWIPE_COEFFICIENT)
                        }.toInt()
                        touchAndMoveHorizontal(centerY, startX, endX)
                    }

                    ScrollDirection.Down, ScrollDirection.Up -> {
                        val isScrollDown = scrollDirection is ScrollDirection.Down
                        val height = size.height * scrollCapacity
                        val centerX = el.center.x
                        val startY = if (isScrollDown) {
                            elLocation.y + (height * DEFAULT_SWIPE_COEFFICIENT)
                        } else {
                            elLocation.y + (height * (1 - DEFAULT_SWIPE_COEFFICIENT))
                        }.toInt()
                        val endY = if (isScrollDown) {
                            elLocation.y + (height * (1 - DEFAULT_SWIPE_COEFFICIENT))
                        } else {
                            elLocation.y + (height * DEFAULT_SWIPE_COEFFICIENT)
                        }.toInt()
                        touchAndMoveVertical(centerX, startY, endY)
                    }
                }
            }
        } else {
            repeat(scrollCount) {
                when (scrollDirection) {
                    ScrollDirection.Right, ScrollDirection.Left -> {
                        val isScrollRight = scrollDirection is ScrollDirection.Right
                        val size = driver.manage().window().size
                        val width = size.width * scrollCapacity
                        val centerY = size.height / 2
                        val startX = if (isScrollRight) {
                            width * DEFAULT_SCROLL_COEFFICIENT
                        } else {
                            width * (1 - DEFAULT_SCROLL_COEFFICIENT)
                        }.toInt()
                        val endX = (if (isScrollRight) 0 else width).toInt()
                        touchAndMoveHorizontal(centerY, startX, endX)
                    }

                    ScrollDirection.Down, ScrollDirection.Up -> {
                        val isScrollDown = scrollDirection is ScrollDirection.Down
                        val size = driver.manage().window().size
                        val height = size.height * scrollCapacity
                        val centerX = size.width / 2
                        val startY = if (isScrollDown) {
                            height * DEFAULT_SCROLL_COEFFICIENT
                        } else {
                            height * (1 - DEFAULT_SCROLL_COEFFICIENT)
                        }.toInt()
                        val endY = (if (isScrollDown) 0 else height).toInt()
                        touchAndMoveVertical(centerX, startY, endY)
                    }
                }
            }
        }
    }

    /**
     * Function to scroll the screen vertically.
     */
    private fun touchAndMoveVertical(center: Int, start: Int, end: Int) {
        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger1")
        val swipe = org.openqa.selenium.interactions.Sequence(finger, 0)
        val movement = Duration.ofMillis(500)
        val hold = Duration.ofMillis(500)

        swipe.addAction(
            finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                center,
                start
            )
        )
        swipe.addAction(finger.createPointerDown(0))
        swipe.addAction(Pause(finger, hold))
        swipe.addAction(
            finger.createPointerMove(
                movement,
                PointerInput.Origin.viewport(),
                center,
                end
            )
        )
        swipe.addAction(finger.createPointerUp(0))

        driver.perform(ImmutableList.of(swipe))
    }

    /**
     * Function to scroll the screen horizontally.
     */
    private fun touchAndMoveHorizontal(center: Int, start: Int, end: Int) {
        val finger = PointerInput(PointerInput.Kind.TOUCH, "finger1")
        val swipe = Sequence(finger, 0)
        val movement = Duration.ofMillis(500)
        val hold = Duration.ofMillis(500)

        swipe.addAction(
            finger.createPointerMove(
                Duration.ZERO,
                PointerInput.Origin.viewport(),
                start,
                center
            )
        )
        swipe.addAction(finger.createPointerDown(0))
        swipe.addAction(Pause(finger, hold))
        swipe.addAction(
            finger.createPointerMove(
                movement,
                PointerInput.Origin.viewport(),
                end,
                center
            )
        )
        swipe.addAction(finger.createPointerUp(0))

        driver.perform(ImmutableList.of(swipe))
    }

    // ---- Gestures DSL (StepContext.*) ----

    /**
     * Scroll down.
     * @param scrollCount number of scroll attempts if the element is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     */
    fun StepContext.scrollDown(
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element = null, scrollCount, scrollCapacity, ScrollDirection.Down)

    /**
     * Scroll up.
     * @param scrollCount number of scroll attempts if the element is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     */
    fun StepContext.scrollUp(
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element = null, scrollCount, scrollCapacity, ScrollDirection.Up)

    /**
     * Scroll right.
     * @param scrollCount number of scroll attempts if the element is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     */
    fun StepContext.scrollRight(
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element = null, scrollCount, scrollCapacity, ScrollDirection.Right)

    /**
     * Scroll left.
     * @param scrollCount number of scroll attempts if the element is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     */
    fun StepContext.scrollLeft(
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element = null, scrollCount, scrollCapacity, ScrollDirection.Left)

    /**
     * Swipe down.
     * @param element the element;
     * @param scrollCount number of scroll attempts if the element is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     */
    fun StepContext.swipeDown(
        element: PageElement?,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Down)

    /**
     * Swipe up.
     * @param element the element;
     * @param scrollCount number of scroll attempts if the element is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     */
    fun StepContext.swipeUp(
        element: PageElement?,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Up)

    /**
     * Swipe right.
     * @param element the element;
     * @param scrollCount number of scroll attempts if the element is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     */
    fun StepContext.swipeRight(
        element: PageElement?,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Right)

    /**
     * Swipe left.
     * @param element the element;
     * @param scrollCount number of scroll attempts if the element is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     */
    fun StepContext.swipeLeft(
        element: PageElement?,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY
    ) = performScroll(element, scrollCount, scrollCapacity, ScrollDirection.Left)
}
