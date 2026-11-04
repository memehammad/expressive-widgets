package dev.memehammad.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import dev.memehammad.widgets.ui.Label
import dev.memehammad.widgets.ui.ShapeIcon

/** Catalogue of widgets with an "Add" button each, plus Shizuku status. */
class MainActivity : ComponentActivity() {
    private val shizuku = mutableStateOf(false to false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Refresher.ensure(this)
        setContent { AppTheme { Screen() } }
    }

    override fun onResume() {
        super.onResume()
        shizuku.value = Shell.running() to Shell.granted()
        Widgets.updateAll(this)
    }

    private fun pin(kind: Kind) {
        AppWidgetManager.getInstance(this)
            .requestPinAppWidget(ComponentName(this, kind.provider), null, null)
    }

    @Composable
    private fun Screen() {
        val scheme = MaterialTheme.colorScheme
        val (running, granted) = shizuku.value
        val shapes = listOf("cookie9", "clover", "sunny", "flower", "burst", "cookie6", "pentagon", "gem")
        val roles = Role.entries
        Scaffold(containerColor = scheme.surface) { pad ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = pad.calculateTopPadding() + 24.dp, bottom = pad.calculateBottomPadding() + 32.dp,
                ),
            ) {
                item {
                    Text("Expressive Widgets", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 8.dp))
                    Text(
                        "Tap Add, or long-press your home screen → Widgets. Long-press a widget to change its shape and colours.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp, end = 8.dp),
                    )
                }
                item { Label("Widgets") }
                itemsIndexed(Widgets.kinds, key = { _, k -> k.id }) { i, k ->
                    val role = roles[i % 3]
                    Surface(
                        shape = RoundedCornerShape(if (i == 0) 28.dp else 8.dp).let {
                            when {
                                Widgets.kinds.size == 1 -> RoundedCornerShape(28.dp)
                                i == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                                i == Widgets.kinds.lastIndex -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                                else -> RoundedCornerShape(6.dp)
                            }
                        },
                        color = scheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            val (fill, on) = when (role) {
                                Role.PRIMARY -> scheme.primaryContainer to scheme.onPrimaryContainer
                                Role.SECONDARY -> scheme.secondaryContainer to scheme.onSecondaryContainer
                                else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
                            }
                            ShapeIcon(Shapes.byId(shapes[i % shapes.size]).res, k.icon, fill, on, 52)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(k.title, style = MaterialTheme.typography.titleMedium)
                                Text(k.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(onClick = { pin(k) }, shape = CircleShape) { Text("Add") }
                        }
                    }
                }

                item { Label("Shizuku") }
                item {
                    Surface(shape = RoundedCornerShape(28.dp), color = scheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    when {
                                        granted -> "Connected"
                                        running -> "Running, not allowed yet"
                                        else -> "Not running"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "Optional. Lets toggles switch directly and the battery widget show health.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            when {
                                granted -> {}
                                running -> TextButton(onClick = { Shell.requestPermission(1) }) { Text("Allow") }
                                else -> TextButton(onClick = {
                                    val launch = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                                    startActivity(launch)
                                }) { Text("Get it") }
                            }
                        }
                    }
                }

                item {
                    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(onClick = {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/memehammad/expressive-widgets")))
                        }) { Text("Expressive Widgets on GitHub") }
                        Text(
                            "Version ${BuildConfig.VERSION_NAME} · open source, MIT",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
