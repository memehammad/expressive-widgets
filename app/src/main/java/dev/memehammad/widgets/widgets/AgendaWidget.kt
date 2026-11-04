package dev.memehammad.widgets.widgets

import android.Manifest
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.widget.RemoteViews
import dev.memehammad.widgets.BaseProvider
import dev.memehammad.widgets.Blob
import dev.memehammad.widgets.Sizes
import dev.memehammad.widgets.blob
import dev.memehammad.widgets.fitDigits
import dev.memehammad.widgets.inner
import dev.memehammad.widgets.size
import dev.memehammad.widgets.textAt
import dev.memehammad.widgets.Cfg
import dev.memehammad.widgets.Kind
import dev.memehammad.widgets.MainActivity
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Widgets
import dev.memehammad.widgets.card
import dev.memehammad.widgets.show
import dev.memehammad.widgets.textColor
import dev.memehammad.widgets.tint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

/** Today's date in a shape plus your upcoming events (next 7 days) from the phone's calendars. */
object AgendaWidget : Kind(
    "agenda", "Agenda", "Today's date in a shape and your next events from the calendars on your phone.",
    R.drawable.ic_calendar, AgendaProvider::class.java,
) {
    data class Event(val id: Long, val title: String, val begin: Long, val end: Long, val allDay: Boolean, val color: Int)

    fun allowed(c: Context) = c.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun events(c: Context, days: Int = 7, max: Int = 20): List<Event> {
        if (!allowed(c)) return emptyList()
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now - 24 * 3600_000L)
            ContentUris.appendId(it, now + days * 24 * 3600_000L)
        }.build()
        val cols = arrayOf(
            CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN, CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.DISPLAY_COLOR,
        )
        val out = mutableListOf<Event>()
        try {
            c.contentResolver.query(uri, cols, "${CalendarContract.Instances.VISIBLE} = 1", null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cur ->
                while (cur.moveToNext()) {
                    val allDay = cur.getInt(4) == 1
                    var begin = cur.getLong(2)
                    var end = cur.getLong(3)
                    if (allDay) { // all-day instances are in UTC; shift to local midnight
                        val off = java.util.TimeZone.getDefault().getOffset(begin)
                        begin -= off
                        end -= off
                    }
                    if (end <= now) continue
                    out += Event(cur.getLong(0), cur.getString(1)?.ifBlank { null } ?: "(No title)", begin, end, allDay, cur.getInt(5))
                    if (out.size >= max) break
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun dayIndex(ms: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return c.get(Calendar.YEAR) * 400 + c.get(Calendar.DAY_OF_YEAR)
    }

    fun whenLabel(context: Context, e: Event, now: Long = System.currentTimeMillis()): String {
        val tf = SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", Locale.getDefault())
        val days = dayIndex(e.begin) - dayIndex(now)
        val day = when {
            e.begin <= now -> if (e.allDay) "Today" else "Now"
            days == 0 -> "Today"
            days == 1 -> "Tomorrow"
            else -> SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(e.begin))
        }
        return when {
            e.allDay -> "$day · all day"
            e.begin <= now -> "Now · until ${tf.format(Date(e.end))}"
            else -> "$day · ${tf.format(Date(e.begin))} – ${tf.format(Date(e.end))}"
        }
    }

    override fun defaults() = Cfg().with("shape", "cookie12").with("role", "primary").with("card", true)

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val events = events(context)
        return Sizes.sized(context, widgetId) { w, h ->
            if (w >= 200f && h >= 150f) build(context, widgetId, cfg, R.layout.widget_agenda, events, 60f, false)
            else build(context, widgetId, cfg, R.layout.widget_agenda_small, events, min(w - 16f, h - 66f).coerceAtLeast(36f), true)
        }
    }

    private fun build(
        context: Context, widgetId: Int, cfg: Cfg, layout: Int, events: List<Event>, d: Float, small: Boolean,
    ): RemoteViews {
        val v = RemoteViews(context.packageName, layout)
        v.card(cfg)
        val p = cfg.paint()
        val now = System.currentTimeMillis()
        val spin = cfg.bool("animate", false)
        v.size(R.id.a_frame, d, d)
        v.blob(R.id.a_blob, Blob.of(context, cfg.shape, p.fill, spin))
        val i = cfg.shape.inner(0f, 0f, d, spin)
        v.setTextViewText(R.id.a_day, SimpleDateFormat("d", Locale.getDefault()).format(Date(now)))
        v.textColor(R.id.a_day, p.on)
        if (small) {
            // Day number with the short weekday under it, both inside the shape.
            val ts = min(fitDigits(i.side * 0.78f, 2f), i.side * 0.6f)
            v.textAt(R.id.a_day, i.cx, i.cy - i.side * 0.12f, d, ts)
            v.textAt(R.id.a_weekday, i.cx, i.cy + i.side * 0.36f, d, (i.side * 0.2f).coerceIn(8f, 16f))
            v.textColor(R.id.a_weekday, p.on)
            v.setTextViewText(R.id.a_weekday, SimpleDateFormat("EEE", Locale.getDefault()).format(Date(now)))
        } else {
            v.textAt(R.id.a_day, i.cx, i.cy, d, min(fitDigits(i.side, 2f), 28f))
            v.setTextViewText(R.id.a_weekday, SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(now)))
        }
        v.setTextViewText(R.id.a_month, SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(now)))

        val next = events.firstOrNull()
        v.setTextViewText(
            R.id.a_next,
            when {
                !allowed(context) -> "Allow calendar access in the app"
                next == null -> "Nothing planned this week"
                else -> next.title
            },
        )
        v.setTextViewText(R.id.a_next_when, next?.let { whenLabel(context, it, now) } ?: "")

        val openCalendar = Widgets.activity(
            context, 6000 + widgetId,
            if (allowed(context)) Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())
            else Intent(context, MainActivity::class.java),
        )
        v.setOnClickPendingIntent(android.R.id.background, openCalendar)
        v.setOnClickPendingIntent(R.id.a_header, openCalendar)

        // Event list (full layout only; RemoteViews skips the calls on the small one).
        val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1)
        for (e in events) {
            val row = RemoteViews(context.packageName, R.layout.agenda_row)
            row.setInt(R.id.ar_dot, "setColorFilter", e.color or 0xFF000000.toInt())
            row.setTextViewText(R.id.ar_title, e.title)
            row.setTextViewText(R.id.ar_when, whenLabel(context, e, now))
            row.setOnClickFillInIntent(
                R.id.ar_root,
                Intent().setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, e.id))
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.begin)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.end),
            )
            items.addItem(e.id * 31 + e.begin / 60_000, row)
        }
        v.setRemoteAdapter(R.id.a_list, items.build())
        v.setPendingIntentTemplate(
            R.id.a_list,
            PendingIntent.getActivity(
                context, 6500 + widgetId, Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        v.setEmptyView(R.id.a_list, R.id.a_empty)
        v.setTextViewText(
            R.id.a_empty,
            if (allowed(context)) "Nothing planned this week" else "Open Expressive Widgets to allow calendar access",
        )
        v.show(R.id.a_next_box, true)
        return v
    }
}

class AgendaProvider : BaseProvider("agenda")
