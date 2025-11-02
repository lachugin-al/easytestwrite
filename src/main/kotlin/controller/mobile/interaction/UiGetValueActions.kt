package controller.mobile.interaction

import controller.mobile.element.PageElement
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION
import utils.NumberParser

interface UiGetValueActions : UiElementFinding {

    /**
     * Get the text from [element] found on the screen.
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
     *
     * @return String
     */
    fun getText(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): String {
        return waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).text.toString()
    }

    /**
     * Get a numeric value from [element] found on the screen.
     *
     * A universal parser for numeric values from “dirty” text (prices/amounts):
     * special spaces are normalized, currency/text suffixes removed,
     * decimal separator detected, and thousand separators removed.
     *
     * Examples: "1 22323", "1232", "12.2323", "price 12 r".
     *
     * @return Double or null
     */
    fun getNumber(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): Double? {
        val text = waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).text.toString()
        return NumberParser.parseNumber(text)
    }

    /**
     * Get the value of [attribute] from [element] found on the screen.
     *
     * @param element the target element;
     * @param elementNumber the index of the found element starting from 1;
     * @param attribute the attribute name whose value should be retrieved;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no DOM changes)
     *        before starting the element search;
     * @param timeoutExpectation number of seconds to wait for the element to appear;
     * @param pollingInterval frequency of element polling in milliseconds;
     * @param scrollCount number of scroll attempts if the element is not found on the current screen;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0], with 1.0 scrolling a full screen;
     * @param scrollDirection scroll direction;
     *
     * @exception java.util.NoSuchElementException if the element is not found
     *
     * @return String
     */
    fun getAttributeValue(
        element: PageElement?,
        elementNumber: Int? = null,
        attribute: String,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): String {
        return waitForElements(
            element = element,
            elementNumber = elementNumber,
            timeoutBeforeExpectation = timeoutBeforeExpectation,
            timeoutExpectation = timeoutExpectation,
            pollingInterval = pollingInterval,
            scrollCount = scrollCount,
            scrollCapacity = scrollCapacity,
            scrollDirection = scrollDirection
        ).getAttribute(attribute)
    }
}
