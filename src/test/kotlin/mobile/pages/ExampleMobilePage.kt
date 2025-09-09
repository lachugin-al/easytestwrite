package mobile.pages

import controller.mobile.element.PageElement
import controller.mobile.element.PageElement.*

/**
 * Test page with markup/layout
 */
object ExampleMobilePage {

    /**
     * Country name "Russia"
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
     * Bottom navigation bar, "Home" button
     */
    val homeNavBar = PageElement(
        android = ResourceId("homeNavBar"),
        ios = Name("[tabMain]")
    )

    /**
     * Bottom navigation bar, "Catalogue" button
     */
    val catNavBar = PageElement(
        android = ResourceId("catNavBar"),
        ios = Name("[tabCatalog]")
    )

    /**
     * Bottom navigation bar, "Cart" button
     */
    val cartNavBar = PageElement(
        android = ResourceId("cartNavBar"),
        ios = Name("[cart]")
    )

    /**
     * Bottom navigation bar, "Profile" button
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
     * Push notification permission pop-up
     */
    val pushAlertAllow = PageElement(
        ios = Name("Нет, спасибо")
    )

    /**
     * Pop-up prompting to update the app
     */
    val notNow = PageElement(
        android = Text("Не сейчас")
    )

    /**
     * Search field
     */
    val searchBar = PageElement(
        android= ContentDesc("Поиск"),
        ios = XPath("//XCUIElementTypeStaticText[@name='Поиск']")
    )

    /**
     * Active search field
     */
    val activeSearchBar = PageElement(
        android= XPath("//android.widget.EditText"),
        ios = Name("[MainPageVC][mainPageSearchFieldId]")
    )

    /**
     * First item in search results
     */
    val tapToItemOnSearchPage = PageElement(
        android= ContentDesc("Изображение товара"),
        ios = Name("[CatalogProductsVC][collectionView][products][0]")
    )

    /**
     * "Add to cart" button on the product page
     */
    val addToCartButtonInItem = PageElement(
        android = Text("В корзину"),
        ios = Name("[ProductCardViewController][addToBasket]")
    )

    /**
     * First size when adding a product to the cart
     */
    val tapToSizeInItem = PageElement(
        android=XPath("//android.widget.ScrollView/android.view.View[1]"),
        ios = Name("[ProductCardSelectSizeView][collectionView][sizeCell]")
    )

    /**
     * "Subscribe to our updates" — "Allow notifications"
     */
    val allowNotice = PageElement(
        ios = Name("[PushAlertVC][acceptButton]")
    )

    /**
     * "Subscribe to our updates" — "No, thanks"
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
     * Sorting field
     */
    val sortingField = PageElement(
        android = AccessibilityId("Сортировка"),
        ios = Name("[CatalogProductsVC][sortButton]")
    )

    /**
     * Choose sorting by cheaper (price descending)
     */
    val selectSortingByPriceDown = PageElement(
        android = XPath("//androidx.compose.ui.platform.ComposeView/android.view.View/android.view.View/android.view.View[6]"),
        ios = Label("Дешевле")
    )

    /**
     * "Apply" button in sorting
     */
    val acceptButtonInSorting = PageElement(
        android = Text("Применить"),
        ios = Label("Применить")
    )

    /**
     * Search field
     */
    val searchbar = PageElement(
        android = Text("Поиск"),
        ios = Name("Поиск")
    )

    /**
     * Search field
     */
    val searchbar2 = PageElement(
        android = ContentDesc("Поиск"),
        ios = Name("Поиск")
    )

    // Locator is too long and unstable — avoid such constructions!!!
    /**
     * Search tag on the search screen
     */
    val selectSearchTag = PageElement(
        android = XPath("//android.widget.FrameLayout[@resource-id='com.wildberries.ru.dev:id/fragmentTabContainer']/androidx.compose.ui.platform.ComposeView/android.view.View/android.view.View/android.view.View[1]/android.view.View/android.view.View[2]"),
        ios = XPath("(//XCUIElementTypeCell[@name='[SearchResultsViewController][tableView][commonSuggest]'])[1]")
    )
}
