package de.openbahn.navigator.tracking

import de.openbahn.navigator.data.TrackedJourneyRepository
import de.openbahn.navigator.data.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class TrackedJourneyDelayCheckUseCase(
    private val repository: TrackedJourneyRepository,
    private val refreshUseCase: TrackedJourneyRefreshUseCase,
    private val userPreferences: UserPreferencesRepository,
    private val delayNotifier: DelayNotificationNotifier,
    private val platformNotifier: PlatformChangeNotifier,
) {
    suspend fun run(): Int {
        repository.pruneArrivedInternal()
        val incrementMinutes = userPreferences.delayNotificationIncrementMinutes.first()
        var notificationsPosted = 0
        repository.getActiveForWorker().forEach { tracked ->
            val token = tracked.refreshToken?.takeIf { it.isNotBlank() } ?: return@forEach
            val result = refreshUseCase.refreshAndCheckNotifications(
                entityId = tracked.id,
                refreshToken = token,
                notificationIncrementMinutes = incrementMinutes,
            ) ?: return@forEach
            result.platformChanges.forEach { change ->
                platformNotifier.showChange(tracked.id, tracked.fromName, tracked.toName, change)
                notificationsPosted++
            }
            val delayMinutes = result.delayMinutes ?: return@forEach
            delayNotifier.showDelay(tracked.id, tracked.fromName, tracked.toName, delayMinutes)
            notificationsPosted++
        }
        return notificationsPosted
    }
}
