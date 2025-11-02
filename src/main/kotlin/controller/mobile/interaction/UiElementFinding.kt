package controller.mobile.interaction

import controller.mobile.element.PageElement
import controller.mobile.core.AppContext
import io.appium.java_client.MobileElement
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import utils.DEFAULT_POLLING_INTERVAL
import utils.DEFAULT_SCROLL_CAPACITY
import utils.DEFAULT_SCROLL_COUNT
import utils.DEFAULT_SCROLL_DIRECTION
import utils.DEFAULT_TIMEOUT_BEFORE_EXPECTATION
import utils.DEFAULT_TIMEOUT_EXPECTATION

interface UiElementFinding : AppContext {

    fun performScroll(
        element: PageElement? = null,
        scrollCount: Int,
        scrollCapacity: Double,
        scrollDirection: ScrollDirection
    )

    /**
     * Find an element on the screen by its [element].
     * @param element the element;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no changes in page source) before starting the search;
     * @param timeoutExpectation number of seconds during which the element is searched for;
     * @param pollingInterval polling frequency in milliseconds;
     * @param scrollCount number of scroll attempts toward the element if it is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     * @param scrollDirection scroll direction of the screen;
     *
     * @exception java.util.NoSuchElementException element not found
     *
     * @return MobileElement
     */
    fun waitForElement(
        element: PageElement?,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): MobileElement {
        // If timeoutBeforeExpectation > 0, wait until the page becomes stable
        if (timeoutBeforeExpectation > 0) {
            val beforeWait = WebDriverWait(driver, timeoutBeforeExpectation, pollingInterval)
            try {
                // Wait until the app UI becomes stable by checking whether the page source stops changing
                var previousPageSource = driver.pageSource
                beforeWait.until { d ->
                    try {
                        val currentPageSource = d.pageSource
                        val isStable = currentPageSource == previousPageSource
                        previousPageSource = currentPageSource

                        // If the page source has not changed, consider the UI stable
                        // But continue checking until the timeout to ensure it truly stabilizes
                        isStable
                    } catch (e: Exception) {
                        // If an error occurs while retrieving the page source, assume the UI is stable
                        logger.debug("Error while checking page stability: ${e.message}")
                        true
                    }
                }
                logger.debug("UI appears stable after waiting")
            } catch (e: Exception) {
                // If the wait fails, log the error but continue searching for the element
                logger.warn("Error while waiting for UI stability: ${e.message}")
            }
        }

        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        val pageElement = element?.get()
        var currentScroll = 0
        while (true) {
            try {
                return wait.until(ExpectedConditions.visibilityOfElementLocated(pageElement as By)) as MobileElement
            } catch (e: Exception) {
                // If the element is not found and scrollCount > 0 was provided
                if (scrollCount > 0 && currentScroll < scrollCount) {
                    performScroll(
                        scrollCount = 1,
                        scrollCapacity = scrollCapacity,
                        scrollDirection = scrollDirection
                    )
                    currentScroll++
                } else {
                    throw NoSuchElementException("Element '$pageElement' was not found within '$timeoutExpectation' seconds after '$currentScroll' scrolls")
                }
            }
        }
    }

    /**
     * Find all elements on the screen by its [element] and return a specific one by its index.
     * @param element the element;
     * @param elementNumber index of the found element starting from 1;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no changes in page source) before starting the search;
     * @param timeoutExpectation number of seconds during which the elements are searched for;
     * @param pollingInterval polling frequency in milliseconds;
     * @param scrollCount number of scroll attempts toward the element if it is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     * @param scrollDirection scroll direction of the screen;
     *
     * @exception java.util.NoSuchElementException elements not found
     *
     * @return MobileElement
     */
    fun waitForElements(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): MobileElement {
        // If timeoutBeforeExpectation > 0, wait until the page becomes stable
        if (timeoutBeforeExpectation > 0) {
            val beforeWait = WebDriverWait(driver, timeoutBeforeExpectation, pollingInterval)
            try {
                // Wait until the app UI becomes stable by checking whether the page source stops changing
                var previousPageSource = driver.pageSource
                beforeWait.until { d ->
                    try {
                        val currentPageSource = d.pageSource
                        val isStable = currentPageSource == previousPageSource
                        previousPageSource = currentPageSource

                        // If the page source has not changed, consider the UI stable
                        // But continue checking until the timeout to ensure it truly stabilizes
                        isStable
                    } catch (e: Exception) {
                        // If an error occurs while retrieving the page source, assume the UI is stable
                        logger.debug("Error while checking page stability: ${e.message}")
                        true
                    }
                }
                logger.debug("UI appears stable after waiting")
            } catch (e: Exception) {
                // If the wait fails, log the error but continue searching for the element
                logger.warn("Error while waiting for UI stability: ${e.message}")
            }
        }

        val wait = WebDriverWait(driver, timeoutExpectation, pollingInterval)
        var currentScroll = 0

        while (true) {
            // Get all locators for the current platform
            val locators = element?.getAll() ?: listOf(element?.get())
            var lastException: Exception? = null
            val attemptedLocators = mutableListOf<Any>()
            val failedLocators = mutableListOf<Any>()

            // Iterate over all locators
            for (locator in locators.filterNotNull()) {
                try {
                    val pageElement = locator
                    attemptedLocators.add(pageElement)
                    val elements =
                        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(pageElement as By)) as List<MobileElement>

                    val safeIndex = elementNumber ?: 1
                    if (safeIndex < 1 || safeIndex > elements.size) {
                        throw IndexOutOfBoundsException("Element $elementNumber is out of allowed range")
                    }

                    // Log info about failed attempts even if the current locator succeeded
                    if (failedLocators.isNotEmpty()) {
                        logger.info("The following locators ${failedLocators.joinToString(", ")} from ${attemptedLocators} were not found.")
                    }

                    return elements[safeIndex - 1]
                } catch (e: Exception) {
                    // Save the last exception
                    lastException = e
                    // Add the locator to the failed list
                    failedLocators.add(locator)
                    // Continue iterating over locators
                    continue
                }
            }

            // If no locator worked and scrollCount > 0 was provided
            if (scrollCount > 0 && currentScroll < scrollCount) {
                performScroll(
                    scrollCount = 1,
                    scrollCapacity = scrollCapacity,
                    scrollDirection = scrollDirection
                )
                currentScroll++
            } else {
                // Use info about the last exception and used locators for a more detailed error message
                val locatorsInfo = if (failedLocators.isNotEmpty()) {
                    "The following locators ${failedLocators.joinToString(", ")} from ${attemptedLocators} were not found."
                } else if (attemptedLocators.isNotEmpty()) {
                    "Attempted to find the following elements: ${attemptedLocators.joinToString(", ")}"
                } else {
                    "No elements were found"
                }

                val errorMessage = if (lastException != null) {
                    "Elements were not found within '$timeoutExpectation' seconds after '$currentScroll' scrolls. $locatorsInfo. Cause: ${lastException.message}"
                } else {
                    "Elements were not found within '$timeoutExpectation' seconds after '$currentScroll' scrolls. $locatorsInfo"
                }
                throw NoSuchElementException(errorMessage)
            }
        }
    }

    /**
     * Find an element on the screen by its [element] or return null.
     * @param element the element;
     * @param elementNumber index of the found element starting from 1;
     * @param timeoutBeforeExpectation number of seconds to wait for UI stabilization (no changes in page source) before starting the search;
     * @param timeoutExpectation number of seconds during which the element is searched for;
     * @param pollingInterval polling frequency in milliseconds;
     * @param scrollCount number of scroll attempts toward the element if it is not found on the current page;
     * @param scrollCapacity scroll height modifier [0.0 - 1.0]; 1.0 scrolls exactly one screen;
     * @param scrollDirection scroll direction of the screen;
     *
     * @exception java.util.NoSuchElementException element not found
     *
     * @return MobileElement or null
     */
    fun waitForElementOrNull(
        element: PageElement?,
        elementNumber: Int? = null,
        timeoutBeforeExpectation: Long = DEFAULT_TIMEOUT_BEFORE_EXPECTATION,
        timeoutExpectation: Long = DEFAULT_TIMEOUT_EXPECTATION,
        pollingInterval: Long = DEFAULT_POLLING_INTERVAL,
        scrollCount: Int = DEFAULT_SCROLL_COUNT,
        scrollCapacity: Double = DEFAULT_SCROLL_CAPACITY,
        scrollDirection: ScrollDirection = DEFAULT_SCROLL_DIRECTION
    ): MobileElement? {
        return try {
            waitForElements(
                element = element,
                elementNumber = elementNumber,
                timeoutBeforeExpectation = timeoutBeforeExpectation,
                timeoutExpectation = timeoutExpectation,
                pollingInterval = pollingInterval,
                scrollCount = scrollCount,
                scrollCapacity = scrollCapacity,
                scrollDirection = scrollDirection
            )
        } catch (e: NoSuchElementException) {
            null
        }
    }
}
