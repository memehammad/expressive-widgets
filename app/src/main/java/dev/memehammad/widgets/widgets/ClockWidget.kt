package dev.memehammad.widgets.widgets

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews
import dev.memehammad.widgets.BaseProvider
import dev.memehammad.widgets.Cfg
import dev.memehammad.widgets.Kind
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Widgets
import dev.memehammad.widgets.card
import dev.memehammad.widgets.show
import dev.memehammad.widgets.textColor
import dev.memehammad.widgets.tint

/** Big clock inside an expressive shape. Tap opens the clock app. */
object ClockWidget : Kind(
    "clock", "Shape clock", "The time inside any shape, inline or hours over minutes, with the date.",
    R.drawable.ic_clock, ClockProvider::class.java,
) {
    override fun defaults() = Cfg().with("shape", "cookie9").with("role", "primary").with("layout", "stacked")

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val stacked = cfg.str("layout", "stacked") == "stacked"
        val v = RemoteViews(context.packageName, if (stacked) R.layout.widget_clock_stack else R.layout.widget_clock)
        v.card(cfg)
        val p = cfg.paint()
        v.setImageViewResource(R.id.c_shape, cfg.shape.res)
        v.tint(R.id.c_shape, p.fill)
        for (id in intArrayOf(R.id.c_time, R.id.c_hour, R.id.c_min, R.id.c_date)) v.textColor(id, p.on)
        v.show(R.id.c_date, cfg.bool("date", true))
        v.setOnClickPendingIntent(
            android.R.id.background,
            Widgets.activity(context, widgetId, Intent(AlarmClock.ACTION_SHOW_ALARMS)),
        )
        return v
    }
}

class ClockProvider : BaseProvider("clock")
