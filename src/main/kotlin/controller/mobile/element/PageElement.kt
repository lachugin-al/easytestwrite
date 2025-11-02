package controller.mobile.element

import app.config.AppConfig
import app.model.Platform
import io.appium.java_client.MobileBy
import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement
import org.openqa.selenium.internal.FindsByXPath

/**
 * Universal page element descriptor.
 *
 * Combines element search strategies for different platforms:
 * Android ([By]), iOS ([By]).
 *
 * Supports special search strategies for mobile platforms:
 * - Accessibility ID (Android and iOS)
 * - UIAutomator (Android only)
 * - iOS Class Chain (iOS only)
 * - iOS Predicate String (iOS only)
 *
 * Used to build cross-platform tests without duplicating locators.
 *
 * @property android Locator for the Android platform.
 * @property ios Locator for the iOS platform.
 */
data class PageElement(
    private val android: By? = null,
    private val ios: By? = null,
    private val androidList: List<By>? = null,
    private val iosList: List<By>? = null,
) {
    /**
     * Get the locator for the current platform.
     *
     * @return Locator of type [By] for mobile platforms or [String] for Web.
     */
    fun get(): Any? = when (AppConfig.getPlatform()) {
        Platform.ANDROID -> android ?: androidList?.firstOrNull() ?: error("Locator for Android is not specified")
        Platform.IOS -> ios ?: iosList?.firstOrNull() ?: error("Locator for iOS is not specified")
    }

    /**
     * Get the list of locators for the current platform.
     *
     * @return List of locators of type [By] for mobile platforms or [String] for Web.
     */
    fun getAll(): List<Any>? = when (AppConfig.getPlatform()) {
        Platform.ANDROID -> androidList ?: android?.let { listOf(it) }
        Platform.IOS -> iosList ?: ios?.let { listOf(it) }
    }

    companion object {
        /**
         * Builds the full resource identifier for Android based on the element name.
         */
        private fun fullPackageId(value: String) = "${AppConfig.getAppPackage()}:id/$value"

        /**
         * Creates a PageElement with a locator by accessibility id for both platforms.
         *
         * @param accessibilityId Accessibility id value.
         * @return PageElement with locator by accessibility id.
         */
        fun byAccessibilityId(accessibilityId: String): PageElement {
            val locator = AccessibilityId(accessibilityId)
            return PageElement(android = locator, ios = locator)
        }

        /**
         * Creates a PageElement with a locator by accessibility id for Android.
         *
         * @param accessibilityId Accessibility id value.
         * @return PageElement with locator by accessibility id for Android.
         */
        fun byAndroidAccessibilityId(accessibilityId: String): PageElement {
            return PageElement(android = AccessibilityId(accessibilityId))
        }

        /**
         * Creates a PageElement with a locator by accessibility id for iOS.
         *
         * @param accessibilityId Accessibility id value.
         * @return PageElement with locator by accessibility id for iOS.
         */
        fun byIOSAccessibilityId(accessibilityId: String): PageElement {
            return PageElement(ios = AccessibilityId(accessibilityId))
        }

        /**
         * Creates a PageElement with a locator by UIAutomator for Android.
         *
         * @param uiAutomatorExpression UIAutomator expression.
         * @return PageElement with locator by UIAutomator for Android.
         */
        fun byAndroidUIAutomator(uiAutomatorExpression: String): PageElement {
            return PageElement(android = AndroidUIAutomator(uiAutomatorExpression))
        }

        /**
         * Creates a PageElement with a locator by Class Chain for iOS.
         *
         * @param classChainExpression Class Chain expression.
         * @return PageElement with locator by Class Chain for iOS.
         */
        fun byIOSClassChain(classChainExpression: String): PageElement {
            return PageElement(ios = IOSClassChain(classChainExpression))
        }

        /**
         * Creates a PageElement with a locator by Predicate String for iOS.
         *
         * @param predicateExpression Predicate String expression.
         * @return PageElement with locator by Predicate String for iOS.
         */
        fun byIOSPredicateString(predicateExpression: String): PageElement {
            return PageElement(ios = IOSPredicateString(predicateExpression))
        }

        /**
         * Creates a PageElement with a list of locators for Android.
         *
         * @param locators List of locators for Android.
         * @return PageElement with a list of locators for Android.
         */
        fun byAndroidLocators(locators: List<By>): PageElement {
            return PageElement(androidList = locators)
        }

        /**
         * Creates a PageElement with a list of locators for iOS.
         *
         * @param locators List of locators for iOS.
         * @return PageElement with a list of locators for iOS.
         */
        fun byIOSLocators(locators: List<By>): PageElement {
            return PageElement(iosList = locators)
        }

        /**
         * Creates a PageElement with a list of locators for all platforms.
         *
         * @param androidLocators List of locators for Android.
         * @param iosLocators List of locators for iOS.
         * @return PageElement with a list of locators for all platforms.
         */
        fun byLocators(
            androidLocators: List<By>? = null,
            iosLocators: List<By>? = null,
        ): PageElement {
            return PageElement(
                androidList = androidLocators,
                iosList = iosLocators,
            )
        }
    }

    /**
     * Base class for creating custom locators via XPath.
     *
     * Implements standard element search methods through [SearchContext].
     *
     * @param value The value used for search.
     * @param valueType Type of the value (e.g. id, text, label).
     */
    abstract class BaseBy(private val value: String?, private val valueType: String) : By() {
        init {
            requireNotNull(value) { "Element by $valueType is not specified and is null." }
        }

        /**
         * Build an XPath expression based on the provided value.
         *
         * @param value The value to insert into XPath.
         * @return Constructed XPath string.
         */
        abstract fun buildXPath(value: String): String

        override fun findElements(context: SearchContext): List<WebElement> {
            return (context as FindsByXPath).findElementsByXPath(buildXPath(value!!))
        }

        override fun findElement(context: SearchContext): WebElement {
            return (context as FindsByXPath).findElementByXPath(buildXPath(value!!))
        }

        override fun toString(): String = "$valueType: $value"
    }

    /** Search by element id. */
    class Id(id: String?) : BaseBy(id, "id") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@id,'$value') or contains(@id,'${fullPackageId(value)}')]"
        }
    }

    /** Search by resource-id (Android). */
    class ResourceId(resourceId: String?) : BaseBy(resourceId, "resource-id") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@resource-id,'$value') or contains(@resource-id,'${fullPackageId(value)}')]"
        }
    }

    /** Search by exact text value of the element. */
    class Text(text: String?) : BaseBy(text, "text") {
        override fun buildXPath(value: String): String {
            return ".//*[@text = '$value']"
        }
    }

    /** Search by partial match of element path. */
    class Contains(text: String?) : BaseBy(text, "text") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@text,'$value') or contains(@id,'$value') or contains(@resource-id,'$value') or contains(@content-desc,'$value') or contains(@name,'$value') or contains(@label,'$value') or contains(@value,'$value')]"
        }
    }

    /** Search by exact match of element path. */
    class ExactMatch(text: String?) : BaseBy(text, "text") {
        override fun buildXPath(value: String): String {
            return ".//*[(@text='$value' or @id='$value' or @resource-id='$value' or @content-desc='$value' or @name='$value' or @label='$value' or @value='$value')]"
        }
    }

    /** Search by content-desc attribute (Android). */
    class ContentDesc(contentDesc: String?) : BaseBy(contentDesc, "content-desc") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@content-desc,'$value')]"
        }
    }

    /** Search using a custom XPath expression. */
    class XPath(xpathExpression: String?) : BaseBy(xpathExpression, "xpath") {
        override fun buildXPath(value: String): String = value
    }

    /** Search by value attribute (iOS). */
    class Value(value: String?) : BaseBy(value, "value") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@value,'$value')]"
        }
    }

    /** Search by name attribute (iOS). */
    class Name(name: String?) : BaseBy(name, "name") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@name,'$value')]"
        }
    }

    /** Search by label attribute (iOS). */
    class Label(label: String?) : BaseBy(label, "label") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@label,'$value')]"
        }
    }

    /**
     * Search by accessibility id (Android and iOS).
     *
     * Uses MobileBy.AccessibilityId strategy.
     */
    class AccessibilityId(accessibilityId: String?) : By() {
        private val accessibilityId: String?
        private val mobileBy: By

        init {
            requireNotNull(accessibilityId) { "Element by accessibility id is not specified and is null." }
            this.accessibilityId = accessibilityId
            this.mobileBy = MobileBy.AccessibilityId(accessibilityId)
        }

        override fun findElements(context: SearchContext): List<WebElement> {
            return context.findElements(mobileBy)
        }

        override fun findElement(context: SearchContext): WebElement {
            return context.findElement(mobileBy)
        }

        override fun toString(): String = "accessibility id: $accessibilityId"
    }

    /**
     * Search using UIAutomator (Android only).
     *
     * Uses MobileBy.AndroidUIAutomator strategy.
     */
    class AndroidUIAutomator(uiAutomatorExpression: String?) : By() {
        private val uiAutomatorExpression: String?
        private val mobileBy: By

        init {
            requireNotNull(uiAutomatorExpression) { "Element by UIAutomator is not specified and is null." }
            this.uiAutomatorExpression = uiAutomatorExpression
            this.mobileBy = MobileBy.AndroidUIAutomator(uiAutomatorExpression)
        }

        override fun findElements(context: SearchContext): List<WebElement> {
            return context.findElements(mobileBy)
        }

        override fun findElement(context: SearchContext): WebElement {
            return context.findElement(mobileBy)
        }

        override fun toString(): String = "android uiautomator: $uiAutomatorExpression"
    }

    /**
     * Search using Class Chain (iOS only).
     *
     * Uses MobileBy.iOSClassChain strategy.
     */
    class IOSClassChain(classChainExpression: String?) : By() {
        private val classChainExpression: String?
        private val mobileBy: By

        init {
            requireNotNull(classChainExpression) { "Element by Class Chain is not specified and is null." }
            this.classChainExpression = classChainExpression
            this.mobileBy = MobileBy.iOSClassChain(classChainExpression)
        }

        override fun findElements(context: SearchContext): List<WebElement> {
            return context.findElements(mobileBy)
        }

        override fun findElement(context: SearchContext): WebElement {
            return context.findElement(mobileBy)
        }

        override fun toString(): String = "ios class chain: $classChainExpression"
    }

    /**
     * Search using Predicate String (iOS only).
     *
     * Uses MobileBy.iOSNsPredicateString strategy.
     */
    class IOSPredicateString(predicateExpression: String?) : By() {
        private val predicateExpression: String?
        private val mobileBy: By

        init {
            requireNotNull(predicateExpression) { "Element by Predicate String is not specified and is null." }
            this.predicateExpression = predicateExpression
            this.mobileBy = MobileBy.iOSNsPredicateString(predicateExpression)
        }

        override fun findElements(context: SearchContext): List<WebElement> {
            return context.findElements(mobileBy)
        }

        override fun findElement(context: SearchContext): WebElement {
            return context.findElement(mobileBy)
        }

        override fun toString(): String = "ios predicate string: $predicateExpression"
    }
}
