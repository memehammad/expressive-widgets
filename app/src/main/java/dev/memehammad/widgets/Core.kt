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
    NEUTRAL("neutral", "Neutral", R.color.neutral_container, R.color.neutral_on_container, R.color.neutral_accent, R.color.neutral_on_accent);

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
    val bold get() = str("fill", "tonal") == "bold"
    val card get() = bool("card", false)

    fun paint(role: Role = this.role, bold: Boolean = this.bold) =
        if (bold) Paint(role.accent, role.onAccent) else Paint(role.container, role.onContainer)
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
fun RemoteViews.tint(id: Int, color: Int) = setColorStateList(id, "setImageTintList", color)
fun RemoteViews.textColor(id: Int, color: Int) = setColor(id, "setTextColor", color)
fun RemoteViews.show(id: Int, visible: Boolean) = setViewVisibility(id, if (visible) android.view.View.VISIBLE else android.view.View.GONE)

/** Applies the optional card background behind a widget. */
fun RemoteViews.card(cfg: Cfg) {
    if (cfg.card) setInt(android.R.id.background, "setBackgroundResource", R.drawable.w_bg)
    else setInt(android.R.id.background, "setBackgroundResource", 0)
}
