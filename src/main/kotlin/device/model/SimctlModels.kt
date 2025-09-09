package device.model

import kotlinx.serialization.Serializable

/**
 * Response model for querying the list of simulators via `xcrun simctl list --json`.
 *
 * @property devices A map where the key is the platform version name, and the value is the list of simulators.
 */
@Serializable
data class SimulatorsResponse(val devices: Map<String, List<Simulator>>)

/**
 * iOS simulator model.
 *
 * @property udid Unique device identifier.
 * @property name Name of the simulator (e.g., "iPhone 15 Pro").
 * @property state Current device state (e.g., "Booted", "Shutdown").
 */
@Serializable
data class Simulator(val udid: String, val name: String, val state: String)
