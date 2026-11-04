package dev.memehammad.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.SizeF
import android.util.TypedValue
import android.widget.RemoteViews
import kotlin.math.min

/**
 * Exact-size drawing. The launcher tells us each size the widget is shown at (portrait and
 * landscape); we lay shapes and text out in dp for each, so text always sits inside its shape.
 */
object Sizes {
    /** Set while drawing the in-app preview, so exactly one layout is built at this size. */
    @Volatile
    var forced: SizeF? = null

    @Suppress("DEPRECATION")
    fun of(context: Context, widgetId: Int): List<SizeF> {
        forced?.let { return listOf(it) }
        val o: Bundle = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId) ?: Bundle()
        val list = o.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            ?.filter { it.width > 0 && it.height > 0 }?.distinct()
        if (!list.isNullOrEmpty()) return list.take(6)
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return listOf(if (w > 0 && h > 0) SizeF(w.toFloat(), h.toFloat()) else SizeF(110f, 110f))
    }

    /** One layout per reported size; the launcher picks the one that fits. */
    fun sized(context: Context, widgetId: Int, build: (w: Float, h: Float) -> RemoteViews): RemoteViews {
        val sizes = of(context, widgetId)
        return if (sizes.size == 1) build(sizes[0].width, sizes[0].height)
        else RemoteViews(sizes.associateWith { build(it.width, it.height) })
    }
}

private const val DP = TypedValue.COMPLEX_UNIT_DIP

/** Sets a view's size and top/left offset (in a FrameLayout child with gravity top|start). */
fun RemoteViews.place(id: Int, x: Float, y: Float, w: Float, h: Float) {
    setViewLayoutWidth(id, w, DP)
    setViewLayoutHeight(id, h, DP)
    setViewLayoutMargin(id, RemoteViews.MARGIN_LEFT, x, DP)
    setViewLayoutMargin(id, RemoteViews.MARGIN_TOP, y, DP)
}

fun RemoteViews.size(id: Int, w: Float, h: Float) {
    setViewLayoutWidth(id, w, DP)
    setViewLayoutHeight(id, h, DP)
}

/** Centres a one-line text of [size] dp on (cx, cy), [w] wide. */
fun RemoteViews.textAt(id: Int, cx: Float, cy: Float, w: Float, size: Float) {
    place(id, cx - w / 2, cy - size * 0.75f, w, size * 1.5f)
    setTextViewTextSize(id, DP, size)
}

/** Text size (dp) for [chars] heavy digits on one line inside a square of [side] dp. */
fun fitDigits(side: Float, chars: Float, lines: Int = 1): Float =
    min(side / (chars * 0.62f), side / (lines * 0.78f + (lines - 1) * 0.16f))

/**
 * A shape drawn into a container (FrameLayout). Static shapes are tinted ImageViews; animated
 * ones are generated ProgressBar layouts, the only thing widgets animate.
 */
object Blob {
    fun static(c: Context, shape: Shape, color: Int): RemoteViews =
        RemoteViews(c.packageName, R.layout.blob).apply {
            setImageViewResource(R.id.blob, shape.res)
            colorList(R.id.blob, "setImageTintList", color)
        }

    /** A generated animated layout (spin, liquid, morph) tinted [color]. */
    fun animated(c: Context, layout: Int, color: Int): RemoteViews =
        RemoteViews(c.packageName, layout).apply {
            colorList(R.id.blob, "setIndeterminateTintList", color)
        }

    fun of(c: Context, shape: Shape, color: Int, spin: Boolean) =
        if (spin) animated(c, shape.spin, color) else static(c, shape, color)
}

/**
 * Puts [child] into [container] with a stable id: when the widget updates, the launcher reuses
 * the existing view instead of inflating a new one, so running animations don't restart.
 * (removeAllViews keeps stable children for this; anything else in the container is dropped.)
 */
fun RemoteViews.blob(container: Int, child: RemoteViews) {
    removeAllViews(container)
    addStableView(container, child, 1)
}

/** Where text goes inside a shape of [side] dp placed at (x, y): centre and side of the square. */
data class Inner(val cx: Float, val cy: Float, val side: Float)

fun Shape.inner(x: Float, y: Float, side: Float, spinning: Boolean): Inner {
    val b = if (spinning) spinBox else box
    return Inner(x + b.cx * side, y + b.cy * side, b.half * 2 * side)
}

/**
 * Keeps text off the liquid's wavy surface (where it would be half on each colour): returns the
 * text's centre y and whether it sits on the liquid. [half] is half the text block's height.
 */
fun floatOnLiquid(y: Float, d: Float, pct: Int, cy: Float, half: Float): Pair<Float, Boolean> {
    val line = y + d * (109f - 118f * pct / 100f) / 100f
    val m = d * 0.06f
    return when {
        cy - half > line + m -> cy to true
        cy + half < line - m -> cy to false
        line - m - 2 * half >= y + d * 0.1f -> (line - m - half) to false
        else -> kotlin.math.min(line + m + half, y + d * 0.9f - half) to true
    }
}
