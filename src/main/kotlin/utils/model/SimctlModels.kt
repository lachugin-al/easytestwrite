package utils.model

import kotlinx.serialization.Serializable

/**
 * Модель ответа при запросе списка симуляторов через `xcrun simctl list --json`.
 *
 * @property devices Карта, где ключ — название версии платформы, значение — список симуляторов.
 */
@Serializable
data class SimulatorsResponse(val devices: Map<String, List<Simulator>>)

/**
 * Модель симулятора iOS.
 *
 * @property udid Уникальный идентификатор устройства.
 * @property name Имя симулятора (например, "iPhone 15 Pro").
 * @property state Текущее состояние устройства (например, "Booted", "Shutdown").
 */
@Serializable
data class Simulator(val udid: String, val name: String, val state: String)
