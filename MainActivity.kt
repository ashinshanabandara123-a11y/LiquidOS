package com.liquidos.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

private data class LaunchApp(val label: String, val packageName: String, val icon: Bitmap)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setContent { LiquidLauncher() }
    }
}

@Composable
private fun LiquidLauncher() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("liquidos", Context.MODE_PRIVATE) }
    val apps = remember { loadApps(context) }
    var screen by remember { mutableStateOf("home") }
    var showControl by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var overview by remember { mutableStateOf(false) }
    var page by remember { mutableIntStateOf(0) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var iconSize by remember { mutableFloatStateOf(prefs.getFloat("iconSize", 60f)) }
    var blur by remember { mutableFloatStateOf(prefs.getFloat("blur", 70f)) }
    var gridCols by remember { mutableIntStateOf(prefs.getInt("cols", 4)) }
    var labels by remember { mutableStateOf(prefs.getBoolean("labels", true)) }
    var dockStyle by remember { mutableStateOf(prefs.getBoolean("dock", true)) }
    var query by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<LaunchApp?>(null) }
    val bg = if (dark) Color(0xFF101522) else Color(0xFFE8EEF8)
    val foreground = if (dark) Color.White else Color(0xFF172033)
    val glassAlpha = (blur / 100f).coerceIn(.05f, 1f) * if (dark) .19f else .78f
    val glass = if (dark) Color.White.copy(alpha = glassAlpha) else Color.White.copy(alpha = glassAlpha)

    fun launch(app: LaunchApp) {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) context.startActivity(intent)
        else Toast.makeText(context, "App cannot be opened", Toast.LENGTH_SHORT).show()
    }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    if (dark) listOf(Color(0xFF182A45), Color(0xFF604E79), Color(0xFF111827))
                    else listOf(Color(0xFFB9D9F5), Color(0xFFD9C9E9), Color(0xFFF5E9E2))
                )
            )
        ) {
            when (screen) {
                "settings" -> SettingsScreen(
                    dark, { dark = it; prefs.edit().putBoolean("dark", it).apply() },
                    iconSize, { iconSize = it; prefs.edit().putFloat("iconSize", it).apply() },
                    blur, { blur = it; prefs.edit().putFloat("blur", it).apply() },
                    gridCols, { gridCols = it; prefs.edit().putInt("cols", it).apply() },
                    labels, { labels = it; prefs.edit().putBoolean("labels", it).apply() },
                    dockStyle, { dockStyle = it; prefs.edit().putBoolean("dock", it).apply() },
                    onBack = { screen = "home" }
                )
                "library" -> LibraryScreen(apps, foreground, glass, { launch(it) }, { screen = "home" })
                else -> {
                    Column(
                        Modifier.fillMaxSize()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, amount ->
                                        if (amount > 18) showControl = true
                                        if (amount < -18) screen = "library"
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    if (zoom < .92f) overview = true
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()), color = foreground, fontWeight = FontWeight.SemiBold)
                            Text(DateFormat.getDateInstance(DateFormat.FULL).format(Date()), color = foreground.copy(.85f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(22.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassWidget("Weather", "Partly cloudy", "21°", Modifier.weight(1f), glass, foreground)
                            GlassWidget("Calendar", "Today", DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date()), Modifier.weight(1f), glass, foreground)
                        }
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassWidget("Battery", "Device status", "●", Modifier.weight(1f), glass, foreground)
                            GlassWidget("Music", "Now playing", "♫", Modifier.weight(1f), glass, foreground)
                        }
                        Spacer(Modifier.height(20.dp))
                        if (overview) {
                            Text("Home screen overview", color = foreground, fontWeight = FontWeight.Bold)
                            Text("Tap Done to return", color = foreground.copy(.7f), fontSize = 12.sp)
                            Button(onClick = { overview = false }) { Text("Done") }
                        } else {
                            val pageApps = apps.drop(page * 24).take(24)
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridCols),
                                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(pageApps, key = { it.packageName }) { app ->
                                    AppIcon(app, iconSize.dp, labels, foreground, onClick = { launch(app) }, onLongClick = { selectedApp = app })
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            repeat(maxOf(1, (apps.size + 23) / 24)) { index ->
                                Box(Modifier.padding(4.dp).size(if (page == index) 8.dp else 6.dp).clip(CircleShape)
                                    .background(if (page == index) foreground else foreground.copy(.35f))
                                    .clickable { page = index })
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)
                                .clip(RoundedCornerShape(30.dp)).background(if (dockStyle) glass else Color.Transparent).padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            apps.take(4).forEach { app ->
                                AppIcon(app, 48.dp, false, foreground, onClick = { launch(app) }, onLongClick = { selectedApp = app })
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TextButton(onClick = { showSearch = true }) { Icon(Icons.Default.Search, null); Text(" Search") }
                            TextButton(onClick = { screen = "library" }) { Icon(Icons.Default.Apps, null); Text(" Library") }
                            TextButton(onClick = { screen = "settings" }) { Icon(Icons.Default.Settings, null); Text(" Settings") }
                            TextButton(onClick = { showControl = true }) { Icon(Icons.Default.Tune, null); Text(" Control") }
                        }
                    }
                }
            }

            if (showControl) ControlCenter(glass, foreground, onClose = { showControl = false })
            if (showSearch) SearchDialog(apps, foreground, { launch(it) }, { showSearch = false })
            selectedApp?.let { app ->
                Dialog(onDismissRequest = { selectedApp = null }) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(glass).padding(22.dp)) {
                        Text(app.label, color = foreground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        listOf("Open", "App info", "Cancel").forEach { action ->
                            TextButton(onClick = {
                                when (action) {
                                    "Open" -> launch(app)
                                    "App info" -> {
                                        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.parse("package:${app.packageName}")
                                        }
                                        context.startActivity(i)
                                    }
                                }
                                selectedApp = null
                            }) { Text(action) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassWidget(title: String, subtitle: String, value: String, modifier: Modifier, glass: Color, text: Color) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(glass).padding(15.dp)) {
        Text(title, color = text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(subtitle, color = text.copy(.65f), fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        Text(value, color = text, fontSize = 20.sp, fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIcon(app: LaunchApp, size: androidx.compose.ui.unit.Dp, labels: Boolean, text: Color, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        androidx.compose.foundation.Image(
            bitmap = app.icon.asImageBitmap(),
            contentDescription = app.label,
            modifier = Modifier.size(size).clip(RoundedCornerShape(size * .235f))
        )
        if (labels) Text(app.label, color = text, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ControlCenter(glass: Color, text: Color, onClose: () -> Unit) {
    val context = LocalContext.current
    var brightness by remember { mutableFloatStateOf(.7f) }
    var volume by remember { mutableFloatStateOf(.55f) }
    var active by remember { mutableStateOf(setOf("Airplane", "Focus")) }
    val tiles = listOf("Wi-Fi", "Bluetooth", "Cellular", "Airplane", "Flashlight", "Rotation", "Focus", "Screen record")
    Column(Modifier.fillMaxSize().background(Color.Black.copy(.48f)).clickable { onClose() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth().padding(14.dp).clip(RoundedCornerShape(32.dp)).background(glass).clickable(enabled = false) {}.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Control Center", color = text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = text) }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(210.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tiles) { tile ->
                    val enabled = tile in active
                    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(if (enabled) Color(0xFF477BCB) else Color.White.copy(.15f))
                        .clickable {
                            active = if (enabled) active - tile else active + tile
                            if (tile == "Wi-Fi") context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                            if (tile == "Bluetooth") context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tile, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Text("Brightness", color = text, modifier = Modifier.padding(top = 16.dp))
            Slider(value = brightness, onValueChange = { brightness = it })
            Text("Volume", color = text)
            Slider(value = volume, onValueChange = { volume = it })
            Text("Media", color = text, fontWeight = FontWeight.Bold)
            Text("No media playing", color = text.copy(.7f), fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SearchDialog(apps: List<LaunchApp>, text: Color, onLaunch: (LaunchApp) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xEE20283A)).padding(18.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Search apps") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
            Spacer(Modifier.height(10.dp))
            val results = apps.filter { it.label.contains(query, true) }.take(12)
            results.forEach { app ->
                TextButton(onClick = { onLaunch(app); onClose() }, modifier = Modifier.fillMaxWidth()) {
                    Text(app.label, color = Color.White, modifier = Modifier.fillMaxWidth())
                }
            }
            if (results.isEmpty()) Text("No matching apps", color = Color.White.copy(.7f))
        }
    }
}

@Composable
private fun LibraryScreen(apps: List<LaunchApp>, text: Color, glass: Color, onLaunch: (LaunchApp) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = apps.filter { it.label.contains(query, true) }.sortedBy { it.label.lowercase() }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = text) }
            Text("App Library", color = text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search apps") }, leadingIcon = { Icon(Icons.Default.Search, null) })
        Text("Recently used", color = text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(glass).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            apps.take(4).forEach { AppIcon(it, 42.dp, false, text, { onLaunch(it) }, {}) }
        }
        Text("All apps · A–Z", color = text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(filtered, key = { it.packageName }) { AppIcon(it, 48.dp, true, text, { onLaunch(it) }, {}) }
        }
    }
}

@Composable
private fun SettingsScreen(
    dark: Boolean, setDark: (Boolean) -> Unit,
    iconSize: Float, setIconSize: (Float) -> Unit,
    blur: Float, setBlur: (Float) -> Unit,
    cols: Int, setCols: (Int) -> Unit,
    labels: Boolean, setLabels: (Boolean) -> Unit,
    dock: Boolean, setDock: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        androidx.compose.foundation.lazy.LazyColumn {
            item { SettingHeader("Appearance") }
            item { SettingSwitch("Dark appearance", dark, setDark) }
            item { SettingSwitch("Show icon labels", labels, setLabels) }
            item { SettingSwitch("Translucent dock", dock, setDock) }
            item { SettingSlider("Icon size", iconSize, 40f, 84f, setIconSize, "${iconSize.toInt()} dp") }
            item { SettingSlider("Glass intensity", blur, 10f, 100f, setBlur, "${blur.toInt()}%") }
            item {
                Text("Grid columns: $cols", modifier = Modifier.padding(top = 14.dp), fontWeight = FontWeight.Medium)
                Slider(value = cols.toFloat(), onValueChange = { setCols(it.toInt().coerceIn(3, 6)) }, valueRange = 3f..6f, steps = 2)
            }
            item { SettingHeader("Gestures & navigation") }
            item { Text("Swipe up opens App Library · swipe down opens Control Center · tap Search for Spotlight.", modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium) }
            item { SettingHeader("App library") }
            item { Text("Alphabetical listing with a recently used section. Android does not expose reliable usage history without Usage Access permission.", modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium) }
            item { SettingHeader("Backup & restore") }
            item {
                Button(onClick = {
                    val json = """{"theme":"${if (dark) "dark" else "light"}","iconSize":$iconSize,"glassIntensity":$blur,"gridColumns":$cols,"labels":$labels,"dock":$dock}"""
                    val send = Intent(Intent.ACTION_SEND).apply { type = "application/json"; putExtra(Intent.EXTRA_TEXT, json) }
                    context.startActivity(Intent.createChooser(send, "Export LiquidOS configuration"))
                }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Share, null); Text(" Export configuration JSON") }
            }
            item { Text("Import: open a previously exported JSON file and apply its values manually. Configuration is stored locally on this device.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
            item { SettingHeader("System integrations") }
            item {
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, null); Text(" Open Android system settings")
                }
            }
        }
    }
}

@Composable private fun SettingHeader(value: String) {
    Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
}
@Composable private fun SettingSwitch(title: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title); Switch(checked = value, onCheckedChange = onChange)
    }
}
@Composable private fun SettingSlider(title: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit, suffix: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title); Text(suffix, color = MaterialTheme.colorScheme.secondary) }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}

private fun loadApps(context: Context): List<LaunchApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            try {
                LaunchApp(info.loadLabel(pm).toString(), packageName, drawableToBitmap(info.loadIcon(pm)))
            } catch (_: Exception) { null }
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width.coerceAtMost(256), height.coerceAtMost(256), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
