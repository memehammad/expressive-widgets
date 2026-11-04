package dev.memehammad.widgets.widgets

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.PowerManager
import android.provider.Settings
import android.util.SizeF
import android.widget.RemoteViews
import dev.memehammad.widgets.BaseProvider
import dev.memehammad.widgets.Blob
import dev.memehammad.widgets.Role
import dev.memehammad.widgets.blob
import dev.memehammad.widgets.Cfg
import dev.memehammad.widgets.Kind
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Shapes
import dev.memehammad.widgets.Shell
import dev.memehammad.widgets.Widgets
import dev.memehammad.widgets.card
import dev.memehammad.widgets.show
import dev.memehammad.widgets.textColor
import dev.memehammad.widgets.tint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Up to four system toggles. With Shizuku they switch directly (Wi-Fi, mobile data, Bluetooth,
 * airplane mode, ...); without it, tapping opens the matching settings panel. The shape changes
 * when a toggle is on.
 */
object TogglesWidget : Kind(
    "toggles", "Quick toggles", "Up to four system toggles. With Shizuku they switch directly; otherwise they open the setting.",
    R.drawable.ic_toggle, TogglesProvider::class.java,
) {
    enum class Toggle(
        val id: String,
        val label: String,
        val icon: Int,
        val on: String?,
        val off: String?,
        val settings: String,
        val needsShell: Boolean = true,
    ) {
        WIFI("wifi", "Wi-Fi", R.drawable.ic_wifi, "svc wifi enable", "svc wifi disable", Settings.Panel.ACTION_WIFI),
        DATA("data", "Mobile data", R.drawable.ic_data, "svc data enable", "svc data disable", Settings.Panel.ACTION_INTERNET_CONNECTIVITY),
        BLUETOOTH("bluetooth", "Bluetooth", R.drawable.ic_bluetooth, "svc bluetooth enable", "svc bluetooth disable", Settings.ACTION_BLUETOOTH_SETTINGS),
        AIRPLANE("airplane", "Airplane", R.drawable.ic_airplane, "cmd connectivity airplane-mode enable", "cmd connectivity airplane-mode disable", Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        FLASHLIGHT("flashlight", "Flashlight", R.drawable.ic_flashlight, null, null, "", needsShell = false),
        SAVER("saver", "Battery saver", R.drawable.ic_saver, "cmd power set-mode 1", "cmd power set-mode 0", Settings.ACTION_BATTERY_SAVER_SETTINGS),
        DND("dnd", "Do not disturb", R.drawable.ic_dnd, "cmd notification set_dnd on", "cmd notification set_dnd off", Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS),
        DARK("dark", "Dark theme", R.drawable.ic_dark, "cmd uimode night yes", "cmd uimode night no", Settings.ACTION_DISPLAY_SETTINGS),
        LOCATION("location", "Location", R.drawable.ic_location, "cmd location set-location-enabled true", "cmd location set-location-enabled false", Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        ROTATE("rotate", "Auto-rotate", R.drawable.ic_rotate, "settings put system accelerometer_rotation 1", "settings put system accelerometer_rotation 0", Settings.ACTION_DISPLAY_SETTINGS),
        SMOOTH("smooth", "Smooth display", R.drawable.ic_speed,
            "settings put system peak_refresh_rate 120; settings put system min_refresh_rate 120",
            "settings put system peak_refresh_rate 60; settings put system min_refresh_rate 60",
            Settings.ACTION_DISPLAY_SETTINGS),
        NFC("nfc", "NFC", R.drawable.ic_nfc, "svc nfc enable", "svc nfc disable", Settings.ACTION_NFC_SETTINGS);

        companion object {
            fun byId(id: String) = entries.firstOrNull { it.id == id }
        }
    }

    private fun gInt(c: Context, key: String) = Settings.Global.getInt(c.contentResolver, key, 0)
    private fun sInt(c: Context, key: String) = Settings.System.getInt(c.contentResolver, key, 0)

    fun isOn(c: Context, t: Toggle): Boolean = try {
        when (t) {
            Toggle.WIFI -> c.getSystemService(WifiManager::class.java)?.isWifiEnabled == true
            Toggle.DATA -> gInt(c, "mobile_data") == 1
            Toggle.BLUETOOTH -> gInt(c, "bluetooth_on") == 1
            Toggle.AIRPLANE -> gInt(c, Settings.Global.AIRPLANE_MODE_ON) == 1
            Toggle.FLASHLIGHT -> torchOn(c)
            Toggle.SAVER -> c.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
            Toggle.DND -> c.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
                .let { it != null && it != NotificationManager.INTERRUPTION_FILTER_ALL && it != NotificationManager.INTERRUPTION_FILTER_UNKNOWN }
            Toggle.DARK -> c.getSystemService(UiModeManager::class.java)?.nightMode == UiModeManager.MODE_NIGHT_YES
            Toggle.LOCATION -> c.getSystemService(LocationManager::class.java)?.isLocationEnabled == true
            Toggle.ROTATE -> sInt(c, "accelerometer_rotation") == 1
            Toggle.SMOOTH -> Settings.System.getFloat(c.contentResolver, "peak_refresh_rate", 60f) > 61f
            Toggle.NFC -> NfcAdapter.getDefaultAdapter(c)?.isEnabled == true
        }
    } catch (_: Exception) {
        false
    }

    // Flashlight: CameraManager needs no permission; we remember the state we set.
    private fun prefs(c: Context) = c.getSharedPreferences("toggles", Context.MODE_PRIVATE)
    private fun torchOn(c: Context) = prefs(c).getBoolean("torch", false)

    fun setTorch(c: Context, on: Boolean) {
        val cm = c.getSystemService(CameraManager::class.java) ?: return
        val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return
        try {
            cm.setTorchMode(id, on)
            prefs(c).edit().putBoolean("torch", on).apply()
        } catch (_: Exception) {
        }
    }

    override fun defaults() = Cfg().with("shape", "cookie9").with("shape_off", "circle").with("animate", true).with("role", "primary")
        .with("toggles", listOf("wifi", "data", "bluetooth", "flashlight")).with("labels", true)

    private val tiles = arrayOf(
        intArrayOf(R.id.t0, R.id.ts0, R.id.ti0, R.id.tl0),
        intArrayOf(R.id.t1, R.id.ts1, R.id.ti1, R.id.tl1),
        intArrayOf(R.id.t2, R.id.ts2, R.id.ti2, R.id.tl2),
        intArrayOf(R.id.t3, R.id.ts3, R.id.ti3, R.id.tl3),
    )

    override fun render(context: Context, widgetId: Int, cfg: Cfg): RemoteViews {
        val row = build(context, widgetId, cfg, R.layout.widget_toggles)
        val grid = build(context, widgetId, cfg, R.layout.widget_toggles_grid)
        return RemoteViews(mapOf(SizeF(40f, 40f) to row, SizeF(110f, 150f) to grid))
    }

    private fun build(context: Context, widgetId: Int, cfg: Cfg, layout: Int): RemoteViews {
        val v = RemoteViews(context.packageName, layout)
        v.card(cfg)
        val chosen = cfg.list("toggles", listOf("wifi", "data", "bluetooth", "flashlight")).mapNotNull { Toggle.byId(it) }.take(4)
        val shell = Shell.granted()
        val onShape = cfg.shape
        val offShape = Shapes.byId(cfg.str("shape_off", "circle"))
        tiles.forEachIndexed { i, (root, shape, icon, label) ->
            val t = chosen.getOrNull(i)
            v.show(root, t != null)
            if (t == null) return@forEachIndexed
            val on = isOn(context, t)
            // On: the chosen shape, slowly turning, in a bold colour (each tile its own role if mixed).
            // Off: a calm static shape.
            val role = if (cfg.bool("mixed", true)) Role.entries[(cfg.role.ordinal + i) % 3] else cfg.role
            val p = cfg.paint(role, bold = true)
            v.blob(
                shape,
                if (on) Blob.of(context, onShape, p.fill, cfg.bool("animate", true))
                else Blob.static(context, offShape, R.color.w_surface),
            )
            v.setImageViewResource(icon, t.icon)
            v.tint(icon, if (on) p.on else R.color.w_text2)
            v.setTextViewText(label, t.label)
            v.textColor(label, if (cfg.card) R.color.w_text else R.color.w_text)
            v.show(label, cfg.bool("labels", true))
            val direct = !t.needsShell || shell
            val intent = if (direct) {
                PendingIntent.getBroadcast(
                    context, widgetId * 16 + i,
                    Intent(context, ToggleReceiver::class.java).setAction("toggle:${t.id}:$widgetId"),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            } else {
                Widgets.activity(context, widgetId * 16 + i, Intent(t.settings))
            }
            v.setOnClickPendingIntent(root, intent)
        }
        return v
    }
}

private operator fun IntArray.component4() = this[3]

class TogglesProvider : BaseProvider("toggles")

/** Flips a toggle (via Shizuku or directly), then redraws once the system has applied it. */
class ToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val parts = intent.action?.split(":") ?: return
        if (parts.size < 2 || parts[0] != "toggle") return
        val t = TogglesWidget.Toggle.byId(parts[1]) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val on = TogglesWidget.isOn(context, t)
                if (t == TogglesWidget.Toggle.FLASHLIGHT) {
                    TogglesWidget.setTorch(context, !on)
                } else {
                    (if (on) t.off else t.on)?.let { Shell.run(it) }
                }
                Widgets.update(context, TogglesWidget)
                delay(1200)
                Widgets.update(context, TogglesWidget)
                delay(2500)
                Widgets.update(context, TogglesWidget)
            } finally {
                pending.finish()
            }
        }
    }
}
