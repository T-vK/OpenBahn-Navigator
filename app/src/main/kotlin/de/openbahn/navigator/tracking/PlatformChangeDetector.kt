package de.openbahn.navigator.tracking

import de.openbahn.model.Journey
import de.openbahn.model.Leg
import de.openbahn.model.StopEvent
import de.openbahn.model.railLegs

data class PlatformChange(
    val stopName: String,
    val oldPlatform: String,
    val newPlatform: String,
)

/** Detects platform (gleis) changes between two journey snapshots. */
object PlatformChangeDetector {
    fun detect(before: Journey, after: Journey): List<PlatformChange> {
        val previous = extractPlatforms(before)
        val current = extractPlatforms(after)
        return current.mapNotNull { (key, newPlatform) ->
            val oldPlatform = previous[key] ?: return@mapNotNull null
            if (oldPlatform.equals(newPlatform, ignoreCase = true)) return@mapNotNull null
            PlatformChange(
                stopName = key.substringBefore('|'),
                oldPlatform = oldPlatform,
                newPlatform = newPlatform,
            )
        }
    }

    private fun extractPlatforms(journey: Journey): Map<String, String> {
        val platforms = linkedMapOf<String, String>()
        journey.railLegs().forEachIndexed { legIndex, leg ->
            addStop(platforms, leg.origin, "leg$legIndex|dep")
            leg.intermediateStops.forEachIndexed { viaIndex, stop ->
                addStop(platforms, stop, "leg$legIndex|via$viaIndex")
            }
            addStop(platforms, leg.destination, "leg$legIndex|arr")
        }
        return platforms
    }

    private fun addStop(platforms: MutableMap<String, String>, stop: StopEvent, keySuffix: String) {
        val platform = stop.platform?.trim()?.takeIf { it.isNotEmpty() } ?: return
        platforms["${stop.name}|$keySuffix"] = platform
    }
}
