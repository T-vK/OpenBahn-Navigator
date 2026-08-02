package de.openbahn.navigator.tracking

import de.openbahn.model.Journey
import de.openbahn.model.Leg
import de.openbahn.model.StopEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlatformChangeDetectorTest {

    @Test
    fun detect_reportsPlatformChangeAtOrigin() {
        val before = journey(originPlatform = "3")
        val after = journey(originPlatform = "7")

        val changes = PlatformChangeDetector.detect(before, after)

        assertEquals(1, changes.size)
        assertEquals("Hamburg Hbf", changes.single().stopName)
        assertEquals("3", changes.single().oldPlatform)
        assertEquals("7", changes.single().newPlatform)
    }

    @Test
    fun detect_ignoresFirstPlatformAssignment() {
        val before = journey(originPlatform = null)
        val after = journey(originPlatform = "7")

        assertTrue(PlatformChangeDetector.detect(before, after).isEmpty())
    }

    @Test
    fun detect_reportsIntermediateStopPlatformChange() {
        val before = journey(viaPlatform = "5")
        val after = journey(viaPlatform = "8")

        val changes = PlatformChangeDetector.detect(before, after)

        assertEquals(1, changes.size)
        assertEquals("Hannover Hbf", changes.single().stopName)
        assertEquals("5", changes.single().oldPlatform)
        assertEquals("8", changes.single().newPlatform)
    }

    private fun journey(
        originPlatform: String? = "3",
        viaPlatform: String? = null,
    ): Journey = Journey(
        id = "j1",
        legs = listOf(
            Leg(
                origin = StopEvent(
                    name = "Hamburg Hbf",
                    scheduledTime = "2026-06-01T08:00:00",
                    platform = originPlatform,
                ),
                destination = StopEvent(
                    name = "Berlin Hbf",
                    scheduledTime = "2026-06-01T11:00:00",
                    platform = "2",
                ),
                intermediateStops = listOf(
                    StopEvent(
                        name = "Hannover Hbf",
                        scheduledTime = "2026-06-01T09:20:00",
                        platform = viaPlatform,
                    ),
                ),
            ),
        ),
        durationMinutes = 180,
        transfers = 0,
        departure = "2026-06-01T08:00:00",
        arrival = "2026-06-01T11:00:00",
    )
}
