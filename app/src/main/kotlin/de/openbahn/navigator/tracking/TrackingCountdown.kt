package de.openbahn.navigator.tracking

import de.openbahn.model.Journey
import de.openbahn.model.railLegs
import de.openbahn.navigator.data.TrackedJourneyWithJourney
import de.openbahn.navigator.ui.util.parseJourneyDateTime
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Formats remaining seconds as `M:SS` or `MM:SS` (minutes may exceed two digits). */
fun formatTrackingCountdown(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val minutes = clamped / 60
    val seconds = clamped % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/**
 * Seconds until the next meaningful event for [journey]: departure while still before it, otherwise
 * arrival at the final rail stop.
 */
fun countdownSecondsUntil(journey: Journey, now: LocalDateTime): Long? {
    val target = countdownTargetTime(journey, now) ?: return null
    return ChronoUnit.SECONDS.between(now, target).coerceAtLeast(0)
}

/** Soonest positive countdown across all actively tracked journeys. */
fun countdownSecondsUntil(tracked: List<TrackedJourneyWithJourney>, now: LocalDateTime): Long? {
    if (tracked.isEmpty()) return null
    return tracked.mapNotNull { countdownSecondsUntil(it.journey, now) }.minOrNull()
}

internal fun countdownTargetTime(journey: Journey, now: LocalDateTime): LocalDateTime? {
    val rails = journey.railLegs()
    if (rails.isEmpty()) return parseJourneyDateTime(journey.departure)
    val firstDep = eventDateTime(rails.first().origin) ?: parseJourneyDateTime(journey.departure)
    val lastArr = eventDateTime(rails.last().destination) ?: parseJourneyDateTime(journey.arrival)
    if (firstDep != null && now.isBefore(firstDep)) return firstDep
    return lastArr
}

private fun eventDateTime(event: de.openbahn.model.StopEvent): LocalDateTime? {
    val iso = event.prognosedTime?.takeIf { it.isNotBlank() } ?: event.scheduledTime
    return parseJourneyDateTime(iso)
}

/**
 * Prefixes [baseTitle] with a countdown when [countdownSeconds] is non-null, keeping the combined
 * title within [maxLength] by shortening the route portion via [fitRouteTitle].
 */
fun titleWithCountdown(
    routeNames: List<String>,
    baseTitle: String,
    countdownSeconds: Long?,
    maxLength: Int = 50,
): String {
    if (countdownSeconds == null) return baseTitle
    val prefix = "${formatTrackingCountdown(countdownSeconds)} · "
    if (routeNames.isEmpty()) return (prefix + baseTitle).take(maxLength)
    val routeMax = (maxLength - prefix.length).coerceAtLeast(8)
    return prefix + fitRouteTitle(routeNames, routeMax)
}
