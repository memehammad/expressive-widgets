package dev.memehammad.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Refreshes data-backed widgets (battery, weather, calendar, Home Assistant) every 15 minutes. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DataSources.refreshAll(applicationContext)
        Widgets.updateAll(applicationContext)
        return Result.success()
    }
}

object Refresher {
    fun ensure(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build(),
        )
    }
}

/** Redraw everything after reboot, time or timezone changes (world clocks, day labels). */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Refresher.ensure(context)
        Widgets.updateAll(context)
    }
}

/** Hook for widgets that fetch data; filled in by later widget types. */
object DataSources {
    private val sources = mutableListOf<suspend (Context) -> Unit>()

    fun register(source: suspend (Context) -> Unit) {
        sources += source
    }

    suspend fun refreshAll(context: Context) {
        Widgets.kinds.size // make sure every widget type has registered its data source
        for (s in sources) runCatching { s(context) }
    }
}
