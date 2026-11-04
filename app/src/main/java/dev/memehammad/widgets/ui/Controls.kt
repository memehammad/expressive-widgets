package dev.memehammad.widgets.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.memehammad.widgets.R
import dev.memehammad.widgets.Shapes

@Composable
fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Card with rounded corners for a group of settings. */
@Composable
fun Group(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) { content() }
    }
}

/**
 * Horizontal strip of shapes; the selected one is filled with the accent colour. [only] limits the
 * choice; [autoLabel] adds a first "automatic" entry with id "auto".
 */
@Composable
fun ShapePicker(selected: String, only: List<String>? = null, autoLabel: String? = null, onPick: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shapes = Shapes.all.filter { only == null || it.id in only }
    val sel = if (selected == "auto") "auto" else Shapes.byId(selected).id
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (autoLabel != null) {
            item(key = "auto") {
                PickerItem(R.drawable.shp_sunny, autoLabel, sel == "auto", scheme.tertiary) { onPick("auto") }
            }
        }
        items(shapes, key = { it.id }) { s ->
            PickerItem(s.res, s.label, s.id == sel, scheme.primary) { onPick(s.id) }
        }
    }
}

@Composable
private fun PickerItem(res: Int, label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Image(
            painterResource(res),
            contentDescription = label,
            colorFilter = ColorFilter.tint(if (selected) accent else scheme.surfaceContainerHighest),
            modifier = Modifier.size(52.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else scheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp).padding(top = 4.dp),
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** Single-choice chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Choice(options: List<Pair<T, String>>, selected: T, onPick: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        for ((value, label) in options) {
            FilterChip(selected = value == selected, onClick = { onPick(value) }, label = { Text(label) }, shape = CircleShape)
        }
    }
}

/** Multi-choice chips that keep the order in which they were picked; [max] limits the count. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Picks(options: List<Pair<String, String>>, selected: List<String>, max: Int, onChange: (List<String>) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        for ((value, label) in options) {
            val idx = selected.indexOf(value)
            FilterChip(
                selected = idx >= 0,
                onClick = {
                    onChange(
                        when {
                            idx >= 0 -> selected - value
                            selected.size < max -> selected + value
                            else -> selected.drop(1) + value
                        },
                    )
                },
                label = { Text(if (idx >= 0) "${idx + 1}. $label" else label) },
                shape = CircleShape,
            )
        }
    }
}

@Composable
fun SwitchRow(title: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (desc != null) Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Icon inside a filled expressive shape. */
@Composable
fun ShapeIcon(shape: Int, icon: Int, fill: Color, tint: Color, size: Int = 48) {
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Image(painterResource(shape), contentDescription = null, colorFilter = ColorFilter.tint(fill), modifier = Modifier.size(size.dp))
        Image(painterResource(icon), contentDescription = null, colorFilter = ColorFilter.tint(tint), modifier = Modifier.size((size * 0.46f).dp))
    }
}

/**
 * Picks the shared "Home screen" card colour: one wallpaper tone for light mode and one for
 * dark mode, so cards can match the launcher's icon backgrounds exactly.
 */
@Composable
fun HomeColourPicker(onChange: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var home by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(dev.memehammad.widgets.Palette.home(context)) }
    for ((dark, title) in listOf(false to "Light mode", true to "Dark mode")) {
        val selected = if (dark) home.second else home.first
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp),
        )
        for ((fam, label) in dev.memehammad.widgets.Palette.families) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
                items(dev.memehammad.widgets.Palette.tones) { tone ->
                    val spec = "$fam:$tone"
                    val color = Color(context.getColor(dev.memehammad.widgets.Palette.res(spec)))
                    val sel = spec == selected
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(34.dp)
                            .background(color, if (sel) RoundedCornerShape(10.dp) else CircleShape)
                            .then(
                                if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            )
                            .clickable {
                                if (dark) dev.memehammad.widgets.Palette.setHome(context, dark = spec)
                                else dev.memehammad.widgets.Palette.setHome(context, light = spec)
                                home = dev.memehammad.widgets.Palette.home(context)
                                onChange()
                            },
                    )
                }
            }
        }
    }
}
