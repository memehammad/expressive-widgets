package dev.memehammad.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.memehammad.widgets.ui.AppTheme
import dev.memehammad.widgets.ui.Choice
import dev.memehammad.widgets.ui.Group
import dev.memehammad.widgets.ui.HomeColourPicker
import dev.memehammad.widgets.ui.Label
import dev.memehammad.widgets.ui.Picks
import dev.memehammad.widgets.ui.ShapePicker
import dev.memehammad.widgets.ui.SwitchRow
import dev.memehammad.widgets.widgets.ClockWidget
import dev.memehammad.widgets.widgets.StepsWidget
import dev.memehammad.widgets.widgets.TogglesWidget
import dev.memehammad.widgets.widgets.WorldClockWidget

/**
 * Settings for one widget (shown when adding it, and via long-press → settings). Every change is
 * saved and applied to the widget straight away.
 */
class ConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val kind = Widgets.kindOf(this, widgetId)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || kind == null) {
            finish()
            return
        }
        // Adding the widget succeeds even if this screen is simply closed.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        // Save the defaults right away so the widget looks right immediately.
        Configs.save(this, widgetId, Widgets.cfg(this, widgetId, kind))
        Widgets.update(this, kind, intArrayOf(widgetId))

        setContent {
            AppTheme {
                var cfg by remember { mutableStateOf(Widgets.cfg(this, widgetId, kind)) }
                fun set(k: String, v: Any) {
                    cfg = cfg.with(k, v)
                    Configs.save(this, widgetId, cfg)
                    Widgets.update(this, kind, intArrayOf(widgetId))
                }

                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                  Column(Modifier.systemBarsPadding()) {
                    Preview(kind, widgetId, cfg)
                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
                    ) {
                        item {
                            Text(kind.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 8.dp))
                            Text(
                                kind.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                            )
                        }

                        item { Label(if (kind.id == "clock") "Shape (hours)" else "Shape") }
                        item {
                            Group {
                                when (kind.id) {
                                    "battery" -> ShapePicker(cfg.shape.id, only = Liquid.shapes) { set("shape", it) }
                                    "weather" -> ShapePicker(cfg.str("shape", "auto"), autoLabel = "Weather") { set("shape", it) }
                                    "steps" -> ShapePicker(cfg.shape.id, only = Liquid.shapes) { set("shape", it) }
                                    "activity" -> ShapePicker(cfg.str("shape", "auto"), autoLabel = "Sport") { set("shape", it) }
                                    else -> ShapePicker(cfg.shape.id) { set("shape", it) }
                                }
                            }
                        }

                        item { Label("Colour") }
                        item {
                            Group {
                                Choice(Role.entries.map { it to it.label }, cfg.role) { set("role", it.id) }
                                Choice(listOf("tonal" to "Soft", "bold" to "Bold"), cfg.str("fill", "tonal")) { set("fill", it) }
                                if (kind.id in listOf("clock", "weather", "toggles", "agenda", "activity")) {
                                    SwitchRow("Animate", "Shapes slowly turn (and the sun breathes)", cfg.bool("animate", kind.id !in listOf("agenda", "activity"))) { set("animate", it) }
                                }
                            }
                        }
                        item { Label("Background") }
                        item {
                            Group {
                                Choice(listOf("off" to "None") + cardColors, if (cfg.card) cfg.cardColor else "off") {
                                    if (it == "off") set("card", false) else {
                                        set("cardColor", it)
                                        set("card", true)
                                    }
                                }
                                if (cfg.card && cfg.cardColor == "icons") {
                                    Text(
                                        "Home screen colour is shared by all widgets. Pick the tone that matches your icons:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                    )
                                    HomeColourPicker {
                                        Widgets.updateAll(this@ConfigActivity)
                                        set("homeRev", System.currentTimeMillis())
                                    }
                                }
                            }
                        }

                        item { KindOptions(kind, cfg, ::set) }

                        item {
                            Button(
                                onClick = { finish() },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp),
                            ) { Text("Done") }
                        }
                    }
                  }
                }
            }
        }
    }

    /** The real widget, drawn live at its home-screen size on a colourful backdrop. */
    @Composable
    private fun Preview(kind: Kind, widgetId: Int, cfg: Cfg) {
        val scheme = MaterialTheme.colorScheme
        val size = remember { Sizes.of(this, widgetId).first() }
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(230.dp).padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer, scheme.secondaryContainer))),
            contentAlignment = Alignment.Center,
        ) {
            val scale = minOf(1f, (maxWidth.value - 24f) / size.width, (maxHeight.value - 24f) / size.height)
            key(cfg.json.toString()) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).also { frame ->
                            Sizes.forced = size
                            val rv = try {
                                kind.render(ctx, widgetId, cfg)
                            } catch (e: Exception) {
                                null
                            } finally {
                                Sizes.forced = null
                            }
                            rv?.let { frame.addView(runCatching { it.apply(ctx, frame) }.getOrNull() ?: return@let) }
                        }
                    },
                    modifier = Modifier.requiredSize(size.width.dp, size.height.dp).graphicsLayer(scaleX = scale, scaleY = scale),
                )
            }
            // Swallow taps so the preview doesn't open apps or flip toggles.
            Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { } })
        }
    }

    @Composable
    private fun KindOptions(kind: Kind, cfg: Cfg, set: (String, Any) -> Unit) {
        when (kind.id) {
            "clock" -> {
                val style = cfg.str("style", cfg.str("layout", "duo"))
                Label("Clock")
                Group {
                    Choice(ClockWidget.styles, style) { set("style", it) }
                    SwitchRow("Date", "A little date chip", cfg.bool("date", true)) { set("date", it) }
                    if (style == "analog") SwitchRow("Seconds", "A dot that orbits the dial", cfg.bool("seconds", true)) { set("seconds", it) }
                }
                Label(if (style == "duo" || style == "trio") "Minutes shape and colour" else "Accent colour")
                Group {
                    if (style == "duo" || style == "trio") ShapePicker(cfg.str("shape2", "clover4")) { set("shape2", it) }
                    Choice(Role.entries.map { it to it.label }, Role.byId(cfg.str("role2", "tertiary"))) { set("role2", it.id) }
                }
                if (style == "trio") {
                    Label("Date shape")
                    Group { ShapePicker(cfg.str("shape3", "sunny")) { set("shape3", it) } }
                }
            }
            "weather" -> {
                Label("Style")
                Group {
                    Choice(listOf("shape" to "Shape", "big" to "Big temperature"), cfg.str("style", "shape")) { set("style", it) }
                }
            }
            "battery" -> {
                Label("Battery")
                Group {
                    Choice(listOf("liquid" to "Liquid", "pebbles" to "Pebbles"), cfg.str("style", "liquid")) { set("style", it) }
                    if (cfg.str("style", "liquid") == "pebbles") {
                        Text(
                            "Every other pebble uses this shape:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                        ShapePicker(cfg.str("shape2", "circle"), only = Liquid.shapes) { set("shape2", it) }
                    }
                    Text(
                        "Charging shows a morphing badge. Accent colour:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                    Choice(Role.entries.map { it to it.label }, Role.byId(cfg.str("role2", "tertiary"))) { set("role2", it.id) }
                    ShizukuRow(text = "Battery health needs Shizuku; watts, temperature and cycles work without it.")
                }
            }
            "steps" -> {
                Label("Daily goal")
                Group {
                    Choice(StepsWidget.goals.map { it to "%,d".format(it) }, cfg.int("goal", 8000)) { set("goal", it) }
                    SwitchRow("Week", "Bars for the last 7 days on big sizes", cfg.bool("week", true)) { set("week", it) }
                }
                Label("Goal colour")
                Group { Choice(Role.entries.map { it to it.label }, Role.byId(cfg.str("role2", "tertiary"))) { set("role2", it.id) } }
            }
            "world" -> {
                Label("Cities (up to 3)")
                Group {
                    Picks(
                        WorldClockWidget.cities.map { it.first to it.first },
                        cfg.list("cities", listOf("London", "New York", "Tokyo")), 3,
                    ) { set("cities", it) }
                    SwitchRow("Mixed", "Each city in its own shape and colour", cfg.bool("mixed", true)) { set("mixed", it) }
                }
            }
            "toggles" -> {
                Label("Toggles (up to 4)")
                Group {
                    Picks(
                        TogglesWidget.Toggle.entries.map { it.id to it.label },
                        cfg.list("toggles", listOf("wifi", "data", "bluetooth", "flashlight")), 4,
                    ) { set("toggles", it) }
                    SwitchRow("Labels", "Name under each toggle", cfg.bool("labels", true)) { set("labels", it) }
                    SwitchRow("Mixed colours", "Each toggle in its own colour when on", cfg.bool("mixed", true)) { set("mixed", it) }
                    ShizukuRow()
                }
                Label("Shape when off")
                Group { ShapePicker(cfg.str("shape_off", "circle")) { set("shape_off", it) } }
            }
        }
    }

    @Composable
    private fun ShizukuRow(text: String = "Without Shizuku, toggles open the setting instead of switching directly.") {
        val ok = Shell.granted()
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (ok) "Shizuku is connected." else text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!ok && Shell.running()) TextButton(onClick = { Shell.requestPermission(1) }) { Text("Allow") }
        }
    }
}
