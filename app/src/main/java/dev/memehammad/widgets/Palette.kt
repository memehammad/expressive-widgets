package dev.memehammad.widgets

import android.content.Context
import android.content.res.ColorStateList

/**
 * The system's wallpaper palette (Material You tones). Card colours are picked as one tone for
 * light mode and one for dark mode, so a card can match exactly what the launcher uses.
 */
object Palette {
    val families = listOf("a1" to "Primary", "a2" to "Secondary", "a3" to "Tertiary", "n1" to "Neutral", "n2" to "Neutral variant")
    val tones = listOf(0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000)

    private val res = mapOf(
        "a1" to intArrayOf(android.R.color.system_accent1_0, android.R.color.system_accent1_10, android.R.color.system_accent1_50, android.R.color.system_accent1_100, android.R.color.system_accent1_200, android.R.color.system_accent1_300, android.R.color.system_accent1_400, android.R.color.system_accent1_500, android.R.color.system_accent1_600, android.R.color.system_accent1_700, android.R.color.system_accent1_800, android.R.color.system_accent1_900, android.R.color.system_accent1_1000),
        "a2" to intArrayOf(android.R.color.system_accent2_0, android.R.color.system_accent2_10, android.R.color.system_accent2_50, android.R.color.system_accent2_100, android.R.color.system_accent2_200, android.R.color.system_accent2_300, android.R.color.system_accent2_400, android.R.color.system_accent2_500, android.R.color.system_accent2_600, android.R.color.system_accent2_700, android.R.color.system_accent2_800, android.R.color.system_accent2_900, android.R.color.system_accent2_1000),
        "a3" to intArrayOf(android.R.color.system_accent3_0, android.R.color.system_accent3_10, android.R.color.system_accent3_50, android.R.color.system_accent3_100, android.R.color.system_accent3_200, android.R.color.system_accent3_300, android.R.color.system_accent3_400, android.R.color.system_accent3_500, android.R.color.system_accent3_600, android.R.color.system_accent3_700, android.R.color.system_accent3_800, android.R.color.system_accent3_900, android.R.color.system_accent3_1000),
        "n1" to intArrayOf(android.R.color.system_neutral1_0, android.R.color.system_neutral1_10, android.R.color.system_neutral1_50, android.R.color.system_neutral1_100, android.R.color.system_neutral1_200, android.R.color.system_neutral1_300, android.R.color.system_neutral1_400, android.R.color.system_neutral1_500, android.R.color.system_neutral1_600, android.R.color.system_neutral1_700, android.R.color.system_neutral1_800, android.R.color.system_neutral1_900, android.R.color.system_neutral1_1000),
        "n2" to intArrayOf(android.R.color.system_neutral2_0, android.R.color.system_neutral2_10, android.R.color.system_neutral2_50, android.R.color.system_neutral2_100, android.R.color.system_neutral2_200, android.R.color.system_neutral2_300, android.R.color.system_neutral2_400, android.R.color.system_neutral2_500, android.R.color.system_neutral2_600, android.R.color.system_neutral2_700, android.R.color.system_neutral2_800, android.R.color.system_neutral2_900, android.R.color.system_neutral2_1000),
    )

    /** "a1:100" -> colour resource. */
    fun res(spec: String): Int {
        val (f, t) = spec.split(":").let { it[0] to (it.getOrNull(1)?.toIntOrNull() ?: 100) }
        val arr = res[f] ?: res.getValue("a1")
        return arr[tones.indexOf(t).takeIf { it >= 0 } ?: 3]
    }

    fun color(c: Context, spec: String): ColorStateList = c.getColorStateList(res(spec))

    /** Colour codes for the Home screen role (resolved per light/dark mode). */
    const val ICON_BACK = -1
    const val ICON_SYMBOL = -2

    /** App context, so colour codes can be resolved while drawing widgets. */
    @Volatile
    var app: Context? = null

    fun special(code: Int): Pair<ColorStateList, ColorStateList> {
        val c = app ?: error("Palette.app not set")
        val (day, night) = when {
            code == ICON_SYMBOL -> symbol(c)
            code == ICON_BACK -> home(c)
            else -> decode(code)
        }
        return color(c, day) to color(c, night)
    }

    private fun index(spec: String): Int {
        val (f, t) = spec.split(":").let { it[0] to (it.getOrNull(1)?.toIntOrNull() ?: 100) }
        val fi = families.indexOfFirst { it.first == f }.coerceAtLeast(0)
        val ti = tones.indexOf(t).coerceAtLeast(0)
        return fi * tones.size + ti
    }

    private fun spec(index: Int) = "${families[index / tones.size].first}:${tones[index % tones.size]}"

    /** A light/dark tone pair packed into a negative colour code (see colorList). */
    fun code(day: String, night: String): Int = -(1_000_000 + index(day) * 1000 + index(night))

    private fun decode(code: Int): Pair<String, String> {
        val n = -code - 1_000_000
        return spec(n / 1000) to spec(n % 1000)
    }

    /** A readable text tone on top of [spec]: very dark on light tones, very light on dark ones. */
    fun onFor(spec: String): String {
        val (f, t) = spec.split(":").let { it[0] to (it.getOrNull(1)?.toIntOrNull() ?: 100) }
        return if (t <= 400) "$f:900" else "$f:10"
    }

    /** Symbol (foreground) tones of themed icons: primary 700 in light mode, primary 100 in dark. */
    fun symbol(c: Context): Pair<String, String> =
        (prefs(c).getString("slight", null) ?: "a1:700") to (prefs(c).getString("sdark", null) ?: "a1:100")

    fun setSymbol(c: Context, light: String? = null, dark: String? = null) {
        prefs(c).edit().apply {
            if (light != null) putString("slight", light)
            if (dark != null) putString("sdark", dark)
        }.apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("palette", Context.MODE_PRIVATE)

    /**
     * The "Home screen" card colour shared by all widgets. Default: what Pixel's launcher uses
     * behind themed icons (primary 100 in light mode, neutral 800 in dark mode).
     */
    fun home(c: Context): Pair<String, String> =
        (prefs(c).getString("light", null) ?: "a1:100") to (prefs(c).getString("dark", null) ?: "n1:800")

    fun setHome(c: Context, light: String? = null, dark: String? = null) {
        prefs(c).edit().apply {
            if (light != null) putString("light", light)
            if (dark != null) putString("dark", dark)
        }.apply()
    }

    /** Light and dark tones for a card colour choice. */
    fun card(c: Context, choice: String): Pair<String, String> = when (choice) {
        "surface" -> "n1:50" to "n1:900"
        "primary" -> "a1:50" to "a1:800"
        "secondary" -> "a2:50" to "a2:800"
        "tertiary" -> "a3:50" to "a3:800"
        else -> home(c)
    }
}
