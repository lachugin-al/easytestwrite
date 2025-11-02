package proxy

import org.slf4j.LoggerFactory
import utils.TerminalUtils

/**
 * Utility for logging the current system proxy settings
 * during sessions on Android emulator and iOS simulator.
 *
 * Works as safely as possible: if required tools
 * (adb/scutil/xcrun) are missing, it does not crash but logs a warning.
 */
object ProxyInspector {
    private val logger = LoggerFactory.getLogger(ProxyInspector::class.java)

    /**
     * Logs information about proxy settings on the Android emulator side.
     * Uses adb to read system settings and properties.
     */
    fun logAndroidProxy() {
        logger.info("[Proxy] Checking Android emulator system proxy settings…")

        // Check adb availability
        val adbVersion = TerminalUtils.runCommand(listOf("adb", "version"))
        if (adbVersion.isBlank()) {
            logger.warn("[Proxy][Android] adb is not available or not found in PATH — skipping check.")
            return
        }

        // List of connected devices (for context)
        val devicesList = TerminalUtils.runCommand(listOf("adb", "devices", "-l")).trim()
        if (devicesList.isNotBlank()) logger.info("[Proxy][Android] Connected devices:\n$devicesList")

        // Reading system settings (Settings.Global)
        val httpProxy = TerminalUtils.runCommand(listOf("adb", "shell", "settings", "get", "global", "http_proxy")).trim()
        val httpHost = TerminalUtils.runCommand(listOf("adb", "shell", "settings", "get", "global", "http_proxy_host")).trim()
        val httpPort = TerminalUtils.runCommand(listOf("adb", "shell", "settings", "get", "global", "http_proxy_port")).trim()
        val exclusionList = TerminalUtils.runCommand(listOf("adb", "shell", "settings", "get", "global", "global_http_proxy_exclusion_list")).trim()

        // Sometimes a persistent global proxy is used
        val globalHttpHost = TerminalUtils.runCommand(listOf("adb", "shell", "settings", "get", "global", "global_http_proxy_host")).trim()
        val globalHttpPort = TerminalUtils.runCommand(listOf("adb", "shell", "settings", "get", "global", "global_http_proxy_port")).trim()

        // Additionally read system properties (sometimes emulator sets them this way)
        val propHttpHost = TerminalUtils.runCommand(listOf("adb", "shell", "getprop", "http.proxyHost")).trim()
        val propHttpPort = TerminalUtils.runCommand(listOf("adb", "shell", "getprop", "http.proxyPort")).trim()
        val propHttpsHost = TerminalUtils.runCommand(listOf("adb", "shell", "getprop", "https.proxyHost")).trim()
        val propHttpsPort = TerminalUtils.runCommand(listOf("adb", "shell", "getprop", "https.proxyPort")).trim()

        logger.info(
            """
            |[Proxy][Android] Current values:
            |  settings global http_proxy:               ${httpProxy.ifBlank { "<empty>" }}
            |  settings global http_proxy_host:          ${httpHost.ifBlank { "<empty>" }}
            |  settings global http_proxy_port:          ${httpPort.ifBlank { "<empty>" }}
            |  settings global global_http_proxy_host:   ${globalHttpHost.ifBlank { "<empty>" }}
            |  settings global global_http_proxy_port:   ${globalHttpPort.ifBlank { "<empty>" }}
            |  settings global global_http_proxy_exclusion_list: ${exclusionList.ifBlank { "<empty>" }}
            |  getprop http.proxyHost:                   ${propHttpHost.ifBlank { "<empty>" }}
            |  getprop http.proxyPort:                   ${propHttpPort.ifBlank { "<empty>" }}
            |  getprop https.proxyHost:                  ${propHttpsHost.ifBlank { "<empty>" }}
            |  getprop https.proxyPort:                  ${propHttpsPort.ifBlank { "<empty>" }}
            |""".trimMargin()
        )
    }

    /**
     * Logs information about macOS system proxy settings (used by iOS simulator).
     */
    fun logIosSimulatorProxy() {
        logger.info("[Proxy] Checking macOS system proxy settings (for iOS Simulator)…")

        // Check if Xcode tools are available (not required, but gives context)
        val simctlHelp = TerminalUtils.runCommand(listOf("xcrun", "simctl", "list", "devices", "booted")).trim()
        if (simctlHelp.isNotBlank()) {
            logger.info("[Proxy][iOS] Booted simulators:\n$simctlHelp")
        }

        // Main source of truth — scutil --proxy
        val scutilOut = TerminalUtils.runCommand(listOf("scutil", "--proxy")).trim()
        if (scutilOut.isBlank()) {
            logger.warn("[Proxy][iOS] Failed to get data via 'scutil --proxy' — command may be unavailable.")
        } else {
            logger.info("[Proxy][iOS] scutil --proxy:\n$scutilOut")
        }

        // Additionally try reading web/secure web proxy for Wi-Fi interface (if available)
        val webProxy = TerminalUtils.runCommand(listOf("networksetup", "-getwebproxy", "Wi-Fi")).trim()
        if (webProxy.isNotBlank()) logger.info("[Proxy][iOS] networksetup -getwebproxy Wi-Fi:\n$webProxy")
        val secureWebProxy = TerminalUtils.runCommand(listOf("networksetup", "-getsecurewebproxy", "Wi-Fi")).trim()
        if (secureWebProxy.isNotBlank()) logger.info("[Proxy][iOS] networksetup -getsecurewebproxy Wi-Fi:\n$secureWebProxy")

        // And environment variables, in case proxies are set via env
        val envHttp = System.getenv("HTTP_PROXY") ?: System.getenv("http_proxy")
        val envHttps = System.getenv("HTTPS_PROXY") ?: System.getenv("https_proxy")
        val envNoProxy = System.getenv("NO_PROXY") ?: System.getenv("no_proxy")
        logger.info(
            "[Proxy][iOS] env HTTP(S)_PROXY/NO_PROXY: HTTP=${envHttp ?: "<empty>"}; HTTPS=${envHttps ?: "<empty>"}; NO_PROXY=${envNoProxy ?: "<empty>"}"
        )
    }
}
