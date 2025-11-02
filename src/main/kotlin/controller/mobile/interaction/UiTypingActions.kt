package controller.mobile.interaction

import controller.mobile.element.PageElement
import dsl.testing.StepContext
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface UiTypingActions : UiElementFinding {

    /**
     * Find an element on the screen by its [element] and type [text].
     * @param element the element;
     * @param elementNumber index of the found element starting from 1;
     * @param text text to type;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no changes in page source) before starting the search;
     * @param timeoutExpectation number of seconds during which the element is searched for;
     * @param pollingInterval polling frequency in milliseconds;
     * @param scrollCount number of scroll attempts toward the element if it is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     * @param scrollDirection scroll direction of the screen;
     *
     * @exception java.util.NoSuchElementException element not found
     */
    fun StepContext.typeText(
        element: PageElement?,
        elementNumber: Int? = null,
        text: String,
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
        ).sendKeys(text)
    }
}
