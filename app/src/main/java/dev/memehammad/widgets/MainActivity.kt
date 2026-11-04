package dev.memehammad.widgets

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.memehammad.widgets.ui.AppTheme
import dev.memehammad.widgets.ui.Label
import dev.memehammad.widgets.ui.ShapeIcon
import dev.memehammad.widgets.ui.SwitchRow
import dev.memehammad.widgets.widgets.AgendaWidget
import dev.memehammad.widgets.widgets.Weather
import dev.memehammad.widgets.widgets.WeatherWidget
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Catalogue of widgets with an "Add" button each, plus Shizuku status. */
class MainActivity : ComponentActivity() {
    private val shizuku = mutableStateOf(false to false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Refresher.ensure(this)
        setContent { AppTheme { Screen() } }
    }

    private val calendar = mutableStateOf(false)
    private val weatherPlace = mutableStateOf<Weather.Place?>(null)
    private val weatherAuto = mutableStateOf(false)
    private val bgLocation = mutableStateOf(false)

    private fun syncWeather() {
        weatherPlace.value = Weather.place(this)
        weatherAuto.value = Weather.auto(this)
        bgLocation.value = Weather.backgroundAllowed(this)
    }

    private fun refreshWeather() {
        lifecycleScope.launch {
            Weather.refresh(this@MainActivity, maxAgeMs = 0)
            syncWeather()
            Widgets.update(this@MainActivity, WeatherWidget)
        }
    }

    private fun useLocation() {
        if (Weather.locationAllowed(this)) {
            Weather.setAuto(this, true)
            syncWeather()
            refreshWeather()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 3)
        }
    }

    @Deprecated("Activity callback")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        calendar.value = AgendaWidget.allowed(this)
        if (requestCode == 3 && Weather.locationAllowed(this)) useLocation()
        syncWeather()
        Widgets.updateAll(this)
    }

    override fun onResume() {
        super.onResume()
        shizuku.value = Shell.running() to Shell.granted()
        calendar.value = AgendaWidget.allowed(this)
        syncWeather()
        if (Weather.auto(this)) refreshWeather()
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
        val shapes = listOf("cookie9", "clover4", "sunny", "puffy", "softburst", "ghostish", "flower", "heart")
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

                item { Label("Weather") }
                item { WeatherCard() }
                item { Label("Calendar") }
                item {
                    Surface(shape = RoundedCornerShape(28.dp), color = scheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(if (calendar.value) "Calendar access allowed" else "Calendar access needed", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "The agenda widget reads your events on this phone. Nothing leaves your device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                            if (!calendar.value) TextButton(onClick = { requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 2) }) { Text("Allow") }
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

    /** Location search (Open-Meteo geocoding) and units for the weather widget. */
    @Composable
    private fun WeatherCard() {
        val scheme = MaterialTheme.colorScheme
        val scope = rememberCoroutineScope()
        val place = weatherPlace.value
        val auto = weatherAuto.value
        var fahrenheit by remember { mutableStateOf(Weather.fahrenheit(this)) }
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<Weather.Place>>(emptyList()) }
        var searching by remember { mutableStateOf(false) }
        fun refresh() = refreshWeather()
        Surface(shape = RoundedCornerShape(28.dp), color = scheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text(
                    when {
                        auto -> "Current location" + (place?.let { " · ${it.name.substringBefore(",")}" } ?: "")
                        else -> place?.name ?: "No location set"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Text(
                    "Use your location or search for a city. Forecasts from Open-Meteo, no account needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("City") },
                    shape = CircleShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (query.isNotBlank()) scope.launch {
                            searching = true
                            results = Weather.search(query.trim())
                            searching = false
                        }
                    }),
                    trailingIcon = {
                        TextButton(onClick = {
                            if (query.isNotBlank()) scope.launch {
                                searching = true
                                results = Weather.search(query.trim())
                                searching = false
                            }
                        }) { Text(if (searching) "…" else "Search") }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                SwitchRow("Use my location", "Follows you; rough location is enough", auto) {
                    if (it) useLocation() else {
                        Weather.setAuto(this@MainActivity, false)
                        syncWeather()
                    }
                }
                if (auto && !bgLocation.value) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "To update while the app is closed, set location to \"Allow all the time\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 4) }) { Text("Allow") }
                    }
                }
                for (r in results) {
                    Text(
                        r.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().clickable {
                            Weather.setAuto(this@MainActivity, false)
                            Weather.setPlace(this@MainActivity, r)
                            syncWeather()
                            results = emptyList()
                            query = ""
                            refresh()
                        }.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
                SwitchRow("Fahrenheit", null, fahrenheit) {
                    fahrenheit = it
                    Weather.setFahrenheit(this@MainActivity, it)
                    refresh()
                }
            }
        }
    }
}
