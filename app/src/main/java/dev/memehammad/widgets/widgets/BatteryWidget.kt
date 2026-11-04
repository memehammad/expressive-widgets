package dev.memehammad.widgets.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews
import dev.memehammad.widgets.BaseProvider
import dev.memehammad.widgets.Blob
import dev.memehammad.widgets.Liquid
import dev.memehammad.widgets.Morphs
import dev.memehammad.widgets.Paint
import dev.memehammad.widgets.Role
import dev.memehammad.widgets.Shapes
import dev.memehammad.widgets.Sizes
import dev.memehammad.widgets.blob
import dev.memehammad.widgets.fitDigits
import dev.memehammad.widgets.inner
import dev.memehammad.widgets.place
import dev.memehammad.widgets.size
import dev.memehammad.widgets.textAt
import dev.memehammad.widgets.Cfg
import dev.memehammad.widgets.DataSources
import dev.memehammad.widgets.Kind
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Shell
import dev.memehammad.widgets.Widgets
import dev.memehammad.widgets.card
import dev.memehammad.widgets.show
import dev.memehammad.widgets.textColor
import dev.memehammad.widgets.tint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Battery level as a shape filled with moving liquid (two waves). Wider sizes add charging watts, time to full,
 * temperature, cycle count and (with Shizuku) battery health.
 */
object BatteryWidget : Kind(
    "battery", "Battery", "A shape that fills with your charge. Wide sizes show watts, time to full, temperature, cycles and health.",
    R.drawable.ic_battery, BatteryProvider::class.java,
) {
    data class Info(
        val level: Int,
        val charging: Boolean,
        val watts: Double?,
        val tempC: Double?,
        val cycles: Int?,
        val fullInMs: Long?,
        val healthPct: Int?,
    )

    init {
        // Battery health needs the shell (sysfs isn't readable by apps); refreshed with the widgets.
        DataSources.register { ctx -> refreshHealth(ctx) }
    }

    private fun prefs(c: Context) = c.getSharedPreferences("battery", Context.MODE_PRIVATE)

    suspend fun refreshHealth(context: Context) {
        val last = prefs(context).getLong("health_at", 0L)
        if (System.currentTimeMillis() - last < 6 * 3600_000L) return
        val out = Shell.run(
            "cat /sys/class/power_supply/battery/charge_full /sys/class/power_supply/battery/charge_full_design",
        ) ?: return
        val nums = out.lines().mapNotNull { it.trim().toLongOrNull() }
        if (nums.size >= 2 && nums[1] > 0) {
            val pct = (nums[0] * 100 / nums[1]).toInt().coerceIn(1, 120)
            prefs(context).edit().putInt("health", pct).putLong("health_at", System.currentTimeMillis()).apply()
        }
    }

    fun read(context: Context): Info {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = context.getSystemService(BatteryManager::class.java)
        val level = i?.let {
            val l = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val s = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (l >= 0 && s > 0) l * 100 / s else null
        } ?: bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val voltage = i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0 // mV
        val current = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: Int.MIN_VALUE // µA
        val watts = if (voltage > 0 && current != Int.MIN_VALUE && current != 0) abs(current.toDouble()) * voltage / 1e9 else null
        val temp = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }?.div(10.0)
        val cycles = i?.getIntExtra("android.os.extra.CYCLE_COUNT", -1)?.takeIf { it >= 0 }
        val full = if (charging) bm?.computeChargeTimeRemaining()?.takeIf { it > 0 } else null
        val health = prefs(context).getInt("health", -1).takeIf { it > 0 }
        return Info(level, charging, watts, temp, cycles, full, health)
    }

    override fun defaults() = Cfg().with("shape", "cookie9").with("role", "secondary").with("role2", "tertiary")

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val info = read(context)
        return Sizes.sized(context, widgetId) { w, h -> build(context, widgetId, cfg, info, w, h) }
    }

    private fun build(context: Context, widgetId: Int, cfg: Cfg, info: Info, w: Float, h: Float): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget_battery)
        v.card(cfg)
        v.size(R.id.b_stage, w, h)
        val pad = if (cfg.card) 10f else 4f
        val wide = w >= 180f && w > h * 1.6f
        val d = if (wide) h - 2 * pad else min(w, h) - 2 * pad
        val x = if (wide) pad + 4f else (w - d) / 2
        val y = (h - d) / 2
        val shape = cfg.shape.takeIf { Liquid.supports(it.id) } ?: Shapes.byId("square")

        // Low battery switches to the error colours so it stands out.
        val low = info.level <= 15 && !info.charging
        val liquid = if (low) Paint(R.color.err_accent, R.color.err_on_accent) else cfg.paint(bold = true)
        val track = if (low) Paint(R.color.err_container, R.color.err_on_container) else cfg.paint(bold = false)
        v.place(R.id.b_track, x, y, d, d)
        v.place(R.id.b_liquid, x, y, d, d)
        v.blob(R.id.b_track, Blob.static(context, shape, track.fill))
        v.blob(R.id.b_liquid, Blob.animated(context, Liquid.layout(shape.id, info.level), liquid.fill))

        // Percentage in the middle; its colour follows whichever part it sits on.
        val i = shape.inner(x, y, d, false)
        val ts = min(fitDigits(i.side, 3.9f), d * 0.3f)
        v.setTextViewText(R.id.b_pct, "${info.level}%")
        v.textAt(R.id.b_pct, i.cx, i.cy, d, ts)
        v.textColor(R.id.b_pct, if (info.level >= 55) liquid.on else track.on)

        // Charging: a morphing, turning badge with a bolt, half over the shape's corner.
        v.show(R.id.b_badge, info.charging)
        v.show(R.id.b_bolt, info.charging)
        if (info.charging) {
            val bs = (d * 0.36f).coerceAtMost(56f)
            val bx = min(x + d - bs * 0.8f, w - bs)
            val by = max(0f, y - bs * 0.15f)
            val badge = cfg.paint(Role.byId(cfg.str("role2", "tertiary")), true)
            v.place(R.id.b_badge, bx, by, bs, bs)
            v.blob(R.id.b_badge, Blob.animated(context, Morphs.charge, badge.fill))
            v.place(R.id.b_bolt, bx + bs * 0.27f, by + bs * 0.27f, bs * 0.46f, bs * 0.46f)
            v.tint(R.id.b_bolt, badge.on)
        }

        v.show(R.id.b_info, wide)
        if (wide) {
            val ix = x + d + 14f
            v.place(R.id.b_info, ix, 0f, w - ix - pad, h)
            v.setTextViewText(
                R.id.b_title,
                when {
                    info.charging && info.level >= 100 -> "Fully charged"
                    info.charging -> "Charging" + (info.watts?.let { " · %.1f W".format(it) } ?: "")
                    else -> "On battery" + (info.watts?.let { " · %.1f W".format(it) } ?: "")
                },
            )
            val line1 = listOfNotNull(
                info.fullInMs?.let { "Full in ${duration(it)}" },
                info.tempC?.let { "%.0f °C".format(it) },
            ).joinToString(" · ")
            val line2 = listOfNotNull(
                info.healthPct?.let { "Health $it%" },
                info.cycles?.let { "$it cycles" },
            ).joinToString(" · ").ifEmpty { if (Shell.granted()) "" else "Health needs Shizuku" }
            v.setTextViewText(R.id.b_l1, line1)
            v.show(R.id.b_l1, line1.isNotEmpty())
            v.setTextViewText(R.id.b_l2, line2)
            v.show(R.id.b_l2, line2.isNotEmpty())
        }

        v.setOnClickPendingIntent(
            android.R.id.background,
            Widgets.activity(context, 9000 + widgetId, Intent(Intent.ACTION_POWER_USAGE_SUMMARY)),
        )
        return v
    }

    private fun duration(ms: Long): String {
        val min = ms / 60_000
        return if (min < 60) "$min min" else "${min / 60} h ${min % 60} min"
    }
}

class BatteryProvider : BaseProvider("battery")
