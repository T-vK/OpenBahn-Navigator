package de.openbahn.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JourneyRealtimeTest {
    @Test
    fun delayMinutesFromTimes_computesPositiveDelay() {
        assertEquals(7, delayMinutesFromTimes("2026-05-30T12:00:00", "2026-05-30T12:07:00"))
    }

    @Test
    fun delayMinutesFromTimes_returnsNullWhenOnTime() {
        assertNull(delayMinutesFromTimes("2026-05-30T12:00:00", "2026-05-30T12:00:00"))
    }

    @Test
    fun withRealtimeFrom_prefersRefreshedDelay() {
        val search = Journey(
            id = "j1",
            legs = listOf(
                Leg(
                    origin = StopEvent("A", scheduledTime = "2026-05-30T12:00:00"),
                    destination = StopEvent("B", scheduledTime = "2026-05-30T14:00:00"),
                ),
            ),
            durationMinutes = 120,
            transfers = 0,
            departure = "2026-05-30T12:00:00",
            arrival = "2026-05-30T14:00:00",
        )
        val refreshed = search.copy(
            legs = listOf(
                search.legs.single().copy(
                    origin = StopEvent(
                        "A",
                        scheduledTime = "2026-05-30T12:00:00",
                        prognosedTime = "2026-05-30T12:09:00",
                        delayMinutes = 9,
                    ),
                ),
            ),
        )
        val merged = search.withRealtimeFrom(refreshed)
        assertEquals("j1", merged.id)
        assertEquals(9, merged.legs.single().origin.delayMinutes)
    }

    @Test
    fun withRealtimeFrom_appliesPlatformChangeWithoutDelayUpdate() {
        val search = Journey(
            id = "j1",
            legs = listOf(
                Leg(
                    origin = StopEvent(
                        "A",
                        scheduledTime = "2026-05-30T12:00:00",
                        platform = "3",
                    ),
                    destination = StopEvent("B", scheduledTime = "2026-05-30T14:00:00"),
                ),
            ),
            durationMinutes = 120,
            transfers = 0,
            departure = "2026-05-30T12:00:00",
            arrival = "2026-05-30T14:00:00",
        )
        val refreshed = search.copy(
            legs = listOf(
                search.legs.single().copy(
                    origin = search.legs.single().origin.copy(platform = "7"),
                ),
            ),
        )

        val merged = search.withRealtimeFrom(refreshed)

        assertEquals("7", merged.legs.single().origin.platform)
        assertNull(merged.legs.single().origin.delayMinutes)
    }

    @Test
    fun withBoardRealtime_appliesPlatformWithoutDelay() {
        val stop = StopEvent("A", scheduledTime = "2026-05-30T12:00:00", platform = "3")

        val merged = stop.withBoardRealtime(
            scheduled = "2026-05-30T12:00:00",
            prognosed = null,
            delayMinutes = null,
            platform = "7",
        )

        assertEquals("7", merged.platform)
        assertNull(merged.delayMinutes)
    }
}
