package dev.memehammad.widgets.widgets

import android.content.Context
import android.widget.RemoteViews
import dev.memehammad.widgets.BaseProvider
import dev.memehammad.widgets.Blob
import dev.memehammad.widgets.Shapes
import dev.memehammad.widgets.blob
import dev.memehammad.widgets.Cfg
import dev.memehammad.widgets.Kind
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Role
import dev.memehammad.widgets.Widgets
import dev.memehammad.widgets.card
import dev.memehammad.widgets.show
import dev.memehammad.widgets.textColor
import dev.memehammad.widgets.tint
import java.util.Calendar
import java.util.TimeZone

/** Up to three cities: hour in a shape, city, day and offset, and the time. */
object WorldClockWidget : Kind(
    "world", "World clocks", "Up to three cities with their local time, day and time difference.",
    R.drawable.ic_globe, WorldClockProvider::class.java,
) {
    /** City label -> time zone id. */
    val cities: List<Pair<String, String>> = listOf(
        "London" to "Europe/London", "Berlin" to "Europe/Berlin", "Paris" to "Europe/Paris",
        "Madrid" to "Europe/Madrid", "Rome" to "Europe/Rome", "Amsterdam" to "Europe/Amsterdam",
        "Stockholm" to "Europe/Stockholm", "Helsinki" to "Europe/Helsinki", "Athens" to "Europe/Athens",
        "Istanbul" to "Europe/Istanbul", "Moscow" to "Europe/Moscow", "Dubai" to "Asia/Dubai",
        "Mumbai" to "Asia/Kolkata", "Bangkok" to "Asia/Bangkok", "Singapore" to "Asia/Singapore",
        "Hong Kong" to "Asia/Hong_Kong", "Shanghai" to "Asia/Shanghai", "Seoul" to "Asia/Seoul",
        "Tokyo" to "Asia/Tokyo", "Sydney" to "Australia/Sydney", "Auckland" to "Pacific/Auckland",
        "Honolulu" to "Pacific/Honolulu", "Los Angeles" to "America/Los_Angeles",
        "Vancouver" to "America/Vancouver", "Denver" to "America/Denver", "Chicago" to "America/Chicago",
        "Toronto" to "America/Toronto", "New York" to "America/New_York", "Halifax" to "America/Halifax",
        "Mexico City" to "America/Mexico_City", "Bogotá" to "America/Bogota", "São Paulo" to "America/Sao_Paulo",
        "Buenos Aires" to "America/Argentina/Buenos_Aires", "Cairo" to "Africa/Cairo",
        "Johannesburg" to "Africa/Johannesburg", "Lagos" to "Africa/Lagos", "Nairobi" to "Africa/Nairobi",
        "Reykjavík" to "Atlantic/Reykjavik", "UTC" to "UTC",
    )

    override fun defaults() = Cfg().with("card", true).with("shape", "cookie6").with("role", "tertiary")
        .with("cities", listOf("London", "New York", "Tokyo"))

    private val rows = arrayOf(
        intArrayOf(R.id.wr0, R.id.ws0, R.id.wh0, R.id.wc0, R.id.wd0, R.id.wt0),
        intArrayOf(R.id.wr1, R.id.ws1, R.id.wh1, R.id.wc1, R.id.wd1, R.id.wt1),
        intArrayOf(R.id.wr2, R.id.ws2, R.id.wh2, R.id.wc2, R.id.wd2, R.id.wt2),
    )

    /** "Today, +5 h" / "Tomorrow, −3 h 30" relative to this phone. */
    private fun dayLabel(tz: TimeZone, now: Long): String {
        val here = Calendar.getInstance()
        val there = Calendar.getInstance(tz)
        here.timeInMillis = now
        there.timeInMillis = now
        val dayDiff = there.get(Calendar.YEAR) * 400 + there.get(Calendar.DAY_OF_YEAR) -
            (here.get(Calendar.YEAR) * 400 + here.get(Calendar.DAY_OF_YEAR))
        val day = when {
            dayDiff > 0 -> "Tomorrow"
            dayDiff < 0 -> "Yesterday"
            else -> "Today"
        }
        val diffMin = (tz.getOffset(now) - TimeZone.getDefault().getOffset(now)) / 60_000
        if (diffMin == 0) return day
        val sign = if (diffMin > 0) "+" else "−"
        val h = kotlin.math.abs(diffMin) / 60
        val m = kotlin.math.abs(diffMin) % 60
        return "$day, $sign${h} h" + if (m != 0) " $m" else ""
    }

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_world)
        v.card(cfg)
        val chosen = cfg.list("cities", listOf("London", "New York", "Tokyo"))
            .mapNotNull { name -> cities.firstOrNull { it.first == name } }
        val roles = listOf(cfg.role, Role.entries[(cfg.role.ordinal + 1) % 3], Role.entries[(cfg.role.ordinal + 2) % 3])
        val now = System.currentTimeMillis()
        rows.forEachIndexed { i, ids ->
            val row = ids[0]; val shape = ids[1]; val hour = ids[2]; val city = ids[3]; val day = ids[4]; val time = ids[5]
            val c = chosen.getOrNull(i)
            v.show(row, c != null)
            if (c == null) return@forEachIndexed
            val tz = TimeZone.getTimeZone(c.second)
            val p = cfg.paint(if (cfg.bool("mixed", true)) roles[i] else cfg.role)
            val mixed = cfg.bool("mixed", true)
            // Mixed: each city gets its own shape as well as its own colour.
            val trio = listOf(cfg.shape.id) + listOf("clover4", "sunny", "cookie9").filter { it != cfg.shape.id }
            val sh = if (mixed) Shapes.byId(trio[i]) else cfg.shape
            v.blob(shape, Blob.static(context, sh, p.fill))
            v.textColor(hour, p.on)
            v.setString(hour, "setTimeZone", c.second)
            v.setString(time, "setTimeZone", c.second)
            v.setTextViewText(city, c.first)
            v.setTextViewText(day, dayLabel(tz, now))
        }
        v.setOnClickPendingIntent(android.R.id.background, Widgets.configIntent(context, widgetId))
        return v
    }
}

class WorldClockProvider : BaseProvider("world")
