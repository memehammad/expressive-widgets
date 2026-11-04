package dev.memehammad.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
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
import dev.memehammad.widgets.ui.Label
import dev.memehammad.widgets.ui.Picks
import dev.memehammad.widgets.ui.ShapePicker
import dev.memehammad.widgets.ui.SwitchRow
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
                    LazyColumn(
                        Modifier.systemBarsPadding(),
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

                        item { Label("Shape") }
                        item { Group { ShapePicker(cfg.shape.id) { set("shape", it.id) } } }

                        item { Label("Colour") }
                        item {
                            Group {
                                Choice(Role.entries.map { it to it.label }, cfg.role) { set("role", it.id) }
                                Choice(listOf("tonal" to "Soft", "bold" to "Bold"), cfg.str("fill", "tonal")) { set("fill", it) }
                                SwitchRow("Card background", "A rounded background behind the widget", cfg.card) { set("card", it) }
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

    @Composable
    private fun KindOptions(kind: Kind, cfg: Cfg, set: (String, Any) -> Unit) {
        when (kind.id) {
            "clock" -> {
                Label("Clock")
                Group {
                    Choice(listOf("stacked" to "Hours over minutes", "inline" to "Inline"), cfg.str("layout", "stacked")) { set("layout", it) }
                    SwitchRow("Show date", null, cfg.bool("date", true)) { set("date", it) }
                }
            }
            "world" -> {
                Label("Cities (up to 3)")
                Group {
                    Picks(
                        WorldClockWidget.cities.map { it.first to it.first },
                        cfg.list("cities", listOf("London", "New York", "Tokyo")), 3,
                    ) { set("cities", it) }
                    SwitchRow("Mixed colours", "Each city in a different colour role", cfg.bool("mixed", true)) { set("mixed", it) }
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
                    ShizukuRow()
                }
                Label("Shape when off")
                Group { ShapePicker(cfg.str("shape_off", "squircle")) { set("shape_off", it.id) } }
            }
            "battery" -> {
                Label("Battery")
                Group { ShizukuRow(text = "Battery health needs Shizuku; watts, temperature and cycles work without it.") }
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
