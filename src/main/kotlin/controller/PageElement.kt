package controller

import app.AppConfig
import app.Platform
import org.openqa.selenium.By
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebElement
import org.openqa.selenium.internal.FindsByXPath

/**
 * Универсальный дескриптор элемента страницы.
 *
 * Объединяет способы поиска элементов для разных платформ:
 * Android ([By]), iOS ([By]) и Web (селектор в формате [String]).
 *
 * Используется для построения кроссплатформенных тестов без необходимости дублирования локаторов.
 *
 * @property android Локатор для платформы Android.
 * @property ios Локатор для платформы iOS.
 * @property web Селектор для Web-платформы (CSS/XPath).
 */
data class PageElement(
    private val android: By?,
    private val ios: By?,
    private val web: String?
) {
    /**
     * Получить локатор для текущей платформы.
     *
     * @return Локатор типа [By] для мобильных платформ или [String] для Web.
     */
    fun get(): Any? = when (AppConfig.getPlatform()) {
        Platform.ANDROID -> android
        Platform.IOS -> ios
        Platform.WEB -> web
    }

    private companion object {
        /**
         * Формирует полный идентификатор ресурса для Android на основе имени элемента.
         */
        fun fullPackageId(value: String) = "${AppConfig.getAppPackage()}:id/$value"
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
     * Поиск по частичному совпадению текста элемента.
     */
    class ContainsText(text: String?) : BaseBy(text, "text") {
        override fun buildXPath(value: String): String {
            return ".//*[contains(@text,'$value')]"
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
}
