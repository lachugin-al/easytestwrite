# Easy test write

Фреймворк для автоматизации тестирования мобильных приложений WB

Использование: 
   - для автоматизации end-to-end тестирования пользовательских сценариев мобильного приложения
   - для автоматизации тестирования аналитики

## Настройка окружения перед началом работы:
1. Установить `JDK 21` - [инструкция](https://www.oracle.com/java/technologies/downloads/#java21)
2. Открыть `Terminal`
3. Установить переменные сруды окружения `ANDROID_SDK_ROOT, ANDROID_HOME, JAVA_HOME`
6. Установить `Appium` - [инструкция](https://github.com/appium/appium)
7. Установить нативные драйвера для Android и iOS: 
   1. Android - [инструкция](https://github.com/appium/appium-uiautomator2-driver)
   2. iOS - [инструкция](https://github.com/appium/appium-xcuitest-driver)
9. Открыть проект в `Android Studio`
10. Перейти в `Device Manager` и нажать + `Create Virtual Device`, далее по шагам выбрать `Pixel 2` и `API 34`
11. Установить `Xcode`
12. Открыть `Xcode`, в меню выбрать `Open Developer Tool -> Simulator`, далее создать `iPhone 16 Plus`

## Структура проекта
Вся кодовая база находится в папке `src/main/` и разделена по пакетам:
* `app` - классы для подключения и запуска приложения для тестирования
* `controller` - классы с контроллерами
* `dsl` - классы DSL
* `events` - классы работы с событиями
* `proxy` - классы прокси сервера
* `utils` - классы с утилитами

Все тесты находятся в папке `src/test/` и разделены по пакетам:
* `mobile`, `web`, `api` - пакеты с тестами
  * `uipages` - классы PageObject модели разметки приложения
  * `uitests` - классы с UI тестами

## Локальный запуск E2E тестов
1. Необходимо запустить Appium сервер, в терминале выполнить команду `appium server --allow-cors`

### Локальный запуск E2E тестов на Android либо iOS:
1. В папке `test/resources` создать файл конфигурации `config.properties`:
2. Заполнить конфигурационные настройки (либо будут применены настройки по умолчанию)
```config.properties
   appium.url=http://localhost:4723/
   platform=ANDROID
   android.version=15
   ios.version=18.4
   android.device.name=Pixel_2_API_34
   ios.device.name=iPhone 16 Plus
   android.app.name=android.apk
   ios.app.name=ios.app
   app.activity=ru.wildberries.view.main.MainActivity
   app.package=com.wildberries.ru.dev
```
3. Выбрать папку с тестами или отдельные тесты для запуска, вызвать меню и нажать `Run ...`

## Написание нового теста с использованием фреймворка
### Структура UI теста
Тесты должны находиться в папке `test/kotlin/uitests/` и использовать PageObject модель с разметкой элементов.
Тесты необходимо разделять по пакетам, в зависимости от того, к какому функционалу относится тот или иной тест.
Каждый тест должен помечаться аннотацией `@Test`.

Пример структуры теста:
```kotlin
class ExampleTest : MobileTest() {

    @Test
    @DisplayName("Название теста")
    @Description("Описание того, что должен делать тест, если не достаточно информации из названия")
    @Feature("Название фичи")
    fun openAppExample() {
        context.run {
            "Загрузка стартового экрана приложения" {
              // ...
              "Проверка 1" {
                 // ...
              }
            }
            
            onlyAndroid {
                "Шаг выполняемый только на Android" {
                    // ...
                    "Проверка 1" {
                        // ...
                    }
                }
            }

            onlyIos {
                "Шаг выполняемый только на iOS" {
                    // ...
                    "Проверка 1" {
                        // ...
                    }
                }
            }

            optional {
                "Опциональный шаг, если не выполнится то тест не упадет" {
                    // ...
                    "Проверка отображения виджета который 50/50 отобразится" {
                        // ...
                    }
                }
            }
        }
    }
}
```

Пример теста:
```kotlin
class SmokeTest : MobileTest() {

    @Test
    @DisplayName("Название теста")
    @Description("Описание того, что должен делать тест, если не достаточно информации из названия")
    @Feature("Название фичи, тесты будут группироваться в отчете по фичам")
    fun openAppExample() {
        context.run {
            "Загрузка приложения" {
                "Экран с выбором региона отображается" {
                    checkVisible(ExampleScreen.ruRegion)
                }
            }

            "Выбрать регион" {
                click(ExampleScreen.ruRegion)
               
                "Кнопка 'Home' на навигационной панели отображается" {
                    checkVisible(ExampleScreen.homeNavBar)
                }
                "Кнопка 'Catalog' на навигационной панели отображается" {
                    checkVisible(ExampleScreen.catNavBar)
                }
                "Кнопка 'Cart' на навигационной панели отображается" {
                    checkVisible(ExampleScreen.cartNavBar)
                }
                "Кнопка 'Profile' на навигационной панели отображается" {
                    checkVisible(ExampleScreen.profileNavBar)
                }
            }
           
            "Пробежимся по навбару" {
                click(ExampleScreen.profileNavBar)
                click(ExampleScreen.cartNavBar)
                click(ExampleScreen.catNavBar)
                click(ExampleScreen.homeNavBar)
            }

            "Проскроллим туда-сюда" {
                scrollDown(scrollCount = 5)
                scrollUp(scrollCount = 2)
            }
        }
    }
}
```

### Перед и после каждого теста выполняются шаги setUp() и tearDown()
```kotlin
    // Функционал выполняемый перед каждым тестом
    @BeforeEach
    fun setUp() {
       // опционально, если необходимо выполнить какие-либо подготовительные действия перед прохождением тестов
    }

    // Функционал выполняемый после каждого теста
    @AfterEach
    fun tearDown() {
       // Ожидает все ассинхронные проверки по ивентам, особенно полезно когда тест уже закончился, но проверки еще нет
       awaitAllEventChecks()
       app.close()
    }
```



### Описание структуры PageObject
В каждый метод нам необходимо передавать локатор элемента на странице который описывается в PageObject модели.
Каждый элемент относится к отпределенному классу в PageObject модели соответствующий определенному виджету, экрану и т.д.
PageObject модель должна располагаться в папке `test/kotlin/uipages/` и разделена по пакетам, которые относятся к тому иил иному функционалу.

Пример описания элемента в PageObject модели:
```kotlin
object ExampleScreen {

    val ruRegionRussiaText = PageElement(
        android = XPath("""//android.widget.TextView[@text="Россия"]"""),
        ios = Value("Россия")
    )

    val homeNavBar = PageElement(
        android = ResourceId("HomeNavBar"),
        ios = Name("Main page")
    )

    val findOfferBar = PageElement(
        android = Text("Найти товар"),
        ios = null
    )

    val favorites = PageElement(
        android = ContentDesc("Добавить в отложенные"),
        ios = Name("IconDual/24/Stroke/heartEmptyWhiteBlack")
    )
}
```
Поиск элементов на странице доступно по аттрибутам: `id`, `resource-id`, `text`, `contains-text`, `content-desc`, `xpath`, `value`, `name`, `label`

### Описание методов (к каждой функции добавлена документация с подробным описанием)
 - Методы извлечения данных:
   - `getText()` - Получить текст из [element] найденном на экране;
   - `getPrice()` - Получить цену из [element] найденном на экране;
   - `getAttributeValue()` - Получить значение [attribute] из [element] найденном на экране;
 - StepContext:
   - `click()` - Найти элемент на экране по его [element] и кликнуть по нему;
   - `typeText()` - Найти элемент на экране по его [element] и ввести [text];
   - `tapArea()` - Нажать в области экрана по [x] и [y];
   - `tapElementArea()` - Нажать в области [element] по его [x] и [y];
   - `scrollDown()` - Выполнить скроллирование вниз;
   - `scrollUp()` - Выполнить скроллирование вверх;
   - `scrollRight()` - Выполнить скроллирование вправо;
   - `scrollLeft()` - Выполнить скроллирование влево;
   - `swipeDown()` - Выполнить свайп в [element] вниз;
   - `swipeUp()` - Выполнить свайп в [element] вверх;
   - `swipeRight()` - Выполнить свайп в [element] вправо;
   - `swipeLeft()` - Выполнить свайп в [element] влево;
   - `openDeeplink()` - Выполняет открытие переданной в параметры ссылки диплинка;
 - ExpectationContext:
   - `checkVisible()` - Проверить виден ли [element] на экране;
   - `checkHasEvent()` - Проверяет наличие события и данных в EventStorage;
   - `checkHasEventAsync()` - Проверяет событие и данные асинхронно в EventStorage исходя из производимых после вызова функции действий пользователя;

### Описание возможных параметров, допустимых для передачи в функции (к каждой параметру добавлена документация с подробным описанием):
   - `element: PageElement?` - элемент;
   - `elementNumber: Int?` - номер найденного элемента начиная с 1;
   - `timeoutBeforeExpectation: Long` - количество секунд, до того как будет производиться поиск элемента;
   - `timeoutExpectation: Long` - количество секунд в течении которого производится поиск элемента;
   - `pollingInterval: Long` - частота опроса элемента в миллисекундах;
   - `scrollCount: Int` - количество скроллирований до элемента, если элемент не найден на текущей странице;
   - `scrollCapacity: Double` - модификатор высота скролла [0.0 - 1.0], при 1.0 проскроллирует экран на 1 страницу;
   - `scrollDirection: ScrollDirection` - направление скроллировая экрана;
   - `x: Int`, `y: Int` - передача точки `x` и `y` на экране для функций `tapArea()` и `tapElementArea()`;

## Allure
В ходе прогонов, генерируется отчет и записывается в Allure
Для генерации отчета в командной строке необходимо выполнить команду `allure serve build/allure-results`