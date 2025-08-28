package utils

import app.config.AppConfig
import app.model.Platform
import kotlinx.coroutines.sync.Mutex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Класс для управления жизненным циклом эмуляторов и симуляторов.
 *
 * Предоставляет методы для запуска и остановки эмуляторов Android и симуляторов iOS.
 * Реализация учитывает различные краевые случаи и ошибки, которые могут возникнуть при работе с эмуляторами.
 */
object EmulatorManager {
    private val logger: Logger = LoggerFactory.getLogger(EmulatorManager::class.java)

    // Мьютекс для предотвращения гонок при параллельном запуске/остановке эмуляторов
    private val emulatorMutex = Mutex()

    // Таймауты
    private const val EMULATOR_BOOT_TIMEOUT_SECONDS = 120
    private const val EMULATOR_STARTUP_TIMEOUT_SECONDS = 60
    private const val COMMAND_TIMEOUT_SECONDS = 30

    // Универсальное ожидание условия без использования Thread.sleep
    private fun waitForCondition(
        timeout: Duration,
        pollInterval: Duration = Duration.ofMillis(500),
        onTick: ((elapsed: Duration) -> Unit)? = null,
        condition: () -> Boolean
    ): Boolean {
        val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
        val latch = java.util.concurrent.CountDownLatch(1)
        val startNs = System.nanoTime()

        val task = Runnable {
            try {
                val elapsed = Duration.ofNanos(System.nanoTime() - startNs)
                if (condition()) {
                    latch.countDown()
                } else {
                    onTick?.invoke(elapsed)
                }
            } catch (t: Throwable) {
                // Не ломаем планировщик из-за исключений в condition/onTick
                logger.debug("Ошибка в задаче ожидания: ${t.message}")
            }
        }

        val future = scheduler.scheduleAtFixedRate(
            task,
            0,
            pollInterval.toMillis(),
            java.util.concurrent.TimeUnit.MILLISECONDS
        )

        val completed = try {
            latch.await(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
        } finally {
            future.cancel(true)
            scheduler.shutdownNow()
        }

        return completed
    }

    /**
     * Запускает эмулятор или симулятор в зависимости от текущей платформы.
     *
     * Обрабатывает следующие краевые случаи:
     * - Неизвестная платформа
     * - Неизвестное/пустое имя устройства
     * - Уже запущен эмулятор/симулятор, но он неработоспособен
     * - Невозможно получить ID устройства
     * - Ошибка запуска процесса эмулятора/симулятора
     * - Эмулятор/симулятор не стартует за таймаут
     * - Параллельный запуск/гонки
     * - Локализация/кодировка вывода
     * - Исключения времени выполнения
     * - Ошибки выполнения команд/зависания
     *
     * @return `true`, если запуск успешен, иначе `false`.
     */
    fun startEmulator(): Boolean {
        return try {
            // Используем мьютекс для предотвращения гонок при параллельном запуске
            if (!emulatorMutex.tryLock()) {
                logger.warn("Другой поток уже выполняет операции с эмулятором, ожидаем...")
                val acquired = waitForCondition(
                    timeout = Duration.ofSeconds(5),
                    pollInterval = Duration.ofMillis(200)
                ) {
                    emulatorMutex.tryLock()
                }
                if (!acquired) {
                    logger.error("Не удалось получить блокировку для запуска эмулятора")
                    return false
                }
            }

            // Проверяем платформу
            when (val platform = AppConfig.getPlatform()) {
                Platform.ANDROID -> startAndroidEmulator()
                Platform.IOS -> startIosSimulator()
                else -> {
                    logger.info("Запуск эмулятора не требуется для платформы $platform")
                    true
                }
            }
        } catch (e: Exception) {
            logger.error("Непредвиденная ошибка при запуске эмулятора: ${e.message}", e)
            false
        } finally {
            if (emulatorMutex.isLocked) {
                emulatorMutex.unlock()
            }
        }
    }

    /**
     * Останавливает эмулятор или симулятор в зависимости от текущей платформы.
     *
     * Обрабатывает следующие краевые случаи:
     * - Неизвестная платформа
     * - Неудачная остановка устройства (и оно продолжает висеть)
     * - Параллельный запуск/гонки
     * - Локализация/кодировка вывода
     * - Исключения времени выполнения
     * - Ошибки выполнения команд/зависания
     *
     * @return `true`, если остановка успешна, иначе `false`.
     */
    fun stopEmulator(): Boolean {
        return try {
            // Используем мьютекс для предотвращения гонок при параллельной остановке
            if (!emulatorMutex.tryLock()) {
                logger.warn("Другой поток уже выполняет операции с эмулятором, ожидаем...")
                val acquired = waitForCondition(
                    timeout = Duration.ofSeconds(5),
                    pollInterval = Duration.ofMillis(200)
                ) {
                    emulatorMutex.tryLock()
                }
                if (!acquired) {
                    logger.error("Не удалось получить блокировку для остановки эмулятора")
                    return false
                }
            }

            // Проверяем платформу
            when (val platform = AppConfig.getPlatform()) {
                Platform.ANDROID -> stopAndroidEmulator()
                Platform.IOS -> stopIosSimulator()
                else -> {
                    logger.info("Остановка эмулятора не требуется для платформы $platform")
                    true
                }
            }
        } catch (e: Exception) {
            logger.error("Непредвиденная ошибка при остановке эмулятора: ${e.message}", e)
            false
        } finally {
            if (emulatorMutex.isLocked) {
                emulatorMutex.unlock()
            }
        }
    }

    /**
     * Проверяет наличие необходимых утилит для работы с эмуляторами/симуляторами.
     *
     * @param commands Список команд для проверки.
     * @return `true`, если все утилиты доступны, иначе `false`.
     */
    private fun checkRequiredTools(commands: List<String>): Boolean {
        for (command in commands) {
            try {
                val cmd = if (System.getProperty("os.name").lowercase().contains("windows")) {
                    listOf("where", command)
                } else {
                    listOf("which", command)
                }
                val result = TerminalUtils.runCommand(
                    command = cmd,
                    timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                )
                if (result.timedOut || result.exitCode != 0) {
                    logger.error("Утилита '$command' не найдена в системе")
                    return false
                }
            } catch (e: Exception) {
                logger.error("Ошибка при проверке наличия утилиты '$command': ${e.message}", e)
                return false
            }
        }
        return true
    }

    /**
     * Ожидает полной загрузки эмулятора Android.
     *
     * Обрабатывает следующие краевые случаи:
     * - Эмулятор не стартует за таймаут
     * - Ошибки выполнения команд/зависания
     * - Локализация/кодировка вывода
     *
     * @param deviceId ID эмулятора, загрузку которого нужно дождаться.
     * @return `true`, если эмулятор успешно загрузился, иначе `false`.
     */
    private fun waitForEmulatorBoot(deviceId: String): Boolean {
        if (deviceId.isBlank()) {
            logger.error("Невозможно дождаться загрузки эмулятора: пустой ID устройства")
            return false
        }

        val maxAttempts = EMULATOR_BOOT_TIMEOUT_SECONDS / 2 // Проверка каждые 2 секунды
        for (i in 1..maxAttempts) {
            try {
                val result = TerminalUtils.runCommand(
                    command = listOf("adb", "-s", deviceId, "shell", "getprop", "sys.boot_completed"),
                    timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                )

                if (result.timedOut) {
                    logger.warn("Таймаут при проверке загрузки эмулятора $deviceId, попытка $i/$maxAttempts")
                    continue
                }

                val bootCompleted = result.stdout.trim()

                if (bootCompleted == "1") {
                    // Дополнительная проверка работоспособности эмулятора
                    if (isEmulatorResponsive(deviceId)) {
                        logger.info("Эмулятор $deviceId полностью загружен и готов к работе.")
                        return true
                    } else {
                        logger.warn("Эмулятор $deviceId загружен, но не отвечает на команды")
                    }
                } else {
                    logger.info("Эмулятор $deviceId ещё не готов (sys.boot_completed=$bootCompleted), попытка $i/$maxAttempts")
                }
            } catch (e: Exception) {
                logger.warn("Ошибка при проверке загрузки эмулятора $deviceId: ${e.message}")
            }

            // Неблокирующее ожидание 2 секунды между проверками
            waitForCondition(
                timeout = Duration.ofSeconds(2),
                pollInterval = Duration.ofMillis(200)
            ) { false }
        }

        logger.error("Эмулятор $deviceId так и не загрузился за отведенное время ($EMULATOR_BOOT_TIMEOUT_SECONDS секунд)")
        return false
    }

    /**
     * Проверяет, отвечает ли эмулятор на команды.
     *
     * @param deviceId ID эмулятора для проверки.
     * @return `true`, если эмулятор отвечает на команды, иначе `false`.
     */
    private fun isEmulatorResponsive(deviceId: String): Boolean {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("adb", "-s", deviceId, "shell", "pm", "list", "packages"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Таймаут при проверке работоспособности эмулятора $deviceId")
                return false
            }
            val output = result.stdout
            return output.contains("package:") && result.exitCode == 0
        } catch (e: Exception) {
            logger.warn("Ошибка при проверке работоспособности эмулятора $deviceId: ${e.message}")
            return false
        }
    }

    /**
     * Запускает эмулятор Android.
     *
     * Обрабатывает следующие краевые случаи:
     * - Неизвестное/пустое имя устройства
     * - Уже запущен эмулятор, но он неработоспособен
     * - Невозможно получить ID устройства
     * - Ошибка запуска процесса эмулятора
     * - Эмулятор не стартует за таймаут
     * - Отсутствие или неправильная версия внешних утилит
     * - Локализация/кодировка вывода
     * - Исключения времени выполнения
     * - Ошибки выполнения команд/зависания
     *
     * @return `true`, если запуск успешен, иначе `false`.
     */
    private fun startAndroidEmulator(): Boolean {
        try {
            // Проверяем наличие необходимых утилит
            if (!checkRequiredTools(listOf("adb", "emulator"))) {
                logger.error("Отсутствуют необходимые утилиты для запуска Android эмулятора")
                return false
            }

            // Получаем имя устройства
            val deviceName = AppConfig.getAndroidDeviceName()
            if (deviceName.isBlank()) {
                logger.error("Не указано имя устройства Android для запуска")
                return false
            }

            logger.info("Запуск Android эмулятора: $deviceName")

            // Проверяем, существует ли указанный эмулятор
            if (!checkEmulatorExists(deviceName)) {
                logger.error("Эмулятор с именем '$deviceName' не найден в системе")
                return false
            }

            // Проверяем, не запущен ли уже эмулятор
            val emulatorId = getEmulatorId()
            if (emulatorId != null) {
                logger.info("Эмулятор Android уже запущен с ID: $emulatorId")

                // Проверяем, работоспособен ли эмулятор
                if (isEmulatorResponsive(emulatorId)) {
                    logger.info("Эмулятор Android с ID: $emulatorId работоспособен")
                    return true
                } else {
                    logger.warn("Эмулятор Android с ID: $emulatorId не отвечает, пробуем перезапустить")
                    // Принудительно останавливаем неработоспособный эмулятор
                    forceStopAndroidEmulator(emulatorId)
                }
            }

            // Запускаем эмулятор с дополнительными параметрами для стабильности
            val commandList = mutableListOf(
                "emulator",
                "-avd",
                deviceName,
                "-no-snapshot-load",
                "-no-boot-anim",
                "-gpu", "swiftshader_indirect",
                "-no-audio"
            )

            // Добавляем параметр "-no-window" только если включен headless режим
            if (AppConfig.isAndroidHeadlessMode()) {
                commandList.add("-no-window")  // Запуск без окна для стабильности в CI/CD
            }

            val command = commandList

            logger.info("Запуск команды: ${command.joinToString(" ")}")

            // Запускаем эмулятор в отдельном процессе, чтобы не блокировать выполнение тестов
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            // Создаем поток для чтения вывода процесса, чтобы избежать блокировки буфера
            Thread {
                try {
                    process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            logger.debug("Emulator output: $line")
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Ошибка при чтении вывода эмулятора: ${e.message}")
                }
            }.start()

            // Ждем, пока эмулятор запустится и получит ID, с таймаутом
            val maxAttempts = EMULATOR_STARTUP_TIMEOUT_SECONDS / 2 // Проверка каждые 2 секунды
            var newEmulatorId: String? = null

            for (i in 1..maxAttempts) {
                // Проверяем, не завершился ли процесс с ошибкой
                if (!process.isAlive && process.exitValue() != 0) {
                    logger.error("Процесс эмулятора завершился с ошибкой, код: ${process.exitValue()}")
                    return false
                }

                newEmulatorId = getEmulatorId()
                if (newEmulatorId != null) {
                    logger.info("Эмулятор Android запущен с ID: $newEmulatorId после $i попыток")
                    break
                } else {
                    logger.info("Ожидание запуска эмулятора Android, попытка $i/$maxAttempts")
                    // Неблокирующее ожидание 2 секунды между проверками
                    waitForCondition(
                        timeout = Duration.ofSeconds(2),
                        pollInterval = Duration.ofMillis(200)
                    ) { false }
                }
            }

            if (newEmulatorId != null) {
                logger.info("Эмулятор Android запущен с ID: $newEmulatorId")
                // Ждем полной загрузки эмулятора
                val bootSuccess = waitForEmulatorBoot(newEmulatorId)

                if (!bootSuccess) {
                    // Если эмулятор не загрузился полностью, пробуем его остановить
                    logger.warn("Эмулятор не загрузился полностью, пробуем остановить")
                    forceStopAndroidEmulator(newEmulatorId)
                    return false
                }

                return true
            }

            logger.error("Не удалось запустить эмулятор Android за отведенное время ($EMULATOR_STARTUP_TIMEOUT_SECONDS секунд)")
            // Убиваем процесс эмулятора, если он все еще работает
            if (process.isAlive) {
                process.destroyForcibly()
            }
            return false

        } catch (e: Exception) {
            logger.error("Ошибка при запуске эмулятора Android: ${e.message}", e)
            return false
        }
    }

    /**
     * Проверяет, существует ли эмулятор с указанным именем.
     *
     * @param deviceName Имя эмулятора для проверки.
     * @return `true`, если эмулятор существует, иначе `false`.
     */
    private fun checkEmulatorExists(deviceName: String): Boolean {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("emulator", "-list-avds"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Таймаут при получении списка эмуляторов")
                return false
            }
            val output = result.stdout
            return output.lines().any { it.trim() == deviceName }
        } catch (e: Exception) {
            logger.warn("Ошибка при проверке существования эмулятора: ${e.message}")
            return false
        }
    }

    /**
     * Получает ID запущенного эмулятора Android.
     *
     * @return ID эмулятора или `null`, если эмулятор не запущен.
     */
    fun getEmulatorId(): String? {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("adb", "devices"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Таймаут при получении списка устройств")
                return null
            }
            val output = result.stdout
            // Ищем строку, содержащую "emulator-" и "device"
            return output.lines()
                .filter { it.contains("emulator-") && it.contains("device") }
                .map { it.split("\\s+".toRegex()).first() }
                .firstOrNull()
        } catch (e: Exception) {
            logger.warn("Ошибка при получении ID эмулятора: ${e.message}")
            return null
        }
    }

    /**
     * Принудительно останавливает эмулятор Android.
     *
     * @param emulatorId ID эмулятора для остановки.
     */
    private fun forceStopAndroidEmulator(emulatorId: String) {
        try {
            // Сначала пробуем остановить через adb emu kill
            val result1 = TerminalUtils.runCommand(
                command = listOf("adb", "-s", emulatorId, "emu", "kill"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result1.timedOut) {
                logger.warn("Таймаут при остановке эмулятора через adb emu kill")
            }

            // Подождем до 2 секунд, давая эмулятору шанс остановиться без Thread.sleep
            waitForCondition(
                timeout = Duration.ofSeconds(2),
                pollInterval = Duration.ofMillis(200)
            ) { getEmulatorId() == null }

            // Проверяем, остановился ли эмулятор
            if (getEmulatorId() != null) {
                logger.warn("Эмулятор не остановился через adb emu kill, пробуем через killall")

                // Если не остановился, пробуем через killall (для Linux/Mac)
                if (!System.getProperty("os.name").lowercase().contains("windows")) {
                    TerminalUtils.runCommand(
                        command = listOf("killall", "-9", "qemu-system-x86_64"),
                        timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                    )
                } else {
                    // Для Windows используем taskkill
                    TerminalUtils.runCommand(
                        command = listOf("taskkill", "/F", "/IM", "qemu-system-x86_64.exe"),
                        timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn("Ошибка при принудительной остановке эмулятора: ${e.message}")
        }
    }

    /**
     * Останавливает эмулятор Android.
     *
     * Обрабатывает следующие краевые случаи:
     * - Неудачная остановка устройства (и оно продолжает висеть)
     * - Отсутствие или неправильная версия внешних утилит
     * - Локализация/кодировка вывода
     * - Исключения времени выполнения
     * - Ошибки выполнения команд/зависания
     *
     * @return `true`, если остановка успешна, иначе `false`.
     */
    private fun stopAndroidEmulator(): Boolean {
        try {
            // Проверяем наличие необходимых утилит
            if (!checkRequiredTools(listOf("adb"))) {
                logger.error("Отсутствуют необходимые утилиты для остановки Android эмулятора")
                return false
            }

            val emulatorId = getEmulatorId()
            if (emulatorId == null) {
                logger.info("Эмулятор Android не запущен, остановка не требуется")
                return true
            }

            logger.info("Остановка эмулятора Android с ID: $emulatorId")

            // Пробуем остановить эмулятор стандартным способом
            val result = TerminalUtils.runCommand(
                command = listOf("adb", "-s", emulatorId, "emu", "kill"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Таймаут при остановке эмулятора Android")
            }

            // После команды завершения подождём, давая эмулятору шанс остановиться корректно.
            // Используем переменную окружения ANDROID_EMULATOR_WAIT_TIME_BEFORE_KILL (секунды), если задана.
            val envWaitSeconds = System.getenv("ANDROID_EMULATOR_WAIT_TIME_BEFORE_KILL")?.toIntOrNull()
            // По умолчанию эмулятор ждёт 20с сам, дадим чуть больше запаса.
            val gracefulWaitSeconds = (envWaitSeconds ?: 20) + 5

            val stoppedGracefully = waitForCondition(
                timeout = Duration.ofSeconds(gracefulWaitSeconds.toLong()),
                pollInterval = Duration.ofSeconds(1),
                onTick = { elapsed ->
                    val elapsedSec = elapsed.seconds
                    if (elapsedSec == 0L || elapsedSec % 5L == 0L) {
                        logger.debug("Ожидание остановки эмулятора... ${'$'}elapsedSec/${'$'}gracefulWaitSeconds s")
                    }
                }
            ) {
                getEmulatorId() == null
            }

            if (!stoppedGracefully && getEmulatorId() != null) {
                logger.warn("Эмулятор Android не остановился стандартным способом за ${'$'}gracefulWaitSeconds s, применяем принудительную остановку")
                forceStopAndroidEmulator(emulatorId)

                // Проверяем еще раз с коротким ожиданием без Thread.sleep
                val stoppedAfterForce = waitForCondition(
                    timeout = Duration.ofSeconds(5),
                    pollInterval = Duration.ofMillis(500)
                ) {
                    getEmulatorId() == null
                }
                val stillRunningAfterForce = !stoppedAfterForce
                if (stillRunningAfterForce) {
                    logger.error("Не удалось остановить эмулятор Android даже принудительно")
                    return false
                }
            }

            logger.info("Эмулятор Android успешно остановлен")
            return true
        } catch (e: Exception) {
            logger.error("Ошибка при остановке эмулятора Android: ${e.message}", e)
            return false
        }
    }

    /**
     * Запускает симулятор iOS.
     *
     * Обрабатывает следующие краевые случаи:
     * - Неизвестное/пустое имя устройства
     * - Уже запущен симулятор, но он неработоспособен
     * - Невозможно получить ID устройства
     * - Ошибка запуска процесса симулятора
     * - Симулятор не стартует за таймаут
     * - Ошибка парсинга JSON вывода симулятора
     * - Отсутствие или неправильная версия внешних утилит
     * - Локализация/кодировка вывода
     * - Исключения времени выполнения
     * - Ошибки выполнения команд/зависания
     *
     * @return `true`, если запуск успешен, иначе `false`.
     */
    private fun startIosSimulator(): Boolean {
        try {
            // Проверяем, что мы на macOS (iOS симуляторы работают только на macOS)
            if (!System.getProperty("os.name").contains("Mac")) {
                logger.error("iOS симуляторы поддерживаются только на macOS")
                return false
            }

            // Проверяем наличие необходимых утилит
            if (!checkRequiredTools(listOf("xcrun", "simctl"))) {
                logger.error("Отсутствуют необходимые утилиты для запуска iOS симулятора")
                return false
            }

            // Получаем имя устройства
            val deviceName = AppConfig.getIosDeviceName()
            if (deviceName.isBlank()) {
                logger.error("Не указано имя устройства iOS для запуска")
                return false
            }

            logger.info("Запуск iOS симулятора: $deviceName")

            // Проверяем, не запущен ли уже симулятор
            val simulatorId = getSimulatorId(deviceName)
            if (simulatorId != null) {
                logger.info("Симулятор iOS уже запущен с ID: $simulatorId")

                // Проверяем, работоспособен ли симулятор
                if (isSimulatorResponsive(simulatorId)) {
                    logger.info("Симулятор iOS с ID: $simulatorId работоспособен")
                    return true
                } else {
                    logger.warn("Симулятор iOS с ID: $simulatorId не отвечает, пробуем перезапустить")
                    // Принудительно останавливаем неработоспособный симулятор
                    forceStopIosSimulator(simulatorId)
                }
            }

            // Получаем список всех доступных симуляторов
            val simulatorsJson = getSimulatorsList()
            if (simulatorsJson.isBlank()) {
                logger.error("Не удалось получить список доступных iOS симуляторов")
                return false
            }

            // Парсим JSON с обработкой ошибок
            try {
                val json = kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                }

                val simulatorsResponse = json.decodeFromString<utils.model.SimulatorsResponse>(simulatorsJson)

                // Ищем симулятор с нужным именем
                var foundSimulatorId: String? = null

                for (runtime in simulatorsResponse.devices.values) {
                    val simulator = runtime.find { it.name == deviceName }
                    if (simulator != null) {
                        foundSimulatorId = simulator.udid
                        break
                    }
                }

                if (foundSimulatorId == null) {
                    logger.error("Не найден симулятор iOS с именем: $deviceName")
                    return false
                }

                // Запускаем симулятор
                logger.info("Запуск симулятора iOS с ID: $foundSimulatorId")

                val result = TerminalUtils.runCommand(
                    command = listOf("xcrun", "simctl", "boot", foundSimulatorId),
                    timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
                )
                if (result.timedOut) {
                    logger.error("Таймаут при запуске симулятора iOS")
                    return false
                }
                if (result.exitCode != 0) {
                    val error = if (result.stderr.isNotBlank()) result.stderr else result.stdout
                    logger.error("Ошибка при запуске симулятора iOS: $error")
                    return false
                }

                // Ждем, пока симулятор полностью загрузится
                val maxAttempts = EMULATOR_BOOT_TIMEOUT_SECONDS / 2 // Проверка каждые 2 секунды
                for (i in 1..maxAttempts) {
                    if (isSimulatorResponsive(foundSimulatorId)) {
                        logger.info("Симулятор iOS успешно запущен и готов к работе")
                        return true
                    } else {
                        logger.info("Ожидание загрузки симулятора iOS, попытка $i/$maxAttempts")
                        waitForCondition(
                            timeout = Duration.ofSeconds(2),
                            pollInterval = Duration.ofMillis(200)
                        ) { false }
                    }
                }

                logger.error("Симулятор iOS не стал работоспособным за отведенное время")
                return false

            } catch (e: Exception) {
                logger.error("Ошибка при парсинге JSON вывода симулятора: ${e.message}", e)
                return false
            }
        } catch (e: Exception) {
            logger.error("Ошибка при запуске симулятора iOS: ${e.message}", e)
            return false
        }
    }

    /**
     * Получает список всех доступных iOS симуляторов в формате JSON.
     *
     * @return JSON-строка со списком симуляторов или пустая строка в случае ошибки.
     */
    private fun getSimulatorsList(): String {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "list", "--json"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Таймаут при получении списка iOS симуляторов")
                return ""
            }
            return result.stdout
        } catch (e: Exception) {
            logger.warn("Ошибка при получении списка iOS симуляторов: ${e.message}")
            return ""
        }
    }

    /**
     * Получает ID запущенного iOS симулятора по его имени.
     *
     * @param simulatorName Имя симулятора.
     * @return ID симулятора или `null`, если симулятор не запущен.
     */
    fun getSimulatorId(simulatorName: String): String? {
        try {
            val simulatorsJson = getSimulatorsList()
            if (simulatorsJson.isBlank()) {
                return null
            }

            val json = kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            }

            val simulatorsResponse = json.decodeFromString<utils.model.SimulatorsResponse>(simulatorsJson)

            // Ищем запущенный симулятор с нужным именем
            for (runtime in simulatorsResponse.devices.values) {
                val simulator = runtime.find { it.name == simulatorName && it.state == "Booted" }
                if (simulator != null) {
                    return simulator.udid
                }
            }

            return null
        } catch (e: Exception) {
            logger.warn("Ошибка при получении ID iOS симулятора: ${e.message}")
            return null
        }
    }

    /**
     * Проверяет, отвечает ли iOS симулятор на команды.
     *
     * @param simulatorId ID симулятора для проверки.
     * @return `true`, если симулятор отвечает на команды, иначе `false`.
     */
    private fun isSimulatorResponsive(simulatorId: String): Boolean {
        try {
            val result = TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "list", "apps", simulatorId),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Таймаут при проверке работоспособности симулятора iOS")
                return false
            }
            return result.exitCode == 0
        } catch (e: Exception) {
            logger.warn("Ошибка при проверке работоспособности симулятора iOS: ${e.message}")
            return false
        }
    }

    /**
     * Принудительно останавливает iOS симулятор.
     *
     * @param simulatorId ID симулятора для остановки.
     */
    private fun forceStopIosSimulator(simulatorId: String) {
        try {
            // Сначала пробуем стандартный способ
            val result1 = TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "shutdown", simulatorId),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result1.timedOut) {
                logger.warn("Таймаут при остановке симулятора iOS")
            }

            // Подождем до 2 секунд без Thread.sleep, давая симулятору шанс остановиться
            waitForCondition(
                timeout = Duration.ofSeconds(2),
                pollInterval = Duration.ofMillis(200)
            ) { false }

            // Если не помогло, пробуем остановить все симуляторы
            TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "shutdown", "all"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )

            // Если и это не помогло, пробуем через killall
            TerminalUtils.runCommand(
                command = listOf("killall", "Simulator"),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
        } catch (e: Exception) {
            logger.warn("Ошибка при принудительной остановке симулятора iOS: ${e.message}")
        }
    }

    /**
     * Останавливает симулятор iOS.
     *
     * Обрабатывает следующие краевые случаи:
     * - Неудачная остановка устройства (и оно продолжает висеть)
     * - Отсутствие или неправильная версия внешних утилит
     * - Локализация/кодировка вывода
     * - Исключения времени выполнения
     * - Ошибки выполнения команд/зависания
     *
     * @return `true`, если остановка успешна, иначе `false`.
     */
    private fun stopIosSimulator(): Boolean {
        try {
            // Проверяем, что мы на macOS (iOS симуляторы работают только на macOS)
            if (!System.getProperty("os.name").contains("Mac")) {
                logger.error("iOS симуляторы поддерживаются только на macOS")
                return false
            }

            // Проверяем наличие необходимых утилит
            if (!checkRequiredTools(listOf("xcrun", "simctl"))) {
                logger.error("Отсутствуют необходимые утилиты для остановки iOS симулятора")
                return false
            }

            val deviceName = AppConfig.getIosDeviceName()
            if (deviceName.isBlank()) {
                logger.error("Не указано имя устройства iOS для остановки")
                return false
            }

            val simulatorId = getSimulatorId(deviceName)
            if (simulatorId == null) {
                logger.info("Симулятор iOS не запущен, остановка не требуется")
                return true
            }

            logger.info("Остановка симулятора iOS с ID: $simulatorId")

            // Пробуем остановить симулятор стандартным способом
            val result = TerminalUtils.runCommand(
                command = listOf("xcrun", "simctl", "shutdown", simulatorId),
                timeout = Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS.toLong())
            )
            if (result.timedOut) {
                logger.warn("Таймаут при остановке симулятора iOS")
            }

            // Подождем до 2 секунд без Thread.sleep, давая симулятору шанс остановиться
            waitForCondition(
                timeout = Duration.ofSeconds(2),
                pollInterval = Duration.ofMillis(200)
            ) { getSimulatorId(deviceName) == null }

            // Проверяем, остановился ли симулятор
            val stillRunning = getSimulatorId(deviceName) != null
            if (stillRunning) {
                logger.warn("Симулятор iOS не остановился стандартным способом, применяем принудительную остановку")
                forceStopIosSimulator(simulatorId)

                // Проверяем еще раз с ожиданием до 5 секунд без Thread.sleep
                val stoppedAfterForce = waitForCondition(
                    timeout = Duration.ofSeconds(5),
                    pollInterval = Duration.ofMillis(200)
                ) { getSimulatorId(deviceName) == null }
                val stillRunningAfterForce = !stoppedAfterForce
                if (stillRunningAfterForce) {
                    logger.error("Не удалось остановить симулятор iOS даже принудительно")
                    return false
                }
            }

            logger.info("Симулятор iOS успешно остановлен")
            return true
        } catch (e: Exception) {
            logger.error("Ошибка при остановке симулятора iOS: ${e.message}", e)
            return false
        }
    }
}
