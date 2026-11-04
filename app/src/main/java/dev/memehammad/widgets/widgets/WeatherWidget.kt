package dev.memehammad.widgets.widgets

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.memehammad.widgets.BaseProvider
import dev.memehammad.widgets.Blob
import dev.memehammad.widgets.Morphs
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
import dev.memehammad.widgets.Shapes
import dev.memehammad.widgets.Widgets
import dev.memehammad.widgets.card
import dev.memehammad.widgets.show
import dev.memehammad.widgets.textColor
import dev.memehammad.widgets.tint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/** Weather from Open-Meteo (free, no account or key). Location and units are set in the app. */
object Weather {
    data class Place(val name: String, val lat: Double, val lon: Double)

    data class Hour(val timeMs: Long, val temp: Double, val code: Int, val rainPct: Int)

    data class Now(
        val temp: Double, val code: Int, val day: Boolean,
        val high: Double, val low: Double, val hours: List<Hour>, val fetchedAt: Long,
    )

    private fun prefs(c: Context) = c.getSharedPreferences("weather", Context.MODE_PRIVATE)

    fun place(c: Context): Place? {
        val p = prefs(c)
        val name = p.getString("place", null) ?: return null
        return Place(name, p.getFloat("lat", 0f).toDouble(), p.getFloat("lon", 0f).toDouble())
    }

    fun setPlace(c: Context, place: Place) {
        prefs(c).edit().putString("place", place.name).putFloat("lat", place.lat.toFloat())
            .putFloat("lon", place.lon.toFloat()).remove("data").apply()
    }

    /** Follow the phone's location instead of a fixed city. */
    fun auto(c: Context) = prefs(c).getBoolean("auto", false)

    fun setAuto(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("auto", on).apply()
    }

    fun locationAllowed(c: Context) =
        c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun backgroundAllowed(c: Context) =
        c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Rough current position (recent last fix, else a fresh one) and a town name for it. */
    @SuppressLint("MissingPermission")
    suspend fun locate(c: Context): Place? {
        if (!locationAllowed(c)) return null
        val lm = c.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        val loc = if (last != null && System.currentTimeMillis() - last.time < 30 * 60_000L) last
        else fresh(c, lm, providers.firstOrNull()) ?: last ?: return null
        val name = withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(c).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
                    ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
            }.getOrNull()
        } ?: "Current location"
        return Place(name, loc.latitude, loc.longitude)
    }

    @SuppressLint("MissingPermission")
    private suspend fun fresh(c: Context, lm: LocationManager, provider: String?): Location? {
        provider ?: return null
        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                try {
                    lm.getCurrentLocation(provider, signal, c.mainExecutor) { cont.resume(it) }
                } catch (e: Exception) {
                    cont.resume(null)
                }
            }
        }
    }

    /** In location mode, moves the saved place along with the phone. */
    suspend fun updateLocation(c: Context): Boolean {
        if (!auto(c)) return false
        val here = locate(c) ?: return false
        val old = place(c)
        val moved = old == null || kotlin.math.abs(old.lat - here.lat) > 0.03 || kotlin.math.abs(old.lon - here.lon) > 0.03
        if (moved) setPlace(c, here)
        return moved
    }

    fun fahrenheit(c: Context) = prefs(c).getBoolean("fahrenheit", false)

    fun setFahrenheit(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("fahrenheit", on).remove("data").apply()
    }

    private fun get(url: String): String? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "ExpressiveWidgets")
        conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    } catch (_: Exception) {
        null
    }

    /** City search for the settings screen. */
    suspend fun search(query: String): List<Place> = withContext(Dispatchers.IO) {
        val body = get(
            "https://geocoding-api.open-meteo.com/v1/search?count=8&language=en&name=" +
                URLEncoder.encode(query, "UTF-8"),
        ) ?: return@withContext emptyList()
        val arr = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val extra = listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", ")
            Place(o.getString("name") + if (extra.isNotEmpty()) ", $extra" else "", o.getDouble("latitude"), o.getDouble("longitude"))
        }
    }

    /** Fetches the forecast if the cached one is older than [maxAgeMs]. */
    suspend fun refresh(c: Context, maxAgeMs: Long = 30 * 60_000L) {
        updateLocation(c)
        val place = place(c) ?: return
        cached(c)?.let { if (System.currentTimeMillis() - it.fetchedAt < maxAgeMs) return }
        val unit = if (fahrenheit(c)) "fahrenheit" else "celsius"
        val body = withContext(Dispatchers.IO) {
            get(
                "https://api.open-meteo.com/v1/forecast?latitude=${place.lat}&longitude=${place.lon}" +
                    "&current=temperature_2m,weather_code,is_day" +
                    "&hourly=temperature_2m,weather_code,precipitation_probability" +
                    "&daily=temperature_2m_max,temperature_2m_min" +
                    "&timezone=auto&timeformat=unixtime&forecast_days=2&temperature_unit=$unit",
            )
        } ?: return
        prefs(c).edit().putString("data", body).putLong("fetched", System.currentTimeMillis()).apply()
    }

    fun cached(c: Context): Now? {
        val body = prefs(c).getString("data", null) ?: return null
        return try {
            val o = JSONObject(body)
            val cur = o.getJSONObject("current")
            val daily = o.getJSONObject("daily")
            val h = o.getJSONObject("hourly")
            val times = h.getJSONArray("time")
            val now = System.currentTimeMillis()
            val hours = (0 until times.length()).map { i ->
                Hour(
                    times.getLong(i) * 1000,
                    h.getJSONArray("temperature_2m").optDouble(i),
                    h.getJSONArray("weather_code").optInt(i),
                    h.getJSONArray("precipitation_probability").optInt(i),
                )
            }.filter { it.timeMs > now - 3600_000L }
            Now(
                cur.getDouble("temperature_2m"), cur.getInt("weather_code"), cur.optInt("is_day", 1) == 1,
                daily.getJSONArray("temperature_2m_max").getDouble(0),
                daily.getJSONArray("temperature_2m_min").getDouble(0),
                hours, prefs(c).getLong("fetched", 0L),
            )
        } catch (_: Exception) {
            null
        }
    }

    /** WMO weather code -> label, icon and a matching expressive shape. */
    fun describe(code: Int, day: Boolean = true): Triple<String, Int, String> = when (code) {
        0 -> Triple("Clear", if (day) R.drawable.w_sun else R.drawable.w_moon, if (day) "sunny" else "cookie12")
        1 -> Triple("Mostly clear", if (day) R.drawable.w_partly else R.drawable.w_moon, if (day) "sunny" else "cookie12")
        2 -> Triple("Partly cloudy", if (day) R.drawable.w_partly else R.drawable.w_moon, "cookie9")
        3 -> Triple("Cloudy", R.drawable.w_cloud, "puffy")
        45, 48 -> Triple("Fog", R.drawable.w_fog, "bun")
        51, 53, 55, 56, 57 -> Triple("Drizzle", R.drawable.w_rain, "ghostish")
        61, 63, 65, 66, 67, 80, 81, 82 -> Triple(if (code >= 80) "Showers" else "Rain", R.drawable.w_rain, "ghostish")
        71, 73, 75, 77, 85, 86 -> Triple("Snow", R.drawable.w_snow, "softboom")
        95, 96, 99 -> Triple("Thunderstorm", R.drawable.w_storm, "burst")
        else -> Triple("—", R.drawable.w_cloud, "cookie9")
    }

    fun deg(t: Double) = "${Math.round(t)}°"
}

object WeatherWidget : Kind(
    "weather", "Weather", "Current weather in a shape that matches it, with highs and lows and the next hours. Uses Open-Meteo.",
    R.drawable.w_partly, WeatherProvider::class.java,
) {
    init {
        DataSources.register { ctx -> Weather.refresh(ctx) }
    }

    override fun defaults() = Cfg().with("shape", "auto").with("role", "primary").with("animate", true)

    private val hourIds = arrayOf(
        intArrayOf(R.id.h0, R.id.ht0, R.id.hi0, R.id.hv0),
        intArrayOf(R.id.h1, R.id.ht1, R.id.hi1, R.id.hv1),
        intArrayOf(R.id.h2, R.id.ht2, R.id.hi2, R.id.hv2),
        intArrayOf(R.id.h3, R.id.ht3, R.id.hi3, R.id.hv3),
        intArrayOf(R.id.h4, R.id.ht4, R.id.hi4, R.id.hv4),
        intArrayOf(R.id.h5, R.id.ht5, R.id.hi5, R.id.hv5),
    )

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews =
        Sizes.sized(context, widgetId) { w, h ->
            when {
                w >= 200f && h >= 190f -> build(context, widgetId, cfg, R.layout.widget_weather_big, 104f)
                w >= 200f && w > h * 1.5f -> build(context, widgetId, cfg, R.layout.widget_weather_wide, min(h - 16f, 120f))
                else -> build(context, widgetId, cfg, R.layout.widget_weather, min(w, h) - if (cfg.card) 20f else 8f)
            }
        }

    private fun build(context: Context, widgetId: Int, cfg: Cfg, layout: Int, d: Float): RemoteViews {
        val v = RemoteViews(context.packageName, layout)
        v.card(cfg)
        val now = Weather.cached(context)
        val place = Weather.place(context)
        val p = cfg.paint()
        val (label, icon, autoShape) = Weather.describe(now?.code ?: 3, now?.day ?: true)
        val auto = cfg.str("shape", "auto") == "auto"
        val shape = if (auto) Shapes.byId(autoShape) else cfg.shape
        val spin = cfg.bool("animate", true)
        // Sunshine gets a turning, breathing sun; everything else its shape (turning if animated).
        v.size(R.id.wx_frame, d, d)
        v.blob(
            R.id.wx_blob,
            if (spin && auto && autoShape == "sunny") Blob.animated(context, Morphs.sun, p.fill)
            else Blob.of(context, shape, p.fill, spin),
        )
        val i = shape.inner(0f, 0f, d, spin)
        val iconSize = i.side * 0.5f
        v.place(R.id.wx_icon, i.cx - iconSize / 2, i.cy - i.side * 0.47f, iconSize, iconSize)
        v.setImageViewResource(R.id.wx_icon, icon)
        v.tint(R.id.wx_icon, p.on)
        v.setTextViewText(R.id.wx_temp, now?.let { Weather.deg(it.temp) } ?: "–")
        v.textAt(R.id.wx_temp, i.cx, i.cy + i.side * 0.26f, d, min(fitDigits(i.side, 3.2f), i.side * 0.4f))
        v.textColor(R.id.wx_temp, p.on)

        v.setTextViewText(
            R.id.wx_place,
            when {
                place == null -> if (Weather.auto(context)) "Finding you…" else "Set a location in the app"
                now == null -> place.name.substringBefore(",")
                else -> place.name.substringBefore(",")
            },
        )
        v.setTextViewText(R.id.wx_cond, if (now != null) label else "Loading…")
        v.setTextViewText(R.id.wx_range, now?.let { "H ${Weather.deg(it.high)} · L ${Weather.deg(it.low)}" } ?: "")
        // Next rain in the coming hours, if any.
        val rain = now?.hours?.take(12)?.firstOrNull { it.rainPct >= 50 }
        v.setTextViewText(R.id.wx_rain, rain?.let { "Rain likely at ${hourLabel(context, it.timeMs)}" } ?: "")
        v.show(R.id.wx_rain, rain != null)

        val hours = now?.hours?.drop(1)?.take(hourIds.size).orEmpty()
        hourIds.forEachIndexed { i, (root, time, ic, temp) ->
            val h = hours.getOrNull(i)
            v.show(root, h != null)
            if (h == null) return@forEachIndexed
            v.setTextViewText(time, hourLabel(context, h.timeMs))
            v.setImageViewResource(ic, Weather.describe(h.code).second)
            v.tint(ic, R.color.w_text2)
            v.setTextViewText(temp, Weather.deg(h.temp))
        }

        // Tap: open the phone's weather app (or a weather search if there is none).
        v.setOnClickPendingIntent(android.R.id.background, Widgets.activity(context, 7000 + widgetId, weatherApp(context)))
        return v
    }

    private val weatherApps = listOf(
        "com.google.android.apps.weather", "com.samsung.android.weather", "com.sec.android.daemonapp",
        "com.miui.weather2", "net.oneplus.weather", "com.oplus.weather.service", "com.coloros.weather2",
        "com.motorola.timeweatherwidget", "com.huawei.android.totemweather", "org.breezyweather",
    )

    private fun weatherApp(context: Context): Intent {
        val pm = context.packageManager
        for (pkg in weatherApps) pm.getLaunchIntentForPackage(pkg)?.let { return it }
        return Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=weather"))
    }

    private fun hourLabel(context: Context, ms: Long): String =
        SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h a", Locale.getDefault())
            .format(Date(ms))
}

private operator fun IntArray.component4() = this[3]

class WeatherProvider : BaseProvider("weather")

class WeatherRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Weather.refresh(context, maxAgeMs = 0)
                Widgets.update(context, WeatherWidget)
            } finally {
                pending.finish()
            }
        }
    }
}
