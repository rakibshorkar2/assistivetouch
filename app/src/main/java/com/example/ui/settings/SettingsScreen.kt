package com.example.ui.settings

import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AudioVolumeManager
import com.example.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val prefs by viewModel.userPreferences.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Floating Assistant Appearance
            item {
                SettingsSectionHeader(title = "Floating Button Appearance", icon = Icons.Default.Smartphone)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Button Skin Customization
                        Column {
                            Text("Floating Button Skin", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Choose visual appearance for the floating overlay button.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            val skins = listOf(
                                "ASSISTIVE_TOUCH" to "Assistive Touch",
                                "MINIMAL_DOT" to "Minimal Dot",
                                "GLASSMORPHIC_ORB" to "Glass Orb",
                                "CYBERPUNK_NEON" to "Cyber Neon"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                skins.forEach { (skinKey, label) ->
                                    val selected = prefs.buttonSkin == skinKey
                                    FilterChip(
                                        selected = selected,
                                        onClick = { viewModel.setButtonSkin(skinKey) },
                                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Button Size
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Button Size", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${prefs.buttonSizeDp} dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val sizes = listOf(48 to "Small (48dp)", 56 to "Medium (56dp)", 68 to "Large (68dp)")
                                sizes.forEachIndexed { index, (size, label) ->
                                    SegmentedButton(
                                        selected = prefs.buttonSizeDp == size,
                                        onClick = { viewModel.setButtonSizeDp(size) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = sizes.size)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Button Opacity
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Button Opacity", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${(prefs.buttonOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = prefs.buttonOpacity,
                                onValueChange = { viewModel.setButtonOpacity(it) },
                                valueRange = 0.3f..1.0f
                            )
                        }

                        // Auto Dim when Idle
                        SettingsToggleRow(
                            title = "Auto-Dim When Idle",
                            description = "Automatically reduces opacity when button is inactive for 3 seconds.",
                            checked = prefs.autoDimOnIdle,
                            onCheckedChange = { viewModel.setAutoDimOnIdle(it) }
                        )

                        if (prefs.autoDimOnIdle) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Idle Opacity Level", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${(prefs.idleDimOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = prefs.idleDimOpacity,
                                    onValueChange = { viewModel.setIdleDimOpacity(it) },
                                    valueRange = 0.1f..0.5f
                                )
                            }
                        }

                        // Edge Snap
                        SettingsToggleRow(
                            title = "Snap to Screen Edges",
                            description = "Automatically moves the floating button to left or right screen edge when released.",
                            checked = prefs.edgeSnapEnabled,
                            onCheckedChange = { viewModel.setEdgeSnap(it) }
                        )

                        // Haptic Feedback
                        SettingsToggleRow(
                            title = "Haptic Vibration",
                            description = "Vibrate briefly on tap, double-tap, or dragging interaction.",
                            checked = prefs.hapticFeedback,
                            onCheckedChange = { viewModel.setHapticFeedback(it) }
                        )
                    }
                }
            }

            // 2. Gestures & Interactions
            item {
                SettingsSectionHeader(title = "Gesture Actions", icon = Icons.Default.Tune)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ActionPickerRow(
                            label = "Single Tap Action",
                            currentValue = prefs.singleTapAction,
                            onSelected = { viewModel.setSingleTapAction(it) }
                        )

                        ActionPickerRow(
                            label = "Double Tap Action",
                            currentValue = prefs.doubleTapAction,
                            onSelected = { viewModel.setDoubleTapAction(it) }
                        )

                        ActionPickerRow(
                            label = "Triple Tap Action",
                            currentValue = prefs.tripleTapAction,
                            onSelected = { viewModel.setTripleTapAction(it) }
                        )

                        ActionPickerRow(
                            label = "Long Press Action",
                            currentValue = prefs.longPressAction,
                            onSelected = { viewModel.setLongPressAction(it) }
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ) {}

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Accessibility Shortcuts Permission",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Required for Lock Screen & Screenshot global gestures",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 3. Audio & Stream
            item {
                SettingsSectionHeader(title = "Audio Stream Settings", icon = Icons.Default.VolumeUp)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Default Audio Stream",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Choose which audio stream the assistant controls by default.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        AudioVolumeManager.SUPPORTED_STREAMS.forEach { (streamType, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setAudioStream(streamType) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = name, style = MaterialTheme.typography.bodyMedium)
                                Icon(
                                    imageVector = if (prefs.audioStream == streamType) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (prefs.audioStream == streamType) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 4. Smart Automation & Safety Limiter
            item {
                SettingsSectionHeader(title = "Safety & Smart Automation", icon = Icons.Default.AppShortcut)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Safety Limiter
                        SettingsToggleRow(
                            title = "Headphone Volume Safety Limiter",
                            description = "Prevents accidental volume spikes when headphones or Bluetooth audio devices are connected.",
                            checked = prefs.safetyLimiterEnabled,
                            onCheckedChange = { viewModel.setSafetyLimiterEnabled(it) }
                        )

                        if (prefs.safetyLimiterEnabled) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Maximum Safe Volume Cap", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${prefs.maxSafetyVolumePercent}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = prefs.maxSafetyVolumePercent.toFloat(),
                                    onValueChange = { viewModel.setMaxSafetyVolumePercent(it.toInt()) },
                                    valueRange = 30f..90f,
                                    steps = 5
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ) {}

                        // Headphone Auto-Profile
                        SettingsToggleRow(
                            title = "Bluetooth / Headphone Auto-Volume Profile",
                            description = "Automatically sets media volume to a preset level when headphones or Bluetooth audio connects.",
                            checked = prefs.headphoneAutoProfileEnabled,
                            onCheckedChange = { viewModel.setHeadphoneAutoProfileEnabled(it) }
                        )

                        if (prefs.headphoneAutoProfileEnabled) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Connected Media Volume", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${prefs.headphoneTargetVolumePercent}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = prefs.headphoneTargetVolumePercent.toFloat(),
                                    onValueChange = { viewModel.setHeadphoneTargetVolumePercent(it.toInt()) },
                                    valueRange = 10f..90f,
                                    steps = 7
                                )
                            }
                        }
                    }
                }
            }

            // 4. Behavior & Theme
            item {
                SettingsSectionHeader(title = "Behavior & Theme", icon = Icons.Default.ColorLens)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Theme Mode
                        Column {
                            Text("App Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val themes = listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark")
                                themes.forEachIndexed { index, (mode, label) ->
                                    SegmentedButton(
                                        selected = prefs.themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themes.size)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Popup Timeout
                        Column {
                            Text("Popup Auto-Close Timeout", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Close the popup overlay automatically after inactivity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                val timeouts = listOf(3 to "3s", 5 to "5s", 10 to "10s", 0 to "Never")
                                timeouts.forEachIndexed { index, (sec, label) ->
                                    SegmentedButton(
                                        selected = prefs.popupTimeoutSeconds == sec,
                                        onClick = { viewModel.setPopupTimeout(sec) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = timeouts.size)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Start on Boot
                        SettingsToggleRow(
                            title = "Start Assistant on Boot",
                            description = "Restore floating assistant button automatically after device restarts.",
                            checked = prefs.startOnBoot,
                            onCheckedChange = { viewModel.setStartOnBoot(it) }
                        )

                        // Persistent Notification
                        SettingsToggleRow(
                            title = "Persistent Service Notification",
                            description = "Keep ongoing notification active to prevent Android system from killing overlay service.",
                            checked = prefs.persistentNotification,
                            onCheckedChange = { viewModel.setPersistentNotification(it) }
                        )
                    }
                }
            }

            // 5. About Section
            item {
                SettingsSectionHeader(title = "About", icon = Icons.Default.Info)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Volume Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Version v1.0.3 (Production Build)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "Developer:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "RAKIB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "A modern, lightweight system audio floating overlay controller for Android. Built with Jetpack Compose, Material Design 3, and Kotlin Coroutines.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Developed by RAKIB • v1.0.3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionPickerRow(
    label: String,
    currentValue: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(
        "POPUP" to "Open Volume Popup",
        "VOL_UP" to "Volume Up (+1)",
        "VOL_DOWN" to "Volume Down (-1)",
        "MUTE" to "Mute / Unmute",
        "LOCK_SCREEN" to "Lock Screen",
        "SCREENSHOT" to "Take Screenshot",
        "FLASHLIGHT" to "Toggle Flashlight",
        "NOTIFICATION_SHADE" to "Pull Notification Shade",
        "SETTINGS" to "Open Settings",
        "DISABLED" to "Disabled"
    )

    val currentLabel = options.find { it.first == currentValue }?.second ?: "Open Volume Popup"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = currentLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = currentLabel, style = MaterialTheme.typography.labelSmall)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (key, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelected(key)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
