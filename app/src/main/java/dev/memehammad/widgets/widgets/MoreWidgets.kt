package dev.memehammad.widgets.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.AlarmClock
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.memehammad.widgets.BaseProvider
import dev.memehammad.widgets.Cfg
import dev.memehammad.widgets.Kind
import dev.memehammad.widgets.Liquid
import dev.memehammad.widgets.Morphs
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Role
import dev.memehammad.widgets.Shapes
import dev.memehammad.widgets.Sizes
import dev.memehammad.widgets.Widgets
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.random.Random

private fun timeFormat(c: Context) =
    SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(c)) "HH:mm" else "h:mm", Locale.getDefault())

private fun until(ms: Long): String {
    val min = (ms / 60_000).coerceAtLeast(0)
    return when {
        min < 60 -> "in $min min"
        min < 24 * 60 -> "in ${min / 60} h ${min % 60}"
        else -> "in ${min / (24 * 60)} d ${min / 60 % 24} h"
    }
}

private fun broadcast(c: Context, cls: Class<*>, widgetId: Int, code: Int) = PendingIntent.getBroadcast(
    c, code, Intent(c, cls).putExtra("id", widgetId).setAction("tap:$widgetId"),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

// ---------------------------------------------------------------------------------------------
/** The next alarm from any clock app. */
object AlarmWidget : Kind(
    "alarm", "Next alarm", "Your next alarm in a shape, with how long until it rings.",
    R.drawable.ic_alarm, AlarmProvider::class.java,
) {
    override fun defaults() = Cfg().with("shape", "cookie12").with("role", "secondary").with("animate", true)

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val next = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock
        val now = System.currentTimeMillis()
        val tap = next?.showIntent ?: Widgets.activity(context, 11000 + widgetId, Intent(AlarmClock.ACTION_SHOW_ALARMS))
        return Sizes.sized(context, widgetId) { w, h ->
            val spec = if (next == null) {
                BadgeSpec(
                    cfg.shape, cfg.paint(), cfg.bool("animate", true), icon = R.drawable.ic_alarm, sub = "No alarm",
                    title = "No alarm set", chips = listOf("Tap to set one"), tap = tap,
                )
            } else {
                val time = timeFormat(context).format(Date(next.triggerTime))
                val days = ChronoUnit.DAYS.between(LocalDate.now(), java.time.Instant.ofEpochMilli(next.triggerTime).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                val dayLabel = when (days) {
                    0L -> "Today"
                    1L -> "Tomorrow"
                    else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(next.triggerTime))
                }
                BadgeSpec(
                    cfg.shape, cfg.paint(), cfg.bool("animate", true),
                    num = time, numChars = time.length * 0.9f, sub = until(next.triggerTime - now),
                    title = "$dayLabel, $time", chips = listOf(until(next.triggerTime - now)), tap = tap,
                )
            }
            Badge.build(context, cfg, w, h, spec)
        }
    }
}

class AlarmProvider : BaseProvider("alarm")

/** Redraws the alarm widget when the next alarm changes. */
class AlarmChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Widgets.update(context, AlarmWidget)
}

// ---------------------------------------------------------------------------------------------
/** Days until (or since) a date. */
object CountdownWidget : Kind(
    "countdown", "Countdown", "Days until (or since) a date you care about, in a shape that gets excited as it nears.",
    R.drawable.ic_event, CountdownProvider::class.java,
) {
    override fun defaults() = Cfg().with("shape", "sunny").with("role", "tertiary").with("animate", true)
        .with("title", "Holidays").with("date", LocalDate.now().plusDays(30).toString())

    fun date(cfg: Cfg): LocalDate = runCatching { LocalDate.parse(cfg.str("date", "")) }.getOrElse { LocalDate.now().plusDays(30) }

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val target = date(cfg)
        val days = ChronoUnit.DAYS.between(LocalDate.now(), target)
        val title = cfg.str("title", "Holidays")
        val tap = Widgets.configIntent(context, widgetId)
        val dateLabel = SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault())
            .format(Date(target.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
        return Sizes.sized(context, widgetId) { w, h ->
            val p = cfg.paint()
            val spec = when {
                days == 0L -> BadgeSpec(
                    Shapes.byId("sunny"), cfg.paint(cfg.role, true), animated = Morphs.sun,
                    num = "Today", numChars = 4.6f, sub = title, title = "$title is today!", chips = listOf(dateLabel), tap = tap,
                )
                else -> {
                    val n = abs(days)
                    val chips = listOfNotNull(
                        dateLabel,
                        if (n >= 14) "${n / 7} weeks ${n % 7} days" else null,
                        if (days > 0 && n <= 3) "${n * 24} hours" else null,
                    )
                    BadgeSpec(
                        cfg.shape, p, cfg.bool("animate", true),
                        num = n.toString(), numChars = n.toString().length.toFloat().coerceAtLeast(2f),
                        sub = if (days > 0) (if (n == 1L) "day to go" else "days to go") else (if (n == 1L) "day ago" else "days ago"),
                        title = title, chips = chips,
                        // The last week gets a pulsing badge.
                        badge = if (days in 1..7) Morphs.pulse to R.drawable.ic_event else null,
                        badgePaint = cfg.paint(Role.byId(cfg.str("role2", "primary")), true),
                        tap = tap,
                    )
                }
            }
            Badge.build(context, cfg, w, h, spec)
        }
    }
}

class CountdownProvider : BaseProvider("countdown")

// ---------------------------------------------------------------------------------------------
/** Moon phase, computed on the phone. */
object MoonWidget : Kind(
    "moon", "Moon", "Tonight's moon phase in a shape, with how lit it is and the next full and new moon.",
    R.drawable.ic_moon, MoonProvider::class.java,
) {
    private const val SYNODIC = 29.530588853
    private const val NEW_MOON_2000 = 947_182_440_000L // 2000-01-06 18:14 UTC

    /** 0 = new, 0.5 = full. */
    fun phase(ms: Long = System.currentTimeMillis()): Double {
        val days = (ms - NEW_MOON_2000) / 86_400_000.0
        return ((days / SYNODIC) % 1.0 + 1.0) % 1.0
    }

    fun name(p: Double): String = when {
        p < 0.03 || p > 0.97 -> "New moon"
        p < 0.22 -> "Waxing crescent"
        p < 0.28 -> "First quarter"
        p < 0.47 -> "Waxing gibbous"
        p < 0.53 -> "Full moon"
        p < 0.72 -> "Waning gibbous"
        p < 0.78 -> "Last quarter"
        else -> "Waning crescent"
    }

    override fun defaults() = Cfg().with("shape", "cookie12").with("role", "neutral").with("fill", "bold").with("animate", false)

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val p = phase()
        val lit = ((1 - cos(2 * Math.PI * p)) / 2 * 100).roundToInt()
        val toFull = (((0.5 - p + 1) % 1.0) * SYNODIC).roundToInt()
        val toNew = (((1.0 - p) % 1.0) * SYNODIC).roundToInt()
        val frames = intArrayOf(
            R.drawable.moon_00, R.drawable.moon_01, R.drawable.moon_02, R.drawable.moon_03, R.drawable.moon_04, R.drawable.moon_05,
            R.drawable.moon_06, R.drawable.moon_07, R.drawable.moon_08, R.drawable.moon_09, R.drawable.moon_10, R.drawable.moon_11,
            R.drawable.moon_12, R.drawable.moon_13, R.drawable.moon_14, R.drawable.moon_15, R.drawable.moon_16, R.drawable.moon_17,
            R.drawable.moon_18, R.drawable.moon_19, R.drawable.moon_20, R.drawable.moon_21, R.drawable.moon_22, R.drawable.moon_23,
        )
        val frame = frames[((p * 24).roundToInt()) % 24]
        return Sizes.sized(context, widgetId) { w, h ->
            Badge.build(
                context, cfg, w, h,
                BadgeSpec(
                    cfg.shape, cfg.paint(), cfg.bool("animate", false), icon = frame,
                    sub = if (cfg.bool("label", true)) "$lit%" else null,
                    title = name(p),
                    chips = listOf("$lit% lit", if (toFull in 1..29) "Full in $toFull d" else "Full moon", "New in $toNew d"),
                    tap = Widgets.configIntent(context, widgetId),
                ),
            )
        }
    }
}

class MoonProvider : BaseProvider("moon")

// ---------------------------------------------------------------------------------------------
/** How much of the year / month / week / day has passed, as liquid in a shape. */
object ProgressWidget : Kind(
    "progress", "Progress", "How much of the year, month, week or day is done, as liquid in a shape.",
    R.drawable.ic_progress, ProgressProvider::class.java,
) {
    val periods = listOf("year" to "Year", "month" to "Month", "week" to "Week", "day" to "Day")

    override fun defaults() = Cfg().with("shape", "cookie4").with("role", "primary").with("period", "year")

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val period = cfg.str("period", "year")
        val (start, end) = when (period) {
            "month" -> today.withDayOfMonth(1).atStartOfDay() to today.withDayOfMonth(1).plusMonths(1).atStartOfDay()
            "week" -> today.with(DayOfWeek.MONDAY).atStartOfDay() to today.with(DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay()
            "day" -> today.atStartOfDay() to today.plusDays(1).atStartOfDay()
            else -> today.withDayOfYear(1).atStartOfDay() to today.withDayOfYear(1).plusYears(1).atStartOfDay()
        }
        val total = ChronoUnit.MINUTES.between(start, end).toDouble()
        val done = ChronoUnit.MINUTES.between(start, now).toDouble()
        val pct = (done / total * 100).toInt().coerceIn(0, 100)
        val label = when (period) {
            "month" -> today.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
            "week" -> "Week ${today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
            "day" -> "Today"
            else -> today.year.toString()
        }
        val leftDays = ChronoUnit.DAYS.between(today, end.toLocalDate())
        val chips = when (period) {
            "day" -> {
                val left = ChronoUnit.MINUTES.between(now, end)
                listOf("${left / 60} h ${left % 60} min left")
            }
            "year" -> listOf("Day ${today.dayOfYear} of ${today.lengthOfYear()}", "$leftDays days left")
            else -> listOf("$leftDays days left")
        }
        val shape = cfg.shape.takeIf { Liquid.supports(it.id) } ?: Shapes.byId("cookie4")
        return Sizes.sized(context, widgetId) { w, h ->
            Badge.build(
                context, cfg, w, h,
                BadgeSpec(
                    shape, cfg.paint(bold = !cfg.bold), liquid = pct, track = cfg.paint(bold = cfg.bold),
                    num = "$pct%", numChars = 3.6f, sub = label,
                    title = "$label is $pct% done", chips = chips,
                    tap = Widgets.configIntent(context, widgetId),
                ),
            )
        }
    }
}

class ProgressProvider : BaseProvider("progress")

// ---------------------------------------------------------------------------------------------
/** Tap to start a focus timer; the shape turns while it runs. */
object TimerWidget : Kind(
    "timer", "Focus timer", "Tap to start a focus timer; the shape turns while it runs. Tap again to stop.",
    R.drawable.ic_timer, TimerProvider::class.java,
) {
    val lengths = listOf(5, 10, 15, 25, 45, 60)

    override fun defaults() = Cfg().with("shape", "clover8").with("role", "tertiary").with("minutes", 25).with("animate", true)

    private fun prefs(c: Context) = c.getSharedPreferences("timer", Context.MODE_PRIVATE)
    fun endAt(c: Context, id: Int) = prefs(c).getLong("end_$id", 0L)

    fun toggle(c: Context, id: Int) {
        val cfg = Widgets.cfg(c, id, this)
        val end = endAt(c, id)
        val now = SystemClock.elapsedRealtime()
        if (end > now) {
            prefs(c).edit().putLong("end_$id", 0L).apply()
            WorkManager.getInstance(c).cancelUniqueWork("timer_$id")
        } else {
            val ms = cfg.int("minutes", 25) * 60_000L
            prefs(c).edit().putLong("end_$id", now + ms).putBoolean("done_$id", false).apply()
            WorkManager.getInstance(c).enqueueUniqueWork(
                "timer_$id", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<TimerDoneWorker>().setInitialDelay(ms, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf("id" to id)).build(),
            )
        }
        Widgets.update(c, this, intArrayOf(id))
    }

    fun finish(c: Context, id: Int) {
        prefs(c).edit().putLong("end_$id", 0L).putBoolean("done_$id", true).apply()
        Widgets.update(c, this, intArrayOf(id))
    }

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val now = SystemClock.elapsedRealtime()
        val end = endAt(context, widgetId)
        val running = end > now
        val done = !running && prefs(context).getBoolean("done_$widgetId", false)
        val minutes = cfg.int("minutes", 25)
        val tap = broadcast(context, TimerReceiver::class.java, widgetId, 12000 + widgetId)
        val endClock = timeFormat(context).format(Date(System.currentTimeMillis() + (end - now)))
        return Sizes.sized(context, widgetId) { w, h ->
            val spec = when {
                running -> BadgeSpec(
                    cfg.shape, cfg.paint(cfg.role, true), cfg.bool("animate", true),
                    chronoEnd = end, numChars = 4.4f, sub = "focus",
                    title = "Focusing", chips = listOf("Ends $endClock", "Tap to stop"), tap = tap,
                )
                done -> BadgeSpec(
                    Shapes.byId("sunny"), cfg.paint(Role.byId(cfg.str("role2", "primary")), true), animated = Morphs.sun,
                    num = "Done", numChars = 4.4f, sub = "tap to restart",
                    title = "Nice work", chips = listOf("$minutes min focused"), tap = tap,
                )
                else -> BadgeSpec(
                    cfg.shape, cfg.paint(), false, icon = R.drawable.ic_play,
                    num = minutes.toString(), numChars = 2f, sub = "min",
                    title = "Focus · $minutes min", chips = listOf("Tap to start"), tap = tap,
                )
            }
            Badge.build(context, cfg, w, h, spec)
        }
    }
}

class TimerProvider : BaseProvider("timer")

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (id >= 0) TimerWidget.toggle(context, id)
    }
}

class TimerDoneWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val id = inputData.getInt("id", -1)
        if (id >= 0) TimerWidget.finish(applicationContext, id)
        return Result.success()
    }
}

// ---------------------------------------------------------------------------------------------
/** Dice, coin or yes/no/maybe; every roll lands in a new shape and colour. */
object DiceWidget : Kind(
    "dice", "Dice", "Tap to roll a die, flip a coin or let it decide. Every roll lands in a new shape and colour.",
    R.drawable.ic_dice, DiceProvider::class.java,
) {
    val modes = listOf("d6" to "Die", "d20" to "D20", "coin" to "Coin", "decide" to "Yes / no")
    private val funShapes = listOf("cookie9", "clover4", "sunny", "flower", "puffy", "softburst", "ghostish", "heart", "gem", "cookie6", "clover8", "pentagon", "bun", "verysunny")

    override fun defaults() = Cfg().with("shape", "cookie6").with("role", "primary").with("mode", "d6").with("shuffle", true)

    private fun prefs(c: Context) = c.getSharedPreferences("dice", Context.MODE_PRIVATE)

    fun roll(c: Context, id: Int) {
        val cfg = Widgets.cfg(c, id, this)
        val r = when (cfg.str("mode", "d6")) {
            "d20" -> (Random.nextInt(20) + 1).toString()
            "coin" -> if (Random.nextBoolean()) "Heads" else "Tails"
            "decide" -> listOf("Yes", "No", "Maybe", "Yes!", "Nope").random()
            else -> (Random.nextInt(6) + 1).toString()
        }
        prefs(c).edit().putString("r_$id", r).putString("s_$id", funShapes.random()).putInt("c_$id", Random.nextInt(3))
            .putInt("n_$id", prefs(c).getInt("n_$id", 0) + 1).apply()
        Widgets.update(c, this, intArrayOf(id))
    }

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val p = prefs(context)
        val result = p.getString("r_$widgetId", null)
        val shuffle = cfg.bool("shuffle", true) && result != null
        val shape = if (shuffle) Shapes.byId(p.getString("s_$widgetId", null)) else cfg.shape
        val role = if (shuffle) Role.entries[p.getInt("c_$widgetId", 0) % 3] else cfg.role
        val mode = modes.firstOrNull { it.first == cfg.str("mode", "d6") }?.second ?: "Die"
        val tap = broadcast(context, DiceReceiver::class.java, widgetId, 13000 + widgetId)
        return Sizes.sized(context, widgetId) { w, h ->
            Badge.build(
                context, cfg, w, h,
                BadgeSpec(
                    shape, cfg.paint(role, cfg.bold), cfg.bool("animate", false),
                    num = result ?: "?", numChars = (result ?: "?").length.toFloat().coerceAtLeast(1.6f),
                    sub = if (result == null) "tap to roll" else mode.lowercase(),
                    title = if (result == null) "Tap to roll" else "$mode: $result",
                    chips = listOf("Roll #${p.getInt("n_$widgetId", 0)}", "Tap to roll again"),
                    tap = tap,
                ),
            )
        }
    }
}

class DiceProvider : BaseProvider("dice")

class DiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        if (id >= 0) DiceWidget.roll(context, id)
    }
}
