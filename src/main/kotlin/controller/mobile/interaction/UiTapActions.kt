package controller.mobile.interaction

import com.google.common.collect.ImmutableList
import controller.mobile.element.PageElement
import dsl.testing.StepContext
import org.openqa.selenium.interactions.PointerInput
import org.openqa.selenium.interactions.Sequence
import org.openqa.selenium.support.ui.WebDriverWait
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION
import java.time.Duration

interface UiTapActions : UiElementFinding {

    /**
     * Tap in the screen area at coordinates [x], [y].
     *
     * @param x the X coordinate on the screen;
     * @param y the Y coordinate on the screen;
     * @param timeoutBeforeExpectation maximum number of seconds to wait before performing the tap.
     * If the [waitCondition] parameter is provided, the tap will wait for that condition to be met within this time.
     * If [waitCondition] is not provided, the tap will wait for UI stabilization (no DOM changes)
     * within the specified time. If the value is 0, no wait is performed before the tap.
     */
    fun StepContext.tapArea(
        x: Int,
        y: Int,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        waitCondition: (() -> Boolean)? = null
    ) {
        if (waitCondition != null) {
            WebDriverWait(driver, timeoutBeforeExpectation)
                .until { waitCondition.invoke() }
        } else if (timeoutBeforeExpectation > 0) {
            val beforeWait = WebDriverWait(driver, timeoutBeforeExpectation)

            // wait for page stabilization
            var previousPageSource = driver.pageSource
            beforeWait.until {
                val currentPageSource = driver.pageSource
                val isStable = currentPageSource == previousPageSource
                previousPageSource = currentPageSource
                isStable
            }
        }

        val pointerInput = PointerInput(PointerInput.Kind.TOUCH, "finger1")
        val sequence = Sequence(pointerInput, 0)
        val origin = PointerInput.Origin.viewport()

        sequence.addAction(pointerInput.createPointerMove(Duration.ZERO, origin, x, y))
        sequence.addAction(pointerInput.createPointerDown(0))
        sequence.addAction(pointerInput.createPointerUp(0))

        driver.perform(ImmutableList.of(sequence))
    }

    /**
     * Tap within the [element] area at coordinates [x], [y].
     *
     * @param element the target element;
     * @param elementNumber index of the found element starting from 1;
     * @param x the X coordinate relative to the element;
     * @param y the Y coordinate relative to the element;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no DOM changes)
     *        before starting the element search;
     * @param timeoutExpectation number of seconds to wait for the element to appear;
     * @param pollingInterval frequency of element polling in milliseconds;
     * @param scrollCount number of scroll attempts if the element is not found on the current screen;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0], with 1.0 scrolling a full screen;
     * @param scrollDirection scroll direction;
     */
    fun StepContext.tapElementArea(
        element: PageElement?,
        elementNumber: Int? = null,
        x: Int,
        y: Int,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        waitForElements(
            element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).let { foundElement ->
            val elementLocation = foundElement.location
            val elementPointX = elementLocation.x + x
            val elementPointY = elementLocation.y + y

            tapArea(elementPointX, elementPointY)
        }
    }
}
