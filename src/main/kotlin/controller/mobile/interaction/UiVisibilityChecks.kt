package controller.mobile.interaction

import controller.mobile.element.PageElement
import dsl.testing.ExpectationContext
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface UiVisibilityChecks : UiElementFinding {

    /**
     * Check if the [element] is visible on the screen.
     *
     * @param element the target element;
     * @param elementNumber the index of the found element starting from 1;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no DOM changes)
     *        before starting the element search;
     * @param timeoutExpectation number of seconds to wait for the element to appear;
     * @param pollingInterval frequency of element polling in milliseconds;
     * @param scrollCount number of scroll attempts if the element is not found on the current screen;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0], with 1.0 scrolling a full screen;
     * @param scrollDirection scroll direction;
     *
     * @exception java.util.NoSuchElementException if the element is not found
     */
    fun ExpectationContext.checkVisible(
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
        ).isDisplayed
    }

    /**
     * Check if an element containing the specified text is visible on the screen.
     *
     * The method builds a locator using the `PageElement.Contains` strategy across all platforms
     * (Android, iOS, Web) and performs element search with scrolling support.
     * If the element is found, its visibility will be checked.
     *
     * Useful for DSL when only part of the text is known (e.g., a button, label, etc.),
     * but the exact locator is not specified or changes dynamically.
     *
     * @param text the exact text to match in the target element;
     * @param containsText the substring that must be contained in the element;
     * @param elementNumber the index of the matched element starting from 1;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no DOM changes)
     *        before starting the element search;
     * @param timeoutExpectation maximum wait time for the element;
     * @param pollingInterval interval between element search attempts;
     * @param scrollCount number of scroll attempts if the element is not found;
     * @param scrollCapacity fraction of the screen to scroll per action [0.0 - 1.0];
     * @param scrollDirection scroll direction;
     *
     * @throws java.util.NoSuchElementException if the element is not found or not visible.
     */
    fun ExpectationContext.checkVisible(
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
        ).isDisplayed
    }
}
