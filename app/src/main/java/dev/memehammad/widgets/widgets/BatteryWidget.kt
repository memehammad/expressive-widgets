package dev.memehammad.widgets.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.SizeF
import android.widget.RemoteViews
import dev.memehammad.widgets.BaseProvider
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

/**
 * Battery level as a shape filled from the bottom. Wider sizes add charging watts, time to full,
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

    override fun defaults() = Cfg().with("shape", "squircle").with("role", "secondary")

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val info = read(context)
        val small = build(context, widgetId, cfg, info, R.layout.widget_battery)
        val wide = build(context, widgetId, cfg, info, R.layout.widget_battery_wide)
        return RemoteViews(mapOf(SizeF(40f, 40f) to small, SizeF(200f, 80f) to wide))
    }

    private fun build(context: Context, widgetId: Int, cfg: Cfg, info: Info, layout: Int): RemoteViews {
        val v = RemoteViews(context.packageName, layout)
        v.card(cfg)
        // Low battery switches to the error colour so it stands out.
        val low = info.level <= 15 && !info.charging
        val p = cfg.paint(bold = true)
        val track = cfg.paint(bold = false)
        v.setImageViewResource(R.id.b_track, cfg.shape.res)
        v.tint(R.id.b_track, track.fill)
        v.setImageViewResource(R.id.b_fill, cfg.shape.fill)
        v.tint(R.id.b_fill, if (low) android.R.color.holo_red_light else p.fill)
        v.setInt(R.id.b_fill, "setImageLevel", info.level * 100)
        // Text sits on the filled part above ~55%, on the track below.
        val onFill = info.level >= 55
        val textColor = if (onFill) p.on else track.on
        v.setTextViewText(R.id.b_pct, "${info.level}%")
        v.textColor(R.id.b_pct, textColor)
        v.show(R.id.b_bolt, info.charging)
        v.tint(R.id.b_bolt, textColor)

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

        // Tap: refresh now (battery changes aren't broadcast to closed apps).
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
