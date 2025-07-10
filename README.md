# Easy test write

Фреймворк для автоматизации тестирования мобильных и веб-приложений WB

Использование: 
   - для автоматизации end-to-end тестирования пользовательских сценариев мобильного приложения
   - для автоматизации тестирования аналитики
   - для автоматизации веб-тестирования с использованием Playwright
   - для создания параметризованных тестов с различными источниками данных

## Настройка окружения перед началом работы:
1. Установить `JDK 21` - [инструкция](https://www.oracle.com/java/technologies/downloads/#java21)
2. Открыть `Terminal`
3. Установить переменные среды окружения `ANDROID_SDK_ROOT, ANDROID_HOME, JAVA_HOME`
4. Установить `Appium` - [инструкция](https://github.com/appium/appium)
5. Установить нативные драйвера для Android и iOS: 
   1. Android - [инструкция](https://github.com/appium/appium-uiautomator2-driver)
   2. iOS - [инструкция](https://github.com/appium/appium-xcuitest-driver)
6. Установить `FFmpeg` для записи видео:
   - Mac: `brew install ffmpeg`
   - Linux: `sudo apt install ffmpeg`
   - Windows: `choco install ffmpeg` или `winget install ffmpeg`
7. Открыть проект в `Android Studio`
8. Перейти в `Device Manager` и нажать + `Create Virtual Device`, далее по шагам выбрать `Pixel 2` и `API 34`
9. Установить `Xcode` (только для macOS)
10. Открыть `Xcode`, в меню выбрать `Open Developer Tool -> Simulator`, далее создать `iPhone 16 Plus`

## Структура проекта
Вся кодовая база находится в папке `src/main/` и разделена по пакетам:
* `app` - классы для подключения и запуска приложения для тестирования
  * `config` - классы конфигурации
  * `driver` - классы драйверов для Android, iOS и Web
  * `model` - модели данных
* `controller` - классы с контроллерами
  * `mobile` - контроллеры для мобильных тестов
  * `element` - классы для работы с элементами UI
  * `handler` - обработчики специфических взаимодействий (алерты и др.)
* `dsl` - классы DSL для написания тестов
* `events` - классы работы с событиями и аналитикой
* `proxy` - классы прокси сервера для перехвата и анализа сетевого трафика
* `utils` - классы с утилитами
  * `allure` - расширения для Allure отчетов
  * `video` - классы для записи видео тестов

Все тесты находятся в папке `src/test/` и разделены по пакетам:
* `mobile`, `web`, `api` - пакеты с тестами
  * `uipages` - классы PageObject модели разметки приложения
  * `uitests` - классы с UI тестами
* `resources` - ресурсы для тестов (конфигурация, тестовые данные)

## Локальный запуск E2E тестов

### Подготовка к запуску
1. Для мобильных тестов необходимо запустить Appium сервер, в терминале выполнить команду:
   ```bash
   appium server --allow-cors
   ```

2. Для веб-тестов с использованием Playwright установка дополнительных компонентов происходит автоматически при первом запуске.

### Локальный запуск E2E тестов на Android, iOS или Web:
1. В папке `test/resources` создать файл конфигурации `config.properties`
2. Заполнить конфигурационные настройки (либо будут применены настройки по умолчанию)
```config.properties
   # URL-адрес сервера Appium для подключения к мобильным устройствам
    appium.url=http://localhost:4723/
    # Платформа для тестирования (ANDROID или iOS)
    platform=ANDROID
    # Версия операционной системы Android для тестирования
    android.version=15
    # Версия операционной системы iOS для тестирования
    ios.version=18.5
    # Название устройства Android для эмуляции
    android.device.name=Pixel_XL
    # Название устройства iOS для эмуляции
    ios.device.name=iPhone 16 Plus
    # Имя файла приложения для Android
    android.app.name=android.apk
    # Имя файла приложения для iOS
    ios.app.name=ios.app
    # Основная активность приложения Android
    app.activity=MainActivity
    # Идентификатор пакета приложения Android
    app.package=dev
    # Автоматическое принятие всплывающих уведомлений на iOS
    ios.auto_accept_alerts=true
    # Автоматическое отклонение всплывающих уведомлений на iOS
    ios.auto_dismiss_alerts=false
    # Режим работы Android-эмулятора без графического интерфейса
    android.headless.mode=true

    # Тип браузера для Playwright (инструмент для автоматизации веб-тестирования)
    playwright.browser.type=chromium
    # Режим работы Playwright без графического интерфейса
    playwright.headless=false

    # Включение/отключение записи видео для тестов на Android
    android.video.recording.enabled=false
    # Включение/отключение записи видео для тестов на iOS
    ios.video.recording.enabled=false
    # Размер видеозаписи тестов
    video.recording.size=640x360
    # Качество видеозаписи тестов (от 1 до 100)
    video.recording.quality=20
    # Битрейт видеозаписи тестов в битах в секунду
    video.recording.bitrate=50000
    # Директория для сохранения видеозаписей тестов
    video.recording.output.dir=build/videos

    # Настройки автоматического запуска и выключения эмулятора/симулятора
    # Автоматический запуск эмулятора/симулятора перед тестами
    emulator.auto.start=true
    # Автоматическое выключение эмулятора/симулятора после тестов
    emulator.auto.shutdown=true

```
3. Выбрать папку с тестами или отдельные тесты для запуска, вызвать меню и нажать `Run ...`

### Запуск тестов с тегами
Для запуска тестов с определенными тегами используйте параметр `-Ptag`:

```bash
./gradlew test -Ptag=Smoke,Regression
```

Это запустит только тесты, помеченные аннотацией `@Tag("Smoke")` или `@Tag("Regression")`.

### Запуск тестов с автоматическим управлением эмуляторами
Для автоматического запуска и остановки эмуляторов/симуляторов:

```bash
./gradlew test -Pemulator.auto.start=true -Pemulator.auto.shutdown=true
```

## Написание нового теста с использованием фреймворка
### Структура UI теста
Тесты должны находиться в папке `test/kotlin/uitests/` и использовать PageObject модель с разметкой элементов.
Тесты необходимо разделять по пакетам, в зависимости от того, к какому функционалу относится тот или иной тест.
Каждый тест должен помечаться аннотацией `@Test`.

### Типы тестов
Фреймворк поддерживает несколько типов тестов:

1. **Мобильные тесты** - наследуются от класса `MobileTest` и используются для тестирования мобильных приложений на Android и iOS
2. **Веб-тесты** - используют Playwright для тестирования веб-приложений
3. **Параметризованные тесты** - используют аннотации `@ParameterizedTest` и различные источники данных
4. **Тесты с тегами** - помечаются аннотацией `@Tag` для выборочного запуска

### Специальные блоки для организации тестов
Фреймворк предоставляет специальные блоки для организации тестов:

- `context.run { ... }` - Основной блок выполнения теста
- `optionalAndroid({ ... })` - Блок кода, который выполняется только на платформе Android
- `optionalIos({ ... })` - Блок кода, который выполняется только на платформе iOS
- `optional({ ... })` - Опциональный блок кода, если не выполнится, то тест не упадет (полезно для проверки элементов, которые могут отображаться не всегда)
- `onlyAndroid { ... }` - Блок кода, который выполняется только на платформе Android
- `onlyIos { ... }` - Блок кода, который выполняется только на платформе iOS

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

            optionalAndroid(
                {
                    "Шаг выполняемый только на Android" {
                        // ...
                        "Проверка 1" {
                            // ...
                        }
                    }
                }
            )

            optionalIos(
                {
                    "Шаг выполняемый только на iOS" {
                        // ...
                        "Проверка 1" {
                            // ...
                        }
                    }
                }
            )

            optional(
                {
                    "Опциональный шаг, если не выполнится то тест не упадет" {
                        // ...
                        "Проверка отображения виджета который 50/50 отобразится" {
                            // ...
                        }
                    }
                }
            )
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

### Жизненный цикл теста
Фреймворк поддерживает стандартные методы жизненного цикла JUnit 5:

```kotlin
// Выполняется один раз перед всеми тестами в классе
@BeforeAll
fun setUpAll() {
    // Инициализация общих ресурсов
}

// Выполняется перед каждым тестом
@BeforeEach
fun setUp(testInfo: TestInfo) {
    // Подготовка к тесту
    // testInfo содержит информацию о текущем тесте
}

// Выполняется после каждого теста
@AfterEach
fun tearDown(testInfo: TestInfo) {
    // Ожидает все асинхронные проверки по ивентам
    awaitAllEventChecks()
    // Закрытие приложения
    app.close()
    // Сохранение видеозаписи теста
}

// Выполняется один раз после всех тестов в классе
@AfterAll
fun tearDownAll() {
    // Освобождение общих ресурсов
}
```

Базовый класс `MobileTest` уже содержит реализацию этих методов, которые автоматически:
- Запускают и останавливают эмуляторы/симуляторы (если включено)
- Начинают и останавливают запись видео (если включено)
- Ожидают завершения асинхронных проверок событий
- Закрывают приложение после теста



### Описание структуры PageObject
В каждый метод нам необходимо передавать локатор элемента на странице который описывается в PageObject модели.
Каждый элемент относится к отпределенному классу в PageObject модели соответствующий определенному виджету, экрану и т.д.
PageObject модель должна располагаться в папке `test/kotlin/uipages/` и разделена по пакетам, которые относятся к тому иил иному функционалу.

Пример описания элемента в PageObject модели:
```kotlin
object ExampleScreen {

    // Стандартное определение элемента с разными локаторами для Android и iOS
    val ruRegionRussiaText = PageElement(
        android = XPath("""//android.widget.TextView[@text="Россия"]"""),
        ios = Value("Россия")
    )

    // Использование списка альтернативных локаторов для Android
    // Фреймворк попробует найти элемент по каждому локатору из списка
    val ruRegionRussiaTextWithAlternatives = PageElement(
        androidList = listOf(Text("Russia"), Text("Россия")),
        ios = Name("Россия")
    )

    val homeNavBar = PageElement(
        android = ResourceId("HomeNavBar"),
        ios = Name("Main page")
    )

    val findOfferBar = PageElement(
        android = Text("Найти товар"),
        ios = null  // Элемент доступен только на Android
    )

    val favorites = PageElement(
        android = ContentDesc("Добавить в отложенные"),
        ios = Name("IconDual/24/Stroke/heartEmptyWhiteBlack")
    )

    // Использование AccessibilityId для обеих платформ
    val accessebilityIdLocator = PageElement(
        android = AccessibilityId("Поиск"),
        ios = AccessibilityId("Поиск")
    )

    // Использование UIAutomator для Android
    val androidUIAutomatorLocator = PageElement(
        android = AndroidUIAutomator("new UiSelector().text(\"Поиск\")")
    )

    // Использование Class Chain для iOS
    val iOSClassChainLocator = PageElement(
        ios = IOSClassChain("**/XCUIElementTypeStaticText[`name == \"Поиск\"`]")
    )

    // Использование Predicate String для iOS
    val iOSPredicateStringLocator = PageElement(
        ios = IOSPredicateString("name == \"Поиск\"")
    )
}
```

Поиск элементов на странице доступно по аттрибутам: `id`, `resource-id`, `text`, `contains-text`, `content-desc`, `xpath`, `value`, `name`, `label`, `accessibility-id`, `android-uiautomator`, `ios-class-chain`, `ios-predicate-string`

#### Расширенные возможности локаторов

Фреймворк поддерживает несколько продвинутых стратегий локации элементов:

1. **Альтернативные локаторы** - Возможность указать список альтернативных локаторов для Android с помощью параметра `androidList`. Фреймворк будет последовательно пытаться найти элемент по каждому локатору из списка.

2. **Платформо-зависимые локаторы** - Возможность указать разные локаторы для Android и iOS, что позволяет писать кросс-платформенные тесты.

3. **Специализированные локаторы** - Поддержка платформо-специфичных локаторов, таких как AndroidUIAutomator, IOSClassChain и IOSPredicateString, для более гибкого поиска элементов.

### Описание методов (к каждой функции добавлена документация с подробным описанием)
 - Методы извлечения данных:
   - `getText()` - Получить текст из [element] найденном на экране;
   - `getPrice()` - Получить цену из [element] найденном на экране;
   - `getAttributeValue()` - Получить значение [attribute] из [element] найденном на экране;
 - StepContext:
   - `click()` - Найти элемент на экране по его [element] и кликнуть по нему;
   - `click(eventName, eventData)` - Найти и кликнуть по элементу, связанному с событием [eventName] и данными [eventData];
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
   - `openDeeplink(deeplink)` - Выполняет открытие переданной в параметры ссылки диплинка (поддерживается для Android и iOS);
 - ExpectationContext:
   - `checkVisible()` - Проверить виден ли [element] на экране;
   - `checkHasEvent(eventName, eventData)` - Проверяет наличие события [eventName] и данных [eventData] в EventStorage;
   - `checkHasEvent(eventName, eventDataFile)` - Проверяет наличие события [eventName] и данных из файла [eventDataFile] в EventStorage;
   - `checkHasEventAsync(eventName, eventData)` - Проверяет событие [eventName] и данные [eventData] асинхронно в EventStorage исходя из производимых после вызова функции действий пользователя;
   - `checkHasEventAsync(eventName, eventDataFile)` - Проверяет событие [eventName] и данные из файла [eventDataFile] асинхронно в EventStorage;
 - Вспомогательные методы:
   - `awaitAllEventChecks()` - Ожидает завершения всех асинхронных проверок событий (вызывается в методе tearDown());

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
   - `eventName: String` - название события для поиска в EventStorage;
   - `eventData: String` - JSON-строка с данными для проверки в событии;
   - `eventDataFile: File` - файл с JSON-данными для проверки в событии;
   - `deeplink: String` - строка с диплинком для открытия в приложении.

## Работа с событиями и аналитикой
Фреймворк предоставляет мощные возможности для проверки событий аналитики, отправляемых приложением. Для этого используется класс `EventStorage`, который хранит все события, перехваченные прокси-сервером.

### Проверка событий
Для проверки событий используются методы:
- `checkHasEvent(eventName, eventData)` - синхронная проверка наличия события
- `checkHasEventAsync(eventName, eventData)` - асинхронная проверка, которая не блокирует выполнение теста
- `checkHasEvent(eventName, eventDataFile)` - проверка с данными из файла
- `checkHasEventAsync(eventName, eventDataFile)` - асинхронная проверка с данными из файла

#### Шаблоны для сопоставления значений в eventData

При проверке событий поддерживаются следующие шаблоны для значений в JSON:

- `"*"` - соответствует любому значению (wildcard)
- `""` - соответствует только пустому значению
- `"~value"` - проверяет частичное совпадение (если 'value' является подстрокой значения)
- Любое другое значение - проверяется точное соответствие

Примеры использования шаблонов:

```kotlin
// Проверка события с любым значением для поля "loc"
checkHasEvent(
    eventName = "view_item",
    eventData = """{"loc": "*"}"""
)

// Проверка события с пустым значением для поля "loc"
checkHasEvent(
    eventName = "view_item",
    eventData = """{"loc": ""}"""
)

// Проверка события, где значение поля "log" содержит подстроку "logsns"
checkHasEvent(
    eventName = "view_item",
    eventData = """{"log": "~partial_value"}"""
)

// Проверка события с точным совпадением значения поля "loc"
checkHasEvent(
    eventName = "view_item",
    eventData = """{"loc": "exact_value"}"""
)
```

#### Синхронная проверка событий

```kotlin
"Проверка отправки события при клике на товар" {
    checkHasEvent(
        eventName = "product_click",
        eventData = """
            {
                "product_id": "12345",
                "category": "electronics"
            }
        """
    )
}
```

#### Асинхронная проверка событий

```kotlin
"Проверка отправки события при клике на товар (асинхронно)" {
    checkHasEventAsync(
        eventName = "product_click",
        eventData = """
            {
                "product_id": "12345",
                "category": "electronics"
            }
        """
    )

    // Продолжение выполнения теста без ожидания проверки события
}
```

При использовании асинхронных проверок необходимо вызвать метод `awaitAllEventChecks()` в методе `tearDown()` для ожидания завершения всех проверок.

### Взаимодействие с элементами на основе событий

Фреймворк позволяет находить и взаимодействовать с элементами на основе связанных с ними событий аналитики:

```kotlin
"Нажимаем на товар, который отправляет определенное событие" {
    click(
        eventName = "view_item_in_list", 
        eventData = """{"loc": "SNS"}""", 
        scrollCount = 1, 
        eventPosition = "first"  // Можно указать "first" или "last"
    )
}
```

Это позволяет создавать более гибкие тесты, которые могут адаптироваться к изменениям в UI, но сохраняют привязку к бизнес-логике через события аналитики.

## Allure
В ходе прогонов, генерируется отчет и записывается в Allure. Фреймворк предоставляет расширенную интеграцию с Allure для улучшения организации и визуализации тестовых отчетов.

### Основные возможности Allure интеграции

- Автоматическая генерация отчетов о выполнении тестов
- Группировка тестов по фичам, эпикам и историям с помощью аннотаций `@Feature`, `@Epic`, `@Story`
- Детальное описание тестов с помощью аннотаций `@DisplayName` и `@Description`
- Организация тестов в наборы (suites) с помощью кастомной аннотации `@Suite`
- Прикрепление скриншотов, логов и видеозаписей к отчетам
- Отслеживание шагов выполнения теста с подробной информацией

### Использование аннотации Suite

Для группировки тестов в логические наборы используйте аннотацию `@Suite`:

```kotlin
@Suite("Название Suite")
@ExtendWith(AllureExtension::class)
class ExampleTest : MobileTest() {
    // Тесты
}
```

### Генерация отчета

Для генерации отчета в командной строке необходимо выполнить команду:

```bash
allure serve build/allure-results
```

## Расширенные возможности фреймворка

### Автоматическое управление эмуляторами и симуляторами

Фреймворк предоставляет встроенную поддержку автоматического управления жизненным циклом эмуляторов Android и симуляторов iOS через класс `EmulatorManager`. Это позволяет запускать и останавливать эмуляторы/симуляторы автоматически в процессе выполнения тестов.

#### Основные возможности

- Автоматический запуск эмулятора/симулятора перед началом тестирования
- Автоматическая остановка эмулятора/симулятора после завершения тестов
- Проверка работоспособности эмулятора/симулятора
- Обработка краевых случаев и ошибок
- Поддержка платформ Android и iOS

#### Настройка

В `config.properties` или через командную строку можно настроить следующие параметры:

```properties
# Автоматический запуск эмулятора/симулятора перед тестами
emulator.auto.start=true
# Автоматическое выключение эмулятора/симулятора после тестов
emulator.auto.shutdown=true
```

### Параметризованное тестирование

Фреймворк поддерживает параметризованное тестирование с использованием JUnit 5, что позволяет запускать один и тот же тест с разными входными данными.

#### Основные возможности

- Параметризация тестов с использованием встроенных источников данных
- Поддержка CSV-файлов как источников тестовых данных
- Автоматический перезапуск только упавших тестов с теми же параметрами

#### Пример использования

```kotlin
@ParameterizedTest(name = "Тест с параметром: {0}")
@ValueSource(strings = ["Значение1", "Значение2", "Значение3"])
@DisplayName("Параметризованный тест с встроенными параметрами")
fun testWithValueSource(param: String) {
    // Тело теста
}

@ParameterizedTest(name = "Тест с параметром из CSV: {0}")
@CsvFileSource(resources = ["/test_data.csv"], numLinesToSkip = 1)
@DisplayName("Параметризованный тест с параметрами из CSV")
fun testWithCsvSource(param: String) {
    // Тело теста
}
```

### Настройки записи видео

В проект добавлена возможность записи видео во время выполнения тестов с использованием класса `VideoRecorder`. Это позволяет записывать видео выполнения тестов на мобильных устройствах для отладки, документирования и демонстрации работы приложения.

#### Основные возможности

- Автоматическая запись видео во время выполнения тестов
- Настройка размера, качества и битрейта видео
- Интеграция с отчетами Allure
- Поддержка платформ Android и iOS (с ограничениями)

#### Доступные параметры

В `config.properties` или через командную строку можно настроить следующие параметры:

- `android.video.recording.enabled` - включение/отключение записи видео для Android (true/false)
- `ios.video.recording.enabled` - включение/отключение записи видео для iOS (true/false)
- `video.recording.size` - размер записываемого видео (например, "1280x720")
- `video.recording.quality` - качество записываемого видео (0-100)
- `video.recording.bitrate` - битрейт видео (бит/с)
- `video.recording.output.dir` - директория для сохранения видеозаписей

#### Использование

Параметры можно задать при запуске тестов через командную строку с помощью флага `-P`:

```bash
./gradlew test -Pandroid.video.recording.enabled=true -Pios.video.recording.enabled=true -Pvideo.recording.size=1280x720 -Pvideo.recording.quality=70 -Pvideo.recording.bitrate=1000000
```

Если параметры не заданы, будут использованы значения из файла `config.properties` или значения по умолчанию:

```properties
android.video.recording.enabled=true
ios.video.recording.enabled=true
video.recording.size=640x360
video.recording.quality=20
video.recording.bitrate=500000
video.recording.output.dir=build/videos
```

#### Пример использования в CI/CD

```yaml
# Пример для GitLab CI
test:
  script:
    - ./gradlew test -Pandroid.video.recording.enabled=true -Pios.video.recording.enabled=true -Pvideo.recording.size=1280x720 -Pvideo.recording.quality=70 -Pvideo.recording.output.dir=build/videos -Pvideo.recording.bitrate=500000
  artifacts:
    paths:
      - build/videos/
```

## Интеграция с Playwright для веб-тестирования

Фреймворк включает интеграцию с Playwright для автоматизации веб-тестирования. Это позволяет использовать единый подход к написанию тестов как для мобильных, так и для веб-приложений.

### Основные возможности

- Поддержка различных браузеров (Chromium, Firefox, WebKit)
- Режим headless для запуска без графического интерфейса
- Единый DSL для написания тестов
- Интеграция с Allure для отчетов

### Настройка

В `config.properties` или через командную строку можно настроить следующие параметры:

```properties
# Тип браузера для Playwright (chromium, firefox, webkit)
playwright.browser.type=chromium
# Режим работы Playwright без графического интерфейса
playwright.headless=false
```

### Пример веб-теста

```kotlin
class WebExampleTest : WebTest() {

    @Test
    @DisplayName("Пример веб-теста")
    @Feature("Web")
    fun webExample() {
        context.run {
            "Открываем главную страницу" {
                navigate("https://www.example.com")

                "Проверяем заголовок страницы" {
                    checkVisible(WebExamplePage.pageTitle)
                }
            }

            "Выполняем поиск" {
                typeText(WebExamplePage.searchInput, "test query")
                click(WebExamplePage.searchButton)

                "Проверяем результаты поиска" {
                    checkVisible(WebExamplePage.searchResults)
                }
            }
        }
    }
}
```

## Заключение

Фреймворк Easy Test Write предоставляет мощный и гибкий инструментарий для автоматизации тестирования мобильных и веб-приложений. Основные преимущества:

- **Кросс-платформенность** - поддержка Android, iOS и веб-приложений
- **Богатый DSL** - интуитивно понятный синтаксис для написания тестов
- **Расширенная интеграция с Allure** - подробные отчеты о выполнении тестов
- **Автоматическое управление эмуляторами** - запуск и остановка эмуляторов/симуляторов
- **Запись видео** - автоматическая запись выполнения тестов
- **Проверка аналитики** - встроенные инструменты для проверки событий аналитики
- **Параметризованное тестирование** - поддержка различных источников данных
- **Гибкие локаторы** - расширенные возможности поиска элементов

Фреймворк постоянно развивается, добавляются новые возможности и улучшается существующий функционал.
