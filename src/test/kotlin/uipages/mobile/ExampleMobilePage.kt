package uipages.mobile

import controller.mobile.element.PageElement
import controller.mobile.element.PageElement.*

/**
 * Тестовая страница с разметкой
 */
object ExampleMobilePage {

    /**
     * Наименование страны "Россия"
     */
    /*val ruRegionRussiaText = PageElement(
        androidList = listOf(Text("Russia"), Text("Россия")),
        ios = Name("Россия")
    )*/

    val ruRegionRussiaText = PageElement(
        android = Text("Россия"),
        ios = Name("Россия")
    )

    /**
     * Нижняя навигационная панель, кнопка "Home"
     */
    val homeNavBar = PageElement(
        android = ResourceId("homeNavBar"),
        ios = Name("[tabMain]")
    )

    /**
     * Нижняя навигационная панель, кнопка "Catalogue"
     */
    val catNavBar = PageElement(
        android = ResourceId("catNavBar"),
        ios = Name("[tabCatalog]")
    )

    /**
     * Нижняя навигационная панель, кнопка "Cart"
     */
    val cartNavBar = PageElement(
        android = ResourceId("cartNavBar"),
        ios = Name("[cart]")
    )

    /**
     * Нижняя навигационная панель, кнопка "Profile"
     */
    val profileNavBar = PageElement(
        android = ResourceId("profileNavBar"),
        ios = Name("[tabPersonal]")
    )

    /**
     * Welcome Onboarding Screen
     */
    val onboardingSkipButton = PageElement(
        ios = Name("WBDButton")
    )

    /**
     * Поп-ап с разрешением на отправку Пуш-уведомлений
     */
    val pushAlertAllow = PageElement(
        ios = Name("Нет, спасибо")
    )

    /**
     * Поп-ап с просьбой обновить приложение
     */
    val notNow = PageElement(
        android = Text("Не сейчас")
    )

    /**
     *Поле поиска
     */
    val searchBar = PageElement(
        android= ContentDesc("Поиск"),
        ios = XPath("//XCUIElementTypeStaticText[@name='Поиск']")
    )

    /**
     *Активное поле поиска
     */
    val activeSearchBar = PageElement(
        android= XPath("//android.widget.EditText"),
        ios = Name("[MainPageVC][mainPageSearchFieldId]")
    )

    /**
     *Первый товар в выдаче поиска
     */
    val tapToItemOnSearchPage = PageElement(
        android= ContentDesc("Изображение товара"),
        ios = Name("[CatalogProductsVC][collectionView][products][0]")
    )

    /**
     *Кнопка добавить в корзину с карточки товара
     */
    val addToCartButtonInItem = PageElement(
        android = Text("В корзину"),
        ios = Name("[ProductCardViewController][addToBasket]")
    )

    /**
     *Первый размер при добавлении товара в корзину
     */
    val tapToSizeInItem = PageElement(
        android=XPath("//android.widget.ScrollView/android.view.View[1]"),
        ios = Name("[ProductCardSelectSizeView][collectionView][sizeCell]")
    )

    /**
     * Подпишитесь на наши обновления "Разрешить уведомления"
     */
    val allowNotice = PageElement(
        ios = Name("[PushAlertVC][acceptButton]")
    )

    /**
     * Подпишитесь на наши обновления "Нет, спасибо"
     */
    val dismissNotice = PageElement(
        ios = Name("WBDButton")
    )

    val androidUIAutomatorLocator = PageElement(
        android = AndroidUIAutomator("new UiSelector().text(\"Поиск\")")
    )

    val iOSClassChainLocator = PageElement(
        ios = IOSClassChain("**/XCUIElementTypeStaticText[`name == \"Поиск\"`]")
    )

    val iOSPredicateStringLocator = PageElement(
        ios = IOSPredicateString("name == \"Поиск\"")
    )

    val accessebilityIdLocator = PageElement(
        android = AccessibilityId("Поиск"),
        ios = AccessibilityId("Поиск")
    )

    /**
     *Поле сортировки
     */
    val sortingField = PageElement(
        android = AccessibilityId("Сортировка"),
        ios = Name("[CatalogProductsVC][sortButton]")
    )

    /**
     *Выбор сортировки дешевле
     */
    val selectSortingByPriceDown = PageElement(
        android = XPath("//androidx.compose.ui.platform.ComposeView/android.view.View/android.view.View/android.view.View[6]"),
        ios = Label("Дешевле")
    )

    /**
     *Кнопка применить в сортировке
     */
    val acceptButtonInSorting = PageElement(
        android = Text("Применить"),
        ios = Label("Применить")
    )

    /**
     *Поле поиска
     */
    val searchbar = PageElement(
        android = Text("Поиск"),
        ios = Name("Поиск")
    )

    /**
     *Поле поиска
     */
    val searchbar2 = PageElement(
        android = ContentDesc("Поиск"),
        ios = Name("Поиск")
    )

    // Слишком длинный и не стабильный локатор - избегать таких конструкций!!!
    /**
     *Поисковой тег на экране поиска
     */
    val selectSearchTag = PageElement(
        android = XPath("//android.widget.FrameLayout[@resource-id='com.wildberries.ru.dev:id/fragmentTabContainer']/androidx.compose.ui.platform.ComposeView/android.view.View/android.view.View/android.view.View[1]/android.view.View/android.view.View[2]"),
        ios = XPath("(//XCUIElementTypeCell[@name='[SearchResultsViewController][tableView][commonSuggest]'])[1]")
    )
}
