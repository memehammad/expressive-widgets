package dev.memehammad.widgets.widgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.memehammad.widgets.BaseProvider
import dev.memehammad.widgets.Blob
import dev.memehammad.widgets.Cfg
import dev.memehammad.widgets.DataSources
import dev.memehammad.widgets.Health
import dev.memehammad.widgets.Kind
import dev.memehammad.widgets.Liquid
import dev.memehammad.widgets.MainActivity
import dev.memehammad.widgets.Morphs
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Role
import dev.memehammad.widgets.Shapes
import dev.memehammad.widgets.Sizes
import dev.memehammad.widgets.Widgets
import dev.memehammad.widgets.blob
import dev.memehammad.widgets.card
import dev.memehammad.widgets.fitDigits
import dev.memehammad.widgets.floatOnLiquid
import dev.memehammad.widgets.inner
import dev.memehammad.widgets.place
import dev.memehammad.widgets.show
import dev.memehammad.widgets.size
import dev.memehammad.widgets.textAt
import dev.memehammad.widgets.textColor
import dev.memehammad.widgets.tint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/** Opens the app a health number came from, else a fitness app, else this app. */
private fun healthIntent(c: Context, pkg: String? = null): Intent {
    val pm = c.packageManager
    for (p in listOfNotNull(pkg, "com.google.android.apps.fitness", "com.fitbit.FitbitMobile", "com.sec.android.app.shealth")) {
        pm.getLaunchIntentForPackage(p)?.let { return it }
    }
    return Intent(c, MainActivity::class.java)
}

private fun fmt(n: Long) = NumberFormat.getIntegerInstance().format(n)

private fun km(v: Double) = if (v >= 10) "%.0f km".format(v) else "%.1f km".format(v)

private fun minutes(ms: Long): String {
    val m = ms / 60_000
    return if (m < 60) "$m min" else "${m / 60} h ${m % 60}"
}

/** Steps today as liquid filling a shape towards your goal; bigger sizes add distance, calories and the week. */
object StepsWidget : Kind(
    "steps", "Steps", "Today's steps fill a shape towards your goal. Bigger sizes add distance, calories and your week. From Health Connect.",
    R.drawable.ic_walk, StepsProvider::class.java,
) {
    init {
        DataSources.register { ctx -> Health.refresh(ctx) }
    }

    val goals = listOf(4000, 6000, 8000, 10000, 12000, 15000)

    override fun defaults() = Cfg().with("shape", "clover8").with("role", "primary").with("role2", "tertiary")
        .with("goal", 8000).with("week", true)

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val data = Health.cached(context)
        return Sizes.sized(context, widgetId) { w, h -> build(context, cfg, data, w, h) }
    }

    private fun build(c: Context, cfg: Cfg, data: Health.Data?, w: Float, h: Float): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_steps)
        v.card(c, cfg)
        v.size(R.id.s_stage, w, h)
        val pad = if (cfg.card) 10f else 4f
        val steps = data?.today?.steps ?: 0L
        val goal = cfg.int("goal", 8000).coerceAtLeast(500)
        val pct = (steps * 100 / goal).toInt().coerceIn(0, 100)
        val done = steps >= goal
        val big = w >= 200f && h >= 170f
        val wide = !big && w >= 180f && w > h * 1.6f
        val d = when {
            big -> min(h * 0.58f, w * 0.45f)
            wide -> h - 2 * pad
            else -> min(w, h) - 2 * pad
        }
        val x = if (big || wide) pad + 2f else (w - d) / 2
        val y = if (big) pad + 2f else (h - d) / 2
        val shape = cfg.shape.takeIf { Liquid.supports(it.id) } ?: Shapes.byId("clover8")
        // Soft: pale shape with strong liquid. Bold: strong shape with pale liquid.
        val track = cfg.paint(bold = cfg.bold)
        val liquid = cfg.paint(bold = !cfg.bold)
        val role2 = cfg.paint(Role.byId(cfg.str("role2", "tertiary")), true)

        v.place(R.id.s_track, x, y, d, d)
        v.place(R.id.s_liquid, x, y, d, d)
        v.blob(R.id.s_track, Blob.static(c, shape, track.fill))
        v.blob(R.id.s_liquid, Blob.animated(c, Liquid.layout(shape.id, pct), liquid.fill))

        val i = shape.inner(x, y, d, false)
        val ts = min(fitDigits(i.side, 5.2f), d * 0.24f)
        v.setTextViewText(R.id.s_num, if (data == null) "–" else fmt(steps))
        val (by, onLiquid) = floatOnLiquid(y, d, pct, i.cy + ts * 0.16f, ts * 0.8f)
        val shift = by - (i.cy + ts * 0.16f)
        val textOn = if (onLiquid) liquid.on else track.on
        v.textAt(R.id.s_num, i.cx, i.cy - ts * 0.3f + shift, d, ts)
        v.textColor(R.id.s_num, textOn)
        v.setTextViewText(
            R.id.s_sub,
            when {
                data != null -> if (done) "goal!" else "of ${if (goal % 1000 == 0) "${goal / 1000}k" else fmt(goal.toLong())}"
                !Health.available(c) -> "no Health Connect"
                else -> "connect in app"
            },
        )
        v.textAt(R.id.s_sub, i.cx, i.cy + ts * 0.62f + shift, d, (ts * 0.42f).coerceAtLeast(9f))
        v.textColor(R.id.s_sub, textOn)

        // Goal reached: a turning, breathing sun with a tick.
        v.show(R.id.s_badge, done)
        v.show(R.id.s_badge_icon, done)
        if (done) {
            val bs = (d * 0.36f).coerceAtMost(54f)
            val bx = min(x + d - bs * 0.8f, w - bs)
            val by = max(0f, y - bs * 0.15f)
            v.place(R.id.s_badge, bx, by, bs, bs)
            v.blob(R.id.s_badge, Blob.animated(c, Morphs.sun, role2.fill))
            v.place(R.id.s_badge_icon, bx + bs * 0.25f, by + bs * 0.25f, bs * 0.5f, bs * 0.5f)
            v.tint(R.id.s_badge_icon, role2.on)
        }

        v.show(R.id.s_info, big || wide)
        if (big || wide) {
            val ix = x + d + 14f
            v.place(R.id.s_info, ix, if (big) y else 0f, w - ix - pad, if (big) d else h)
            v.setTextViewText(R.id.s_title, if (data == null) "Steps" else "${fmt(steps)} steps")
            val chips = listOfNotNull(
                data?.today?.km?.takeIf { it > 0 }?.let { km(it) },
                data?.today?.kcal?.takeIf { it > 0 }?.let { "%.0f kcal".format(it) },
                if (data == null) null else if (done) "Goal reached" else "${fmt(goal - steps)} to go",
            )
            listOf(R.id.s_c0, R.id.s_c1, R.id.s_c2).forEachIndexed { k, id ->
                v.show(id, k < chips.size)
                if (k < chips.size) v.setTextViewText(id, chips[k])
            }
        }

        val showWeek = big && cfg.bool("week", true) && data != null
        v.show(R.id.s_week, showWeek)
        if (showWeek && data != null) {
            val top = y + d + 10f
            val wh = h - top - pad
            v.place(R.id.s_week, pad + 2f, top, w - 2 * pad - 4f, wh)
            val maxV = max(goal.toLong(), data.week.maxOrNull() ?: 0L).toFloat()
            val bars = intArrayOf(R.id.s_bar0, R.id.s_bar1, R.id.s_bar2, R.id.s_bar3, R.id.s_bar4, R.id.s_bar5, R.id.s_bar6)
            val days = intArrayOf(R.id.s_day0, R.id.s_day1, R.id.s_day2, R.id.s_day3, R.id.s_day4, R.id.s_day5, R.id.s_day6)
            val today = LocalDate.now()
            for (k in 0..6) {
                val value = data.week.getOrElse(k) { 0L }.let { if (k == 6) max(it, steps) else it }
                val barH = max(6f, (wh - 18f) * value / maxV)
                v.setViewLayoutHeight(bars[k], barH, android.util.TypedValue.COMPLEX_UNIT_DIP)
                v.tint(
                    bars[k],
                    when {
                        value >= goal -> role2.fill
                        k == 6 -> liquid.fill
                        else -> track.fill
                    },
                )
                v.setTextViewText(days[k], today.minusDays((6 - k).toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()))
            }
        }

        v.setOnClickPendingIntent(android.R.id.background, Widgets.activity(c, 9500, healthIntent(c)))
        return v
    }
}

class StepsProvider : BaseProvider("steps")

/** Your latest workout (from Strava, Fit, Samsung Health, ... via Health Connect) in a shape for its sport. */
object ActivityWidget : Kind(
    "activity", "Workout", "Your latest workout in a shape for its sport, with this week's total. Strava, Fit and others via Health Connect.",
    R.drawable.ic_run, ActivityProvider::class.java,
) {
    init {
        DataSources.register { ctx -> Health.refresh(ctx) }
    }

    override fun defaults() = Cfg().with("shape", "auto").with("role", "tertiary").with("animate", false)

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val data = Health.cached(context)
        return Sizes.sized(context, widgetId) { w, h -> build(context, cfg, data, w, h) }
    }

    private fun build(c: Context, cfg: Cfg, data: Health.Data?, w: Float, h: Float): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_activity)
        v.card(c, cfg)
        v.size(R.id.x_stage, w, h)
        val pad = if (cfg.card) 10f else 4f
        val s = data?.sessions?.firstOrNull()
        val (icon, typeShape) = s?.let { Health.typeLook(it.type) } ?: (R.drawable.ic_bolt to "cookie9")
        val shape = if (cfg.str("shape", "auto") == "auto") Shapes.byId(typeShape) else cfg.shape
        val spin = cfg.bool("animate", false)
        val p = cfg.paint()
        val wide = w >= 180f && w > h * 1.5f
        val d = if (wide) h - 2 * pad else min(w, h) - 2 * pad
        val x = if (wide) pad + 2f else (w - d) / 2
        val y = (h - d) / 2

        v.place(R.id.x_blob, x, y, d, d)
        v.blob(R.id.x_blob, Blob.of(c, shape, p.fill, spin))
        val i = shape.inner(x, y, d, spin)
        val iconSize = i.side * 0.5f
        v.place(R.id.x_icon, i.cx - iconSize / 2, i.cy - i.side * 0.48f, iconSize, iconSize)
        v.setImageViewResource(R.id.x_icon, icon)
        v.tint(R.id.x_icon, p.on)
        v.setTextViewText(
            R.id.x_num,
            when {
                s == null -> "–"
                s.km != null -> if (s.km >= 10) "%.0f".format(s.km) else "%.1f".format(s.km)
                else -> "${(s.end - s.start) / 60_000}'"
            },
        )
        v.textAt(R.id.x_num, i.cx, i.cy + i.side * 0.27f, d, min(fitDigits(i.side, 3.6f), i.side * 0.38f))
        v.textColor(R.id.x_num, p.on)

        v.show(R.id.x_info, wide)
        if (wide) {
            val ix = x + d + 14f
            v.place(R.id.x_info, ix, 0f, w - ix - pad, h)
            v.setTextViewText(
                R.id.x_title,
                s?.title ?: if (Health.available(c)) "No workouts yet" else "Health Connect needed",
            )
            val app = s?.let {
                runCatching { c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(it.app, 0)).toString() }.getOrNull()
            }
            val chips = listOfNotNull(
                s?.let { listOfNotNull(it.km?.let { k -> km(k) }, minutes(it.end - it.start)).joinToString(" · ") },
                s?.let { whenLabel(it.start) + (app?.let { a -> " · $a" } ?: "") },
                data?.takeIf { it.weekCount > 0 }?.let {
                    "This week: ${it.weekCount}" + if (it.weekKm > 0) " · ${km(it.weekKm)}" else ""
                },
            )
            listOf(R.id.x_c0, R.id.x_c1, R.id.x_c2).forEachIndexed { k, id ->
                v.show(id, k < chips.size)
                if (k < chips.size) v.setTextViewText(id, chips[k])
            }
        }
        v.setOnClickPendingIntent(android.R.id.background, Widgets.activity(c, 9600, healthIntent(c, s?.app)))
        return v
    }

    private fun whenLabel(ms: Long): String {
        val day = LocalDate.now().toEpochDay() - java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
        return when (day) {
            0L -> "Today $time"
            1L -> "Yesterday"
            else -> SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(ms))
        }
    }
}

class ActivityProvider : BaseProvider("activity")
