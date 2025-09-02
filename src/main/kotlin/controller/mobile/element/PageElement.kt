package controller.mobile.element

import app.config.AppConfig
import app.model.Platform
import io.appium.java_client.MobileBy
import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement
import org.openqa.selenium.internal.FindsByXPath

/**
 * Универсальный дескриптор элемента страницы.
 *
 * Объединяет способы поиска элементов для разных платформ:
 * Android ([By]), iOS ([By]).
 *
 * Поддерживает специальные стратегии поиска для мобильных платформ:
 * - Accessibility ID (Android и iOS)
 * - UIAutomator (только Android)
 * - iOS Class Chain (только iOS)
 * - iOS Predicate String (только iOS)
 *
 * Используется для построения кроссплатформенных тестов без необходимости дублирования локаторов.
 *
 * @property android Локатор для платформы Android.
 * @property ios Локатор для платформы iOS.
 */
data class PageElement(
    private val android: By? = null,
    private val ios: By? = null,
    private val androidList: List<By>? = null,
    private val iosList: List<By>? = null,
) {
    /**
     * Получить локатор для текущей платформы.
     *
     * @return Локатор типа [By] для мобильных платформ или [String] для Web.
     */
    fun get(): Any? = when (AppConfig.getPlatform()) {
        Platform.ANDROID -> android ?: androidList?.firstOrNull() ?: error("Локатор для Android не задан")
        Platform.IOS -> ios ?: iosList?.firstOrNull() ?: error("Локатор для iOS не задан")
    }

    /**
     * Получить список локаторов для текущей платформы.
     *
     * @return Список локаторов типа [By] для мобильных платформ или [String] для Web.
     */
    fun getAll(): List<Any>? = when (AppConfig.getPlatform()) {
        Platform.ANDROID -> androidList ?: android?.let { listOf(it) }
        Platform.IOS -> iosList ?: ios?.let { listOf(it) }
    }

    companion object {
        /**
         * Формирует полный идентификатор ресурса для Android на основе имени элемента.
         */
        private fun fullPackageId(value: String) = "${AppConfig.getAppPackage()}:id/$value"

        /**
         * Создает PageElement с локатором по accessibility id для обеих платформ.
         *
         * @param accessibilityId Значение accessibility id.
         * @return PageElement с локатором по accessibility id.
         */
        fun byAccessibilityId(accessibilityId: String): PageElement {
            val locator = AccessibilityId(accessibilityId)
            return PageElement(android = locator, ios = locator)
        }

        /**
         * Создает PageElement с локатором по accessibility id для Android.
         *
         * @param accessibilityId Значение accessibility id.
         * @return PageElement с локатором по accessibility id для Android.
         */
        fun byAndroidAccessibilityId(accessibilityId: String): PageElement {
            return PageElement(android = AccessibilityId(accessibilityId))
        }

        /**
         * Создает PageElement с локатором по accessibility id для iOS.
         *
         * @param accessibilityId Значение accessibility id.
         * @return PageElement с локатором по accessibility id для iOS.
         */
        fun byIOSAccessibilityId(accessibilityId: String): PageElement {
            return PageElement(ios = AccessibilityId(accessibilityId))
        }

        /**
         * Создает PageElement с локатором по UIAutomator для Android.
         *
         * @param uiAutomatorExpression Выражение для UIAutomator.
         * @return PageElement с локатором по UIAutomator для Android.
         */
        fun byAndroidUIAutomator(uiAutomatorExpression: String): PageElement {
            return PageElement(android = AndroidUIAutomator(uiAutomatorExpression))
        }

        /**
         * Создает PageElement с локатором по Class Chain для iOS.
         *
         * @param classChainExpression Выражение для Class Chain.
         * @return PageElement с локатором по Class Chain для iOS.
         */
        fun byIOSClassChain(classChainExpression: String): PageElement {
            return PageElement(ios = IOSClassChain(classChainExpression))
        }

        /**
         * Создает PageElement с локатором по Predicate String для iOS.
         *
         * @param predicateExpression Выражение для Predicate String.
         * @return PageElement с локатором по Predicate String для iOS.
         */
        fun byIOSPredicateString(predicateExpression: String): PageElement {
            return PageElement(ios = IOSPredicateString(predicateExpression))
        }

        /**
         * Создает PageElement со списком локаторов для Android.
         *
         * @param locators Список локаторов для Android.
         * @return PageElement со списком локаторов для Android.
         */
        fun byAndroidLocators(locators: List<By>): PageElement {
            return PageElement(androidList = locators)
        }

        /**
         * Создает PageElement со списком локаторов для iOS.
         *
         * @param locators Список локаторов для iOS.
         * @return PageElement со списком локаторов для iOS.
         */
        fun byIOSLocators(locators: List<By>): PageElement {
            return PageElement(iosList = locators)
        }

        /**
         * Создает PageElement со списком локаторов для всех платформ.
         *
         * @param androidLocators Список локаторов для Android.
         * @param iosLocators Список локаторов для iOS.
         * @return PageElement со списком локаторов для всех платформ.
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
     * Базовый класс для создания собственных локаторов через XPath.
     *
     * Реализует стандартные методы поиска элементов через [SearchContext].
     *
     * @param value Значение, по которому производится поиск.
     * @param valueType Тип значения для описания (например: id, text, label).
     */
    abstract class BaseBy(private val value: String?, private val valueType: String) : By() {
        init {
            requireNotNull(value) { "Элемент по $valueType не задан и имеет значение null." }
        }

        /**
         * Построение XPath-выражения на основе переданного значения.
         *
         * @param value Значение для подстановки в XPath.
         * @return Сформированная строка XPath.
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

    /**
     * Поиск по id элемента.
     */
    class Id(id: String?) : BaseBy(id, "id") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@id,'$value') or contains(@id,'${fullPackageId(value)}')]"
        }
    }

    /**
     * Поиск по resource-id (Android).
     */
    class ResourceId(resourceId: String?) : BaseBy(resourceId, "resource-id") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@resource-id,'$value') or contains(@resource-id,'${fullPackageId(value)}')]"
        }
    }

    /**
     * Поиск по точному текстовому значению элемента.
     */
    class Text(text: String?) : BaseBy(text, "text") {
        override fun buildXPath(value: String): String {
            return ".//*[@text = '$value']"
        }
    }

    /**
     * Поиск по частичному совпадению пути элемента.
     */
    class Contains(text: String?) : BaseBy(text, "text") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@text,'$value') or contains(@id,'$value') or contains(@resource-id,'$value') or contains(@content-desc,'$value') or contains(@name,'$value') or contains(@label,'$value') or contains(@value,'$value')]"
        }
    }

    /**
     * Поиск по точному совпадению пути элемента.
     */
    class ExactMatch(text: String?) : BaseBy(text, "text") {
        override fun buildXPath(value: String): String {
            return ".//*[(@text='$value' or @id='$value' or @resource-id='$value' or @content-desc='$value' or @name='$value' or @label='$value' or @value='$value')]"
        }
    }

    /**
     * Поиск по атрибуту content-desc (Android).
     */
    class ContentDesc(contentDesc: String?) : BaseBy(contentDesc, "content-desc") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@content-desc,'$value')]"
        }
    }

    /**
     * Поиск с использованием произвольного XPath.
     */
    class XPath(xpathExpression: String?) : BaseBy(xpathExpression, "xpath") {
        override fun buildXPath(value: String): String = value
    }

    /**
     * Поиск по значению атрибута value (iOS).
     */
    class Value(value: String?) : BaseBy(value, "value") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@value,'$value')]"
        }
    }

    /**
     * Поиск по атрибуту name (iOS).
     */
    class Name(name: String?) : BaseBy(name, "name") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@name,'$value')]"
        }
    }

    /**
     * Поиск по атрибуту label (iOS).
     */
    class Label(label: String?) : BaseBy(label, "label") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@label,'$value')]"
        }
    }

    /**
     * Поиск по accessibility id (Android и iOS).
     *
     * Использует стратегию поиска MobileBy.AccessibilityId.
     */
    class AccessibilityId(accessibilityId: String?) : By() {
        private val accessibilityId: String?
        private val mobileBy: By

        init {
            requireNotNull(accessibilityId) { "Элемент по accessibility id не задан и имеет значение null." }
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
     * Поиск с использованием UIAutomator (только для Android).
     *
     * Использует стратегию поиска MobileBy.AndroidUIAutomator.
     */
    class AndroidUIAutomator(uiAutomatorExpression: String?) : By() {
        private val uiAutomatorExpression: String?
        private val mobileBy: By

        init {
            requireNotNull(uiAutomatorExpression) { "Элемент по UIAutomator не задан и имеет значение null." }
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
     * Поиск с использованием Class Chain (только для iOS).
     *
     * Использует стратегию поиска MobileBy.iOSClassChain.
     */
    class IOSClassChain(classChainExpression: String?) : By() {
        private val classChainExpression: String?
        private val mobileBy: By

        init {
            requireNotNull(classChainExpression) { "Элемент по Class Chain не задан и имеет значение null." }
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
     * Поиск с использованием Predicate String (только для iOS).
     *
     * Использует стратегию поиска MobileBy.iOSNsPredicateString.
     */
    class IOSPredicateString(predicateExpression: String?) : By() {
        private val predicateExpression: String?
        private val mobileBy: By

        init {
            requireNotNull(predicateExpression) { "Элемент по Predicate String не задан и имеет значение null." }
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
