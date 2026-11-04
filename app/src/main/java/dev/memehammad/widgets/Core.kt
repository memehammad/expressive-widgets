package dev.memehammad.widgets

import android.content.Context
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject

/** Material You colour role (from the wallpaper). */
enum class Role(val id: String, val label: String, val container: Int, val onContainer: Int, val accent: Int, val onAccent: Int) {
    PRIMARY("primary", "Primary", R.color.primary_container, R.color.primary_on_container, R.color.primary_accent, R.color.primary_on_accent),
    SECONDARY("secondary", "Secondary", R.color.secondary_container, R.color.secondary_on_container, R.color.secondary_accent, R.color.secondary_on_accent),
    TERTIARY("tertiary", "Tertiary", R.color.tertiary_container, R.color.tertiary_on_container, R.color.tertiary_accent, R.color.tertiary_on_accent),
    NEUTRAL("neutral", "Neutral", R.color.neutral_container, R.color.neutral_on_container, R.color.neutral_accent, R.color.neutral_on_accent),

    /** The tones behind and on your themed app icons (picked in the app); bold swaps them. */
    ICONS("icons", "Home screen", Palette.ICON_BACK, Palette.ICON_SYMBOL, Palette.ICON_SYMBOL, Palette.ICON_BACK),

    /** Any wallpaper tone, picked per widget (text colour is chosen for contrast). */
    CUSTOM("custom", "Custom", 0, 0, 0, 0);

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id } ?: PRIMARY
    }
}

/** Colours for a shape and what's drawn on it. */
data class Paint(val fill: Int, val on: Int)

/** Per-widget settings, stored as JSON keyed by widget id. */
class Cfg(val json: JSONObject = JSONObject()) {
    fun str(k: String, def: String) = json.optString(k, def)
    fun int(k: String, def: Int) = json.optInt(k, def)
    fun bool(k: String, def: Boolean) = json.optBoolean(k, def)
    fun list(k: String, def: List<String>): List<String> = json.optJSONArray(k)?.let { a ->
        (0 until a.length()).map { a.getString(it) }
    } ?: def

    fun with(k: String, v: Any): Cfg = Cfg(JSONObject(json.toString()).put(k, if (v is List<*>) JSONArray(v) else v))

    // Common style options.
    val shape get() = Shapes.byId(str("shape", "cookie9"))
    val role get() = Role.byId(str("role", "primary"))
    /** "tonal" = soft container colour, "bold" = strong accent colour. */
    val bold get() = str("fill", "tonal") == "bold" || clashes(role) || paleOnCard(role)
    val card get() = bool("card", false)
    /** Card colour: "icons" matches themed app icons; "surface" neutral; or a colour role. */
    val cardColor get() = str("cardColor", "icons")

    /** A soft shape would vanish on a card of the same colour, so it goes bold there. */
    private fun clashes(r: Role) = card && cardColor == r.id

    /** A soft shape that would barely show on the card (in light or dark mode) goes bold instead. */
    private fun paleOnCard(r: Role): Boolean {
        if (!card) return false
        val c = Palette.app ?: return false
        return runCatching {
            val (cd, cn) = Palette.card(c, cardColor).let { Palette.color(c, it.first).defaultColor to Palette.color(c, it.second).defaultColor }
            val (fd, fn) = Colors.pair(paint(r, false).fill).let { it.first.defaultColor to it.second.defaultColor }
            contrast(cd, fd) < 1.35 || contrast(cn, fn) < 1.35
        }.getOrDefault(false)
    }

    fun paint(role: Role = this.role, bold: Boolean = this.bold) =
        if (role == Role.CUSTOM) {
            val (day, night) = customFill
            val fill = Palette.code(day, night)
            val on = Palette.code(Palette.onFor(day), Palette.onFor(night))
            if (bold) Paint(on, fill) else Paint(fill, on)
        } else if (bold) Paint(role.accent, role.onAccent) else Paint(role.container, role.onContainer)

    /** Custom shape colour: one tone for light mode, one for dark mode. */
    val customFill get() = str("cfill_l", "a1:300") to str("cfill_d", "a1:400")
}

val cardColors = listOf(
    "icons" to "Home screen", "surface" to "Neutral",
    "primary" to "Primary", "secondary" to "Secondary", "tertiary" to "Tertiary",
)

/** WCAG contrast ratio between two colours. */
fun contrast(a: Int, b: Int): Double {
    fun lum(c: Int): Double {
        fun ch(v: Int): Double = (v / 255.0).let { if (it <= 0.03928) it / 12.92 else Math.pow((it + 0.055) / 1.055, 2.4) }
        return 0.2126 * ch((c shr 16) and 255) + 0.7152 * ch((c shr 8) and 255) + 0.0722 * ch(c and 255)
    }
    val x = lum(a)
    val y = lum(b)
    return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
}

object Configs {
    private fun prefs(c: Context) = c.getSharedPreferences("widgets", Context.MODE_PRIVATE)

    fun get(c: Context, id: Int): Cfg =
        prefs(c).getString("w_$id", null)?.let { runCatching { Cfg(JSONObject(it)) }.getOrNull() } ?: Cfg()

    fun save(c: Context, id: Int, cfg: Cfg) {
        prefs(c).edit().putString("w_$id", cfg.json.toString()).apply()
    }

    fun delete(c: Context, id: Int) {
        prefs(c).edit().remove("w_$id").apply()
    }
}

/** RemoteViews helpers (dynamic colour resources, so widgets follow wallpaper changes). */
/**
 * Colours are resolved here, in the app, to fixed light- and dark-mode values. Passing colour
 * resources would let the launcher swap them for colours taken from the wallpaper behind the
 * widget (Pixel Launcher does this), so widgets wouldn't show the colours picked in settings.
 */
object Colors {
    private fun modeContext(c: android.content.Context, night: Boolean): android.content.Context {
        val cfg = android.content.res.Configuration(c.resources.configuration)
        cfg.uiMode = (cfg.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (night) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO
        return c.createConfigurationContext(cfg)
    }

    /** Light- and dark-mode colours for a colour resource or a Palette code. */
    fun pair(code: Int): Pair<android.content.res.ColorStateList, android.content.res.ColorStateList> {
        if (code < 0) return Palette.special(code)
        val c = Palette.app ?: error("Palette.app not set")
        return modeContext(c, false).getColorStateList(code) to modeContext(c, true).getColorStateList(code)
    }

    fun int(code: Int, night: Boolean): Int = pair(code).let { if (night) it.second else it.first }.defaultColor
}

fun RemoteViews.colorList(id: Int, method: String, color: Int) {
    val (day, night) = Colors.pair(color)
    setColorStateList(id, method, day, night)
}

fun RemoteViews.tint(id: Int, color: Int) = colorList(id, "setImageTintList", color)

fun RemoteViews.textColor(id: Int, color: Int) {
    val (day, night) = Colors.pair(color)
    setColorInt(id, "setTextColor", day.defaultColor, night.defaultColor)
}

fun RemoteViews.show(id: Int, visible: Boolean) = setViewVisibility(id, if (visible) android.view.View.VISIBLE else android.view.View.GONE)

/** Text drawn straight on a widget (not inside a shape): ids per widget layout. */
private val plainText = intArrayOf(
    R.id.a_weekday, R.id.b_title, R.id.hv0, R.id.hv1, R.id.hv2, R.id.hv3, R.id.hv4, R.id.hv5, R.id.s_title,
    R.id.tl0, R.id.tl1, R.id.tl2, R.id.tl3, R.id.wc0, R.id.wc1, R.id.wc2, R.id.wt0, R.id.wt1, R.id.wt2,
    R.id.wx_big_cond, R.id.wx_place, R.id.wx_rain, R.id.x_title,
)
private val plainText2 = intArrayOf(
    R.id.a_empty, R.id.a_month, R.id.ht0, R.id.ht1, R.id.ht2, R.id.ht3, R.id.ht4, R.id.ht5,
    R.id.s_day0, R.id.s_day1, R.id.s_day2, R.id.s_day3, R.id.s_day4, R.id.s_day5, R.id.s_day6,
    R.id.wd0, R.id.wd1, R.id.wd2, R.id.wx_cond, R.id.wx_range,
)

/**
 * Shows the optional card behind a widget, in its light- and dark-mode colours. Without a card,
 * plain text turns white with a soft shadow (like icon labels) so it reads on any wallpaper.
 */
fun RemoteViews.card(c: android.content.Context, cfg: Cfg) {
    setInt(android.R.id.background, "setBackgroundResource", 0)
    plainText.forEach { textColor(it, if (cfg.card) R.color.w_text else R.color.on_wall) }
    plainText2.forEach { textColor(it, if (cfg.card) R.color.w_text2 else R.color.on_wall2) }
    show(R.id.w_card, cfg.card)
    if (cfg.card) {
        val (day, night) = Palette.card(c, cfg.cardColor)
        setColorStateList(R.id.w_card, "setImageTintList", Palette.color(c, day), Palette.color(c, night))
    }
}
