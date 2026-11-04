package dev.memehammad.widgets.widgets

import android.app.PendingIntent
import android.content.Context
import android.widget.RemoteViews
import dev.memehammad.widgets.Blob
import dev.memehammad.widgets.Cfg
import dev.memehammad.widgets.Paint
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Shape
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
import kotlin.math.max
import kotlin.math.min

/** What a "shape with a number" widget shows. */
class BadgeSpec(
    val shape: Shape,
    val paint: Paint,
    val spin: Boolean = false,
    /** A generated animated layout (morph) drawn instead of the shape. */
    val animated: Int? = null,
    /** Liquid fill level (0..100); the shape becomes a track in [track] colours. */
    val liquid: Int? = null,
    val track: Paint? = null,
    val icon: Int? = null,
    val iconTint: Boolean = true,
    val num: String? = null,
    /** How many digit widths [num] needs (for sizing). */
    val numChars: Float = 3f,
    /** Count-down chronometer ending at this elapsedRealtime, shown instead of [num]. */
    val chronoEnd: Long? = null,
    val sub: String? = null,
    val title: String? = null,
    val chips: List<String> = emptyList(),
    /** Morph layout + icon for a little badge on the shape's corner. */
    val badge: Pair<Int, Int>? = null,
    val badgePaint: Paint? = null,
    val tap: PendingIntent? = null,
)

/** Lays a [BadgeSpec] out at an exact size: shape centred (or on the left with details when wide). */
object Badge {
    fun build(c: Context, cfg: Cfg, w: Float, h: Float, s: BadgeSpec): RemoteViews {
        val v = RemoteViews(c.packageName, R.layout.widget_badge)
        v.card(c, cfg)
        v.size(R.id.g_stage, w, h)
        val pad = if (cfg.card) 10f else 4f
        val wide = w >= 180f && w > h * 1.5f
        val d = if (wide) h - 2 * pad else min(w, h) - 2 * pad
        val x = if (wide) pad + 2f else (w - d) / 2
        val y = (h - d) / 2

        // Shape (or liquid in a track, or a morph).
        v.show(R.id.g_track, s.liquid != null)
        v.place(R.id.g_blob, x, y, d, d)
        if (s.liquid != null) {
            val track = s.track ?: s.paint
            v.place(R.id.g_track, x, y, d, d)
            v.blob(R.id.g_track, Blob.static(c, s.shape, track.fill))
            v.blob(R.id.g_blob, Blob.animated(c, dev.memehammad.widgets.Liquid.layout(s.shape.id, s.liquid), s.paint.fill))
        } else if (s.animated != null) {
            v.blob(R.id.g_blob, Blob.animated(c, s.animated, s.paint.fill))
        } else {
            v.blob(R.id.g_blob, Blob.of(c, s.shape, s.paint.fill, s.spin))
        }

        val i = s.shape.inner(x, y, d, s.spin || s.animated != null)
        val hasNum = s.num != null || s.chronoEnd != null
        val numId = if (s.chronoEnd != null) R.id.g_chrono else R.id.g_num
        v.show(R.id.g_num, s.num != null && s.chronoEnd == null)
        v.show(R.id.g_chrono, s.chronoEnd != null)
        v.show(R.id.g_icon, s.icon != null)
        v.show(R.id.g_sub, s.sub != null)

        var on = s.paint.on
        when {
            s.icon != null && !hasNum && s.sub != null -> {
                val sz = i.side * 0.74f
                v.place(R.id.g_icon, i.cx - sz / 2, i.cy - i.side * 0.5f, sz, sz)
                v.textAt(R.id.g_sub, i.cx, i.cy + i.side * 0.4f, d, (i.side * 0.15f).coerceAtLeast(8f))
            }
            s.icon != null && !hasNum -> {
                val sz = i.side * 0.95f
                v.place(R.id.g_icon, i.cx - sz / 2, i.cy - sz / 2, sz, sz)
            }
            s.icon != null -> {
                val sz = i.side * (if (s.sub != null) 0.34f else 0.46f)
                v.place(R.id.g_icon, i.cx - sz / 2, i.cy - i.side * 0.48f, sz, sz)
                val ts = min(fitDigits(i.side, s.numChars), i.side * if (s.sub != null) 0.32f else 0.38f)
                val ny = if (s.sub != null) i.cy + i.side * 0.04f else i.cy + i.side * 0.24f
                v.textAt(numId, i.cx, ny, d, ts)
                if (s.sub != null) v.textAt(R.id.g_sub, i.cx, i.cy + i.side * 0.38f, d, (i.side * 0.13f).coerceAtLeast(8f))
            }
            else -> {
                val ts = min(fitDigits(i.side, s.numChars), d * 0.3f)
                var cy = i.cy + if (s.sub != null) ts * 0.16f else 0f
                if (s.liquid != null) {
                    val (ny, onLiquid) = floatOnLiquid(y, d, s.liquid, cy, if (s.sub != null) ts * 0.8f else ts * 0.45f)
                    cy = ny
                    on = if (onLiquid) s.paint.on else (s.track ?: s.paint).on
                }
                if (s.sub != null) {
                    v.textAt(numId, i.cx, cy - ts * 0.46f, d, ts)
                    v.textAt(R.id.g_sub, i.cx, cy + ts * 0.46f, d, (ts * 0.4f).coerceAtLeast(8f))
                } else if (hasNum) {
                    v.textAt(numId, i.cx, cy, d, ts)
                }
            }
        }
        s.icon?.let {
            v.setImageViewResource(R.id.g_icon, it)
            if (s.iconTint) v.tint(R.id.g_icon, on) else v.setInt(R.id.g_icon, "setColorFilter", 0)
        }
        s.num?.let { v.setTextViewText(R.id.g_num, it) }
        s.sub?.let { v.setTextViewText(R.id.g_sub, it) }
        v.textColor(R.id.g_num, on)
        v.textColor(R.id.g_chrono, on)
        v.textColor(R.id.g_sub, on)
        if (s.chronoEnd != null) {
            v.setChronometer(R.id.g_chrono, s.chronoEnd, null, true)
            v.setChronometerCountDown(R.id.g_chrono, true)
        } else {
            v.setChronometer(R.id.g_chrono, 0L, null, false)
        }

        // Corner badge.
        val b = s.badge
        v.show(R.id.g_badge, b != null)
        v.show(R.id.g_badge_icon, b != null)
        if (b != null) {
            val bp = s.badgePaint ?: s.paint
            val bs = (d * 0.36f).coerceAtMost(54f)
            val bx = min(x + d - bs * 0.8f, w - bs)
            val by = max(0f, y - bs * 0.15f)
            v.place(R.id.g_badge, bx, by, bs, bs)
            v.blob(R.id.g_badge, Blob.animated(c, b.first, bp.fill))
            v.place(R.id.g_badge_icon, bx + bs * 0.27f, by + bs * 0.27f, bs * 0.46f, bs * 0.46f)
            v.setImageViewResource(R.id.g_badge_icon, b.second)
            v.tint(R.id.g_badge_icon, bp.on)
        }

        // Details next to the shape when wide.
        v.show(R.id.g_info, wide)
        if (wide) {
            val ix = x + d + 14f
            v.place(R.id.g_info, ix, 0f, w - ix - pad, h)
            v.setTextViewText(R.id.g_title, s.title ?: "")
            v.textColor(R.id.g_title, if (cfg.card) R.color.w_text else R.color.on_wall)
            listOf(R.id.g_c0, R.id.g_c1, R.id.g_c2).forEachIndexed { k, id ->
                v.show(id, k < s.chips.size)
                if (k < s.chips.size) v.setTextViewText(id, s.chips[k])
            }
        }
        s.tap?.let { v.setOnClickPendingIntent(android.R.id.background, it) }
        return v
    }
}
