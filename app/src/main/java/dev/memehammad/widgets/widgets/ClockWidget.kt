package dev.memehammad.widgets.widgets

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.provider.AlarmClock
import android.widget.RemoteViews
import dev.memehammad.widgets.BaseProvider
import dev.memehammad.widgets.Blob
import dev.memehammad.widgets.Cfg
import dev.memehammad.widgets.Kind
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Role
import dev.memehammad.widgets.Shapes
import dev.memehammad.widgets.Sizes
import dev.memehammad.widgets.Widgets
import dev.memehammad.widgets.blob
import dev.memehammad.widgets.card
import dev.memehammad.widgets.fitDigits
import dev.memehammad.widgets.inner
import dev.memehammad.widgets.place
import dev.memehammad.widgets.show
import dev.memehammad.widgets.size
import dev.memehammad.widgets.textAt
import dev.memehammad.widgets.textColor
import dev.memehammad.widgets.tint
import kotlin.math.max
import kotlin.math.min

/**
 * Clock in four styles:
 *  duo     hours and minutes in two overlapping shapes of different colours
 *  stacked hours over minutes in one shape
 *  inline  h:mm in one shape
 *  analog  a shape as the dial with rounded hands and an orbiting seconds dot
 * Everything is sized for the exact widget size, so the time never leaves its shape.
 */
object ClockWidget : Kind(
    "clock", "Shape clock", "Hours and minutes in playful shapes, or an analog clock with a shape as the dial.",
    R.drawable.ic_clock, ClockProvider::class.java,
) {
    val styles = listOf("duo" to "Duo", "stacked" to "Stacked", "inline" to "Inline", "analog" to "Analog")

    override fun defaults() = Cfg().with("style", "duo").with("shape", "cookie9").with("shape2", "clover4")
        .with("role", "primary").with("role2", "tertiary").with("animate", true).with("date", true)

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews =
        Sizes.sized(context, widgetId) { w, h -> build(context, widgetId, cfg, w, h) }

    private fun build(c: Context, widgetId: Int, cfg: Cfg, w: Float, h: Float): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_clock)
        v.card(cfg)
        v.size(R.id.c_stage, w, h)
        val style = cfg.str("style", cfg.str("layout", "duo"))
        val pad = if (cfg.card) 10f else 4f
        val spin = cfg.bool("animate", true)
        val p = cfg.paint()
        val role2 = Role.byId(cfg.str("role2", "tertiary"))
        val p2 = cfg.paint(role2, !cfg.bold)
        val chipPaint = cfg.paint(role2, true)
        val date = cfg.bool("date", true)
        val shape = cfg.shape
        val shape2 = Shapes.byId(cfg.str("shape2", "clover4"))

        // Reset everything (the launcher re-applies old actions on top of the layout).
        v.show(R.id.c_b0, style != "analog")
        v.show(R.id.c_b1, style == "duo")
        v.show(R.id.c_analog, style == "analog")
        v.show(R.id.c_t0, style != "analog")
        v.show(R.id.c_t1, style == "duo" || style == "stacked")
        v.show(R.id.c_chip, date)

        val m0 = min(w, h) - 2 * pad
        val ch = (m0 * 0.15f).coerceIn(18f, 30f)
        val fs = ch * 0.5f
        val longDate = style != "duo"
        val cw = (if (longDate) 10 else 6) * 0.58f * fs + ch

        fun chip(x: Float, y: Float) {
            val cx = x.coerceIn(pad, max(pad, w - pad - cw))
            val cy = y.coerceIn(pad, max(pad, h - pad - ch))
            v.place(R.id.c_chip, cx, cy, cw, ch)
            v.tint(R.id.c_chip_bg, chipPaint.fill)
            v.textColor(R.id.c_date, chipPaint.on)
            v.setTextViewTextSize(R.id.c_date, android.util.TypedValue.COMPLEX_UNIT_DIP, fs)
            val f = if (longDate) "EEE d MMM" else "EEE d"
            v.setCharSequence(R.id.c_date, "setFormat12Hour", f)
            v.setCharSequence(R.id.c_date, "setFormat24Hour", f)
        }

        when (style) {
            "duo" -> {
                val wide = w > h * 1.45f
                val s: Float
                val x0: Float
                val y0: Float
                val x1: Float
                val y1: Float
                if (wide) {
                    s = min(h - 2 * pad, (w - 2 * pad) / 1.72f)
                    val left = (w - 1.72f * s) / 2
                    val top = (h - s) / 2
                    x0 = left; y0 = top - 0.04f * s
                    x1 = left + 0.72f * s; y1 = top + 0.04f * s
                    if (date) chip(x1 + s - cw, max(pad, y1 - ch * 0.2f))
                } else {
                    val m = m0
                    s = 0.6f * m
                    val left = (w - m) / 2
                    val top = (h - m) / 2
                    x0 = left; y0 = top
                    x1 = left + m - s; y1 = top + m - s
                    if (date) chip(left, top + m - ch)
                }
                v.place(R.id.c_b0, x0, y0, s, s)
                v.place(R.id.c_b1, x1, y1, s, s)
                v.blob(R.id.c_b0, Blob.of(c, shape, p.fill, spin))
                v.blob(R.id.c_b1, Blob.of(c, shape2, p2.fill, spin))
                val i0 = shape.inner(x0, y0, s, spin)
                val i1 = shape2.inner(x1, y1, s, spin)
                val ts = min(fitDigits(i0.side, 2f), fitDigits(i1.side, 2f))
                v.textAt(R.id.c_t0, i0.cx, i0.cy, s, ts)
                v.textAt(R.id.c_t1, i1.cx, i1.cy, s, ts)
                v.setCharSequence(R.id.c_t0, "setFormat12Hour", "hh")
                v.setCharSequence(R.id.c_t0, "setFormat24Hour", "HH")
                v.setCharSequence(R.id.c_t1, "setFormat12Hour", "mm")
                v.setCharSequence(R.id.c_t1, "setFormat24Hour", "mm")
                v.textColor(R.id.c_t0, p.on)
                v.textColor(R.id.c_t1, p2.on)
            }

            "analog" -> {
                val d = min(w - 2 * pad, h - 2 * pad - if (date) ch * 0.5f else 0f)
                val x = (w - d) / 2
                val y = (h - d - if (date) ch * 0.5f else 0f) / 2
                v.place(R.id.c_analog, x, y, d, d)
                v.setIcon(R.id.c_analog, "setDial", Icon.createWithResource(c, shape.res))
                v.setColorStateList(R.id.c_analog, "setDialTintList", p.fill)
                v.setColorStateList(R.id.c_analog, "setHourHandTintList", p.on)
                v.setColorStateList(R.id.c_analog, "setMinuteHandTintList", chipPaint.fill)
                v.setColorStateList(
                    R.id.c_analog, "setSecondHandTintList",
                    if (cfg.bool("seconds", true)) chipPaint.fill else android.R.color.transparent,
                )
                if (date) chip((w - cw) / 2, y + d - ch * 0.6f)
            }

            else -> { // stacked or inline
                val d = min(w - 2 * pad, h - 2 * pad - if (date) ch * 0.5f else 0f)
                val x = (w - d) / 2
                val y = (h - d - if (date) ch * 0.5f else 0f) / 2
                v.place(R.id.c_b0, x, y, d, d)
                v.blob(R.id.c_b0, Blob.of(c, shape, p.fill, spin))
                val i = shape.inner(x, y, d, spin)
                v.textColor(R.id.c_t0, p.on)
                if (style == "stacked") {
                    val ts = fitDigits(i.side, 2f, 2)
                    v.textAt(R.id.c_t0, i.cx, i.cy - 0.48f * ts, d, ts)
                    v.textAt(R.id.c_t1, i.cx, i.cy + 0.48f * ts, d, ts)
                    v.setCharSequence(R.id.c_t0, "setFormat12Hour", "hh")
                    v.setCharSequence(R.id.c_t0, "setFormat24Hour", "HH")
                    v.setCharSequence(R.id.c_t1, "setFormat12Hour", "mm")
                    v.setCharSequence(R.id.c_t1, "setFormat24Hour", "mm")
                    v.textColor(R.id.c_t1, p.on)
                } else {
                    val ts = fitDigits(i.side, 4.4f)
                    v.textAt(R.id.c_t0, i.cx, i.cy, d, ts)
                    v.setCharSequence(R.id.c_t0, "setFormat12Hour", "h:mm")
                    v.setCharSequence(R.id.c_t0, "setFormat24Hour", "HH:mm")
                }
                if (date) chip((w - cw) / 2, y + d - ch * 0.6f)
            }
        }

        v.setOnClickPendingIntent(
            android.R.id.background,
            Widgets.activity(c, widgetId, Intent(AlarmClock.ACTION_SHOW_ALARMS)),
        )
        return v
    }
}

class ClockProvider : BaseProvider("clock")
