package controller.mobile.interaction

import controller.mobile.element.PageElement
import dsl.testing.StepContext
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface UiClickActions : UiElementFinding {

    /**
     * Find an element on the screen by its [element] and click it.
     *
     * @param element the target element;
     * @param elementNumber the index of the found element starting from 1;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no DOM changes)
     *        before starting the element search;
     * @param timeoutExpectation number of seconds to wait for the element to appear;
     * @param pollingInterval frequency of element polling in milliseconds;
     * @param scrollCount number of scroll attempts if the element is not found on the current screen;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0], with 1.0 scrolling a full screen;
     * @param scrollDirection direction of the scroll action;
     *
     * @exception java.util.NoSuchElementException if the element is not found
     */
    fun StepContext.click(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).click()
    }

    /**
     * Find an element on the screen containing the specified text and click it.
     *
     * The method builds a locator using the `PageElement.Contains` strategy across all platforms
     * (Android, iOS, Web) and performs element search with scrolling support.
     * If the element is found, it performs a click.
     *
     * Useful when you need to click an element containing a specific substring
     * (e.g., part of a product name, a button with dynamic text, etc.) without having to know
     * the exact name or locator in advance.
     *
     * @param text the exact text to match in the target element;
     * @param containsText the substring that must be contained in the target element;
     * @param elementNumber the index of the found element starting from 1. If null, the first found is used;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no DOM changes)
     *        before starting the element search;
     * @param timeoutExpectation maximum wait time for the element to appear, in seconds;
     * @param pollingInterval interval between element search attempts, in milliseconds;
     * @param scrollCount number of scroll attempts if the element is not found immediately;
     * @param scrollCapacity fraction of the screen to scroll per action [0.0 - 1.0];
     * @param scrollDirection direction of scrolling (down, up, etc.);
     *
     * @throws java.util.NoSuchElementException if no element containing the specified text is found after all attempts.
     */
    fun StepContext.click(
        text: String? = null,
        containsText: String? = null,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ) {
        require(!(text == null && containsText == null)) {
            "Either 'text' or 'contains' must be specified"
        }
        require(!(text != null && containsText != null)) {
            "Cannot use both 'text' and 'contains' simultaneously"
        }
        val element = when {
            text != null -> PageElement(
                android = PageElement.ExactMatch(text),
                ios = PageElement.ExactMatch(text)
            )

            containsText != null -> PageElement(
                android = PageElement.Contains(containsText),
                ios = PageElement.Contains(containsText),
            )

            else -> error("Neither text nor containsText specified")
        }

        waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).click()
    }
}
