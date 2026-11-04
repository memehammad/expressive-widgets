package dev.memehammad.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import dev.memehammad.widgets.widgets.BatteryWidget
import dev.memehammad.widgets.widgets.ClockWidget
import dev.memehammad.widgets.widgets.TogglesWidget
import dev.memehammad.widgets.widgets.WorldClockWidget
import dev.memehammad.widgets.widgets.WeatherWidget
import dev.memehammad.widgets.widgets.AgendaWidget

/** One widget type: how to draw it, plus what the app's catalogue says about it. */
abstract class Kind(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int,
    val provider: Class<out AppWidgetProvider>,
) {
    abstract fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews

    /** Default settings for a new widget of this kind. */
    open fun defaults(): Cfg = Cfg()
}

object Widgets {
    val kinds: List<Kind> by lazy { listOf(ClockWidget, WeatherWidget, AgendaWidget, WorldClockWidget, BatteryWidget, TogglesWidget) }

    fun kindOf(context: Context, widgetId: Int): Kind? {
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId) ?: return null
        return kinds.firstOrNull { it.provider.name == info.provider.className }
    }

    fun cfg(context: Context, widgetId: Int, kind: Kind): Cfg {
        val c = Configs.get(context, widgetId)
        return if (c.json.length() == 0) kind.defaults() else c
    }

    fun update(context: Context, kind: Kind, ids: IntArray? = null) {
        val mgr = AppWidgetManager.getInstance(context)
        val all = ids ?: mgr.getAppWidgetIds(ComponentName(context, kind.provider))
        for (id in all) {
            val views = try {
                kind.render(context, id, cfg(context, id, kind))
            } catch (e: Exception) {
                RemoteViews(context.packageName, R.layout.w_error)
            }
            mgr.updateAppWidget(id, views)
        }
    }

    fun updateAll(context: Context) = kinds.forEach { update(context, it) }

    fun update(context: Context, widgetId: Int) {
        kindOf(context, widgetId)?.let { update(context, it, intArrayOf(widgetId)) }
    }

    /** Opens the widget's settings screen (tap action for most widgets). */
    fun configIntent(context: Context, widgetId: Int): PendingIntent = PendingIntent.getActivity(
        context, widgetId,
        Intent(context, ConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun activity(context: Context, code: Int, intent: Intent): PendingIntent = PendingIntent.getActivity(
        context, code, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

/** Base provider: draws through the registry and keeps the periodic refresh running. */
abstract class BaseProvider(private val kindId: String) : AppWidgetProvider() {
    private fun kind() = Widgets.kinds.first { it.id == kindId }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Widgets.update(context, kind(), appWidgetIds)
        Refresher.ensure(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, mgr: AppWidgetManager, id: Int, newOptions: Bundle) {
        Widgets.update(context, kind(), intArrayOf(id))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { Configs.delete(context, it) }
    }
}
