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

data class TrackingTitleCountdown(
    val leftSeconds: Long? = null,
    val rightSeconds: Long? = null,
)

/**
 * Seconds until departure at the first rail leg (prognosed when available). Stays at 0 once
 * departure has passed.
 */
fun countdownSecondsUntil(journey: Journey, now: LocalDateTime): Long? {
    val target = countdownDepartureTime(journey) ?: return null
    return ChronoUnit.SECONDS.between(now, target).coerceAtLeast(0)
}

/** Seconds until arrival at the final rail leg. Stays at 0 once arrival has passed. */
fun arrivalCountdownSecondsUntil(journey: Journey, now: LocalDateTime): Long? {
    val target = countdownArrivalTime(journey) ?: return null
    return ChronoUnit.SECONDS.between(now, target).coerceAtLeast(0)
}

fun hasDeparted(journey: Journey, now: LocalDateTime): Boolean {
    val departure = countdownDepartureTime(journey) ?: return false
    return !now.isBefore(departure)
}

/** Soonest departure countdown across journeys that have not departed yet. */
fun countdownSecondsUntil(tracked: List<TrackedJourneyWithJourney>, now: LocalDateTime): Long? {
    if (tracked.isEmpty()) return null
    return tracked
        .filter { !hasDeparted(it.journey, now) }
        .mapNotNull { countdownSecondsUntil(it.journey, now) }
        .minOrNull()
}

/** Soonest arrival countdown across journeys that have already departed. */
fun arrivalCountdownSecondsUntil(tracked: List<TrackedJourneyWithJourney>, now: LocalDateTime): Long? {
    if (tracked.isEmpty()) return null
    return tracked
        .filter { hasDeparted(it.journey, now) }
        .mapNotNull { arrivalCountdownSecondsUntil(it.journey, now) }
        .minOrNull()
}

fun trackingTitleCountdown(
    tracked: List<TrackedJourneyWithJourney>,
    now: LocalDateTime,
    showDepartureCountdown: Boolean,
    showArrivalCountdown: Boolean,
): TrackingTitleCountdown {
    val departureSeconds = if (showDepartureCountdown) {
        countdownSecondsUntil(tracked, now)
    } else {
        null
    }
    val arrivalSeconds = if (showArrivalCountdown) {
        arrivalCountdownSecondsUntil(tracked, now)
    } else {
        null
    }
    return when {
        departureSeconds != null -> TrackingTitleCountdown(leftSeconds = departureSeconds)
        arrivalSeconds != null -> TrackingTitleCountdown(rightSeconds = arrivalSeconds)
        else -> TrackingTitleCountdown()
    }
}

internal fun countdownDepartureTime(journey: Journey): LocalDateTime? {
    val rails = journey.railLegs()
    if (rails.isEmpty()) return parseJourneyDateTime(journey.departure)
    return eventDateTime(rails.first().origin) ?: parseJourneyDateTime(journey.departure)
}

internal fun countdownArrivalTime(journey: Journey): LocalDateTime? {
    val rails = journey.railLegs()
    if (rails.isEmpty()) return parseJourneyDateTime(journey.arrival)
    return eventDateTime(rails.last().destination) ?: parseJourneyDateTime(journey.arrival)
}

private fun eventDateTime(event: de.openbahn.model.StopEvent): LocalDateTime? {
    val iso = event.prognosedTime?.takeIf { it.isNotBlank() } ?: event.scheduledTime
    return parseJourneyDateTime(iso)
}

/**
 * Builds a notification title with an optional departure countdown on the left or an arrival
 * countdown on the right (never both at once).
 */
fun buildTrackingNotificationTitle(
    routeNames: List<String>,
    baseTitle: String,
    countdown: TrackingTitleCountdown,
    maxLength: Int = 50,
): String {
    val left = countdown.leftSeconds?.let { "${formatTrackingCountdown(it)} · " }
    val right = countdown.rightSeconds?.let { " · ${formatTrackingCountdown(it)}" }
    return when {
        left != null -> {
            val routeMax = (maxLength - left.length).coerceAtLeast(8)
            left + if (routeNames.isEmpty()) baseTitle.take(routeMax) else fitRouteTitle(routeNames, routeMax)
        }
        right != null -> {
            val routeMax = (maxLength - right.length).coerceAtLeast(8)
            val route = if (routeNames.isEmpty()) baseTitle.take(routeMax) else fitRouteTitle(routeNames, routeMax)
            route + right
        }
        else -> baseTitle
    }
}

/** @deprecated Use [buildTrackingNotificationTitle] with [TrackingTitleCountdown]. */
fun titleWithCountdown(
    routeNames: List<String>,
    baseTitle: String,
    countdownSeconds: Long?,
    maxLength: Int = 50,
): String = buildTrackingNotificationTitle(
    routeNames = routeNames,
    baseTitle = baseTitle,
    countdown = TrackingTitleCountdown(leftSeconds = countdownSeconds),
    maxLength = maxLength,
)
