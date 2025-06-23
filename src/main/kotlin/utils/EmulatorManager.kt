package utils

import app.config.AppConfig
import app.model.Platform
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Класс для управления жизненным циклом эмуляторов и симуляторов.
 *
 * Предоставляет методы для запуска и остановки эмуляторов Android и симуляторов iOS.
 */
object EmulatorManager {
    private val logger: Logger = LoggerFactory.getLogger(EmulatorManager::class.java)

    /**
     * Запускает эмулятор или симулятор в зависимости от текущей платформы.
     *
     * @return `true`, если запуск успешен, иначе `false`.
     */
    fun startEmulator(): Boolean {
        return when (AppConfig.getPlatform()) {
            Platform.ANDROID -> startAndroidEmulator()
            Platform.IOS -> startIosSimulator()
            else -> {
                logger.info("Запуск эмулятора не требуется для платформы ${AppConfig.getPlatform()}")
                true
            }
        }
    }

    /**
     * Останавливает эмулятор или симулятор в зависимости от текущей платформы.
     *
     * @return `true`, если остановка успешна, иначе `false`.
     */
    fun stopEmulator(): Boolean {
        return when (AppConfig.getPlatform()) {
            Platform.ANDROID -> stopAndroidEmulator()
            Platform.IOS -> stopIosSimulator()
            else -> {
                logger.info("Остановка эмулятора не требуется для платформы ${AppConfig.getPlatform()}")
                true
            }
        }
    }

    /**
     * Запускает эмулятор Android.
     *
     * @return `true`, если запуск успешен, иначе `false`.
     */
    private fun startAndroidEmulator(): Boolean {
        val deviceName = AppConfig.getAndroidDeviceName()
        logger.info("Запуск Android эмулятора: $deviceName")
        
        // Проверяем, не запущен ли уже эмулятор
        val emulatorId = TerminalUtils.getEmulatorId()
        if (emulatorId != null) {
            logger.info("Эмулятор Android уже запущен с ID: $emulatorId")
            return true
        }
        
        // Запускаем эмулятор
        val command = listOf("emulator", "-avd", deviceName, "-no-snapshot-load", "-no-boot-anim")
        val errorMessage = "Не удалось запустить эмулятор Android"
        
        // Запускаем эмулятор в отдельном процессе, чтобы не блокировать выполнение тестов
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        
        // Ждем некоторое время, чтобы эмулятор успел запуститься
        Thread.sleep(10000)
        
        // Проверяем, запустился ли эмулятор
        val newEmulatorId = TerminalUtils.getEmulatorId()
        if (newEmulatorId != null) {
            logger.info("Эмулятор Android успешно запущен с ID: $newEmulatorId")
            return true
        }
        
        logger.error("Не удалось запустить эмулятор Android")
        return false
    }

    /**
     * Останавливает эмулятор Android.
     *
     * @return `true`, если остановка успешна, иначе `false`.
     */
    private fun stopAndroidEmulator(): Boolean {
        val emulatorId = TerminalUtils.getEmulatorId()
        if (emulatorId == null) {
            logger.info("Эмулятор Android не запущен, остановка не требуется")
            return true
        }
        
        logger.info("Остановка эмулятора Android с ID: $emulatorId")
        val command = listOf("adb", "-s", emulatorId, "emu", "kill")
        val errorMessage = "Не удалось остановить эмулятор Android"
        
        return TerminalUtils.runCommand(command, errorMessage)
    }

    /**
     * Запускает симулятор iOS.
     *
     * @return `true`, если запуск успешен, иначе `false`.
     */
    private fun startIosSimulator(): Boolean {
        val deviceName = AppConfig.getIosDeviceName()
        logger.info("Запуск iOS симулятора: $deviceName")
        
        // Проверяем, не запущен ли уже симулятор
        val simulatorId = TerminalUtils.getSimulatorId(deviceName)
        if (simulatorId != null) {
            logger.info("Симулятор iOS уже запущен с ID: $simulatorId")
            return true
        }
        
        // Получаем ID симулятора (в выключенном состоянии)
        val command1 = listOf("xcrun", "simctl", "list", "--json")
        val process = ProcessBuilder(command1)
            .redirectErrorStream(true)
            .start()
        
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
        }
        
        try {
            val simulatorsResponse = json.decodeFromString<TerminalUtils.SimulatorsResponse>(output)
            
            // Ищем симулятор с нужным именем
            for (runtime in simulatorsResponse.devices.values) {
                val simulator = runtime.find { it.name == deviceName }
                if (simulator != null) {
                    // Запускаем симулятор
                    val command2 = listOf("xcrun", "simctl", "boot", simulator.udid)
                    val errorMessage = "Не удалось запустить симулятор iOS"
                    
                    if (TerminalUtils.runCommand(command2, errorMessage)) {
                        logger.info("Симулятор iOS успешно запущен с ID: ${simulator.udid}")
                        return true
                    }
                }
            }
            
            logger.error("Не найден симулятор iOS с именем: $deviceName")
            return false
        } catch (e: Exception) {
            logger.error("Ошибка при запуске симулятора iOS", e)
            return false
        }
    }

    /**
     * Останавливает симулятор iOS.
     *
     * @return `true`, если остановка успешна, иначе `false`.
     */
    private fun stopIosSimulator(): Boolean {
        val deviceName = AppConfig.getIosDeviceName()
        val simulatorId = TerminalUtils.getSimulatorId(deviceName)
        
        if (simulatorId == null) {
            logger.info("Симулятор iOS не запущен, остановка не требуется")
            return true
        }
        
        logger.info("Остановка симулятора iOS с ID: $simulatorId")
        val command = listOf("xcrun", "simctl", "shutdown", simulatorId)
        val errorMessage = "Не удалось остановить симулятор iOS"
        
        return TerminalUtils.runCommand(command, errorMessage)
    }
}