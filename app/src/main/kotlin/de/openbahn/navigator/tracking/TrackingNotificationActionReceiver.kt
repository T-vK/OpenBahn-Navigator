package de.openbahn.navigator.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.openbahn.navigator.R
import de.openbahn.navigator.data.TrackedJourneyRepository
import de.openbahn.navigator.ui.util.formatJourneyShareText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.getKoin

class TrackingNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    TrackingNotificationIntent.ACTION_STOP_TRACKING -> {
                        val repository = getKoin().get<TrackedJourneyRepository>()
                        repository.getActiveForWorker().forEach { entity ->
                            repository.stopTracking(entity.id)
                        }
                    }
                    TrackingNotificationIntent.ACTION_SHARE_TRACKING -> {
                        val repository = getKoin().get<TrackedJourneyRepository>()
                        val tracked = repository.getActiveWithJourneyForWorker()
                        if (tracked.isEmpty()) return@launch
                        val text = tracked.joinToString("\n\n") { item ->
                            formatJourneyShareText(appContext, item.journey)
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val chooser = Intent.createChooser(
                            shareIntent,
                            appContext.getString(R.string.share_journey_chooser),
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        appContext.startActivity(chooser)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
