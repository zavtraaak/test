package com.zavtrak.keyboard.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zavtrak.keyboard.ImeUtils
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val vm: SettingsViewModel = viewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var imeEnabled by remember { mutableStateOf(ImeUtils.isMyImeEnabled(context)) }
    var imeDefault by remember { mutableStateOf(ImeUtils.isMyImeDefault(context)) }

    LaunchedEffect(Unit) {
        // Re-check when activity resumes
        imeEnabled = ImeUtils.isMyImeEnabled(context)
        imeDefault = ImeUtils.isMyImeDefault(context)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Zavtra Keyboard", fontWeight = FontWeight.Bold) },
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OnboardingCard(imeEnabled, imeDefault) {
                imeEnabled = ImeUtils.isMyImeEnabled(context)
                imeDefault = ImeUtils.isMyImeDefault(context)
            }
            Spacer(Modifier.height(16.dp))
            PreviewCard(state)
            Spacer(Modifier.height(16.dp))
            AppearanceSection(state, vm)
            Spacer(Modifier.height(8.dp))
            LayoutSection(state, vm)
            Spacer(Modifier.height(8.dp))
            TypingSection(state, vm)
            Spacer(Modifier.height(8.dp))
            FeedbackSection(state, vm)
            Spacer(Modifier.height(8.dp))
            ErgonomicsSection(state, vm)
            Spacer(Modifier.height(8.dp))
            AdvancedSection(state, vm)
            Spacer(Modifier.height(8.dp))
            AboutSection()
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Composable
private fun OnboardingCard(enabled: Boolean, default: Boolean, onRefresh: () -> Unit) {
    val ctx = LocalContext.current
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Setup", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            StepRow(
                done = enabled,
                title = "1. Enable in system settings",
                action = "Enable",
            ) {
                ImeUtils.openImeSettings(ctx); onRefresh()
            }
            StepRow(
                done = default,
                title = "2. Choose Zavtra as your default keyboard",
                action = "Choose",
            ) {
                ImeUtils.showImePicker(ctx); onRefresh()
            }
            StepRow(
                done = enabled && default,
                title = "3. Done — type in any app",
                action = null,
            ) {}
        }
    }
}

@Composable
private fun StepRow(done: Boolean, title: String, action: String?, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(
            imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(8.dp))
        Text(title, modifier = Modifier.weight(1f))
        if (action != null) {
            FilledTonalButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun PreviewCard(s: com.zavtrak.keyboard.data.AppSettings) {
    var text by remember { mutableStateOf("") }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Try it here", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type to test the keyboard…") },
                minLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tip: long-press letters for accents, swipe space to move the cursor, swipe ⌫ left to delete words.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ElevatedCardSection(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp), content = content)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String? = null, onChange: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row {
            Text(label, modifier = Modifier.weight(1f))
            Text(valueText ?: "%.0f".format(value), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T : Enum<T>> EnumRow(label: String, value: T, options: List<Pair<T, String>>, onChange: (T) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label)
        Spacer(Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (opt, lbl) ->
                FilterChip(
                    selected = opt == value,
                    onClick = { onChange(opt) },
                    label = { Text(lbl) },
                )
            }
        }
    }
}

@Composable
private fun AppearanceSection(s: com.zavtrak.keyboard.data.AppSettings, vm: SettingsViewModel) {
    SectionHeader("Appearance")
    ElevatedCardSection {
        EnumRow(
            "Theme", s.themeMode,
            listOf(
                com.zavtrak.keyboard.data.ThemeMode.System to "System",
                com.zavtrak.keyboard.data.ThemeMode.Light to "Light",
                com.zavtrak.keyboard.data.ThemeMode.Dark to "Dark",
                com.zavtrak.keyboard.data.ThemeMode.Amoled to "AMOLED",
            ),
        ) { v -> vm.update { it.copy(themeMode = v) } }

        EnumRow(
            "Preset", s.preset,
            listOf(
                com.zavtrak.keyboard.data.ThemePreset.Default to "Default",
                com.zavtrak.keyboard.data.ThemePreset.Neon to "Neon",
                com.zavtrak.keyboard.data.ThemePreset.Glass to "Glass",
                com.zavtrak.keyboard.data.ThemePreset.Sunset to "Sunset",
                com.zavtrak.keyboard.data.ThemePreset.Mono to "Mono",
                com.zavtrak.keyboard.data.ThemePreset.Pastel to "Pastel",
                com.zavtrak.keyboard.data.ThemePreset.Forest to "Forest",
                com.zavtrak.keyboard.data.ThemePreset.Ocean to "Ocean",
            ),
        ) { v -> vm.update { it.copy(preset = v) } }

        SwitchRow("Material You (dynamic colors, Android 12+)", s.materialYou) { v -> vm.update { it.copy(materialYou = v) } }
        AccentRow(s.accentColor) { c -> vm.update { it.copy(accentColor = c) } }

        SliderRow("Background opacity", s.backgroundOpacity, 0.6f..1f, valueText = "%.2f".format(s.backgroundOpacity)) { v ->
            vm.update { it.copy(backgroundOpacity = v) }
        }
        SliderRow("Key corner radius", s.keyCornerRadius, 0f..28f) { v ->
            vm.update { it.copy(keyCornerRadius = v) }
        }
        SliderRow("Key height (dp)", s.keyHeight, 40f..80f) { v ->
            vm.update { it.copy(keyHeight = v) }
        }
        SliderRow("Font size (sp)", s.fontSize, 12f..28f) { v ->
            vm.update { it.copy(fontSize = v) }
        }
        SwitchRow("Show key borders", s.showKeyBorders) { vm.update { it.copy(showKeyBorders = !s.showKeyBorders) } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentRow(current: Long, onPick: (Long) -> Unit) {
    val swatches: List<Long> = listOf(
        0xFF7C5CFFL, 0xFF22D3EEL, 0xFFF472B6L, 0xFFFF7A5CL,
        0xFF6FCF97L, 0xFF40A6F0L, 0xFFFFC857L, 0xFFE94F64L,
        0xFFB388FFL, 0xFF00C2A8L, 0xFFFFFFFFL, 0xFF000000L,
    )
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Accent color")
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            swatches.forEach { c ->
                val borderColor = if (c == current) MaterialTheme.colorScheme.onSurface else Color.Transparent
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(c))
                        .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(8.dp))
                        .clickable { onPick(c) }
                )
            }
        }
    }
}

@Composable
private fun LayoutSection(s: com.zavtrak.keyboard.data.AppSettings, vm: SettingsViewModel) {
    SectionHeader("Layout & Languages")
    ElevatedCardSection {
        SwitchRow("English (QWERTY)", s.enableEnglish) { vm.update { it.copy(enableEnglish = !s.enableEnglish) } }
        SwitchRow("Russian (ЙЦУКЕН)", s.enableRussian) { vm.update { it.copy(enableRussian = !s.enableRussian) } }
        SwitchRow("Show number row", s.showNumberRow) { vm.update { it.copy(showNumberRow = !s.showNumberRow) } }
        SwitchRow("Show arrow / cursor row", s.showArrowsRow) { vm.update { it.copy(showArrowsRow = !s.showArrowsRow) } }
        SwitchRow("Long-press top row for numbers", s.longPressNumbers) { vm.update { it.copy(longPressNumbers = !s.longPressNumbers) } }
    }
}

@Composable
private fun TypingSection(s: com.zavtrak.keyboard.data.AppSettings, vm: SettingsViewModel) {
    SectionHeader("Typing")
    ElevatedCardSection {
        SwitchRow("Auto-capitalize sentences", s.autoCapitalize) { vm.update { it.copy(autoCapitalize = !s.autoCapitalize) } }
        SwitchRow("Smart punctuation (double-space → \". \")", s.smartPunctuation) { vm.update { it.copy(smartPunctuation = !s.smartPunctuation) } }
        SwitchRow("Autocorrect", s.autocorrect) { vm.update { it.copy(autocorrect = !s.autocorrect) } }
        SwitchRow("Word suggestions", s.showSuggestions) { vm.update { it.copy(showSuggestions = !s.showSuggestions) } }
        SwitchRow("Learn new words", s.learnWords) { vm.update { it.copy(learnWords = !s.learnWords) } }
        SwitchRow("Incognito mode (no learning, no clipboard history)", s.incognitoMode) { vm.update { it.copy(incognitoMode = !s.incognitoMode) } }
        SwitchRow("Swipe space bar to move cursor", s.swipeSpaceCursor) { vm.update { it.copy(swipeSpaceCursor = !s.swipeSpaceCursor) } }
        SwitchRow("Swipe backspace to delete by word", s.swipeBackspaceWord) { vm.update { it.copy(swipeBackspaceWord = !s.swipeBackspaceWord) } }
    }
}

@Composable
private fun FeedbackSection(s: com.zavtrak.keyboard.data.AppSettings, vm: SettingsViewModel) {
    SectionHeader("Feedback")
    ElevatedCardSection {
        SwitchRow("Haptic feedback", s.haptic) { vm.update { it.copy(haptic = !s.haptic) } }
        EnumRow(
            "Haptic strength", s.hapticStrength,
            listOf(
                com.zavtrak.keyboard.data.HapticStrength.Off to "Off",
                com.zavtrak.keyboard.data.HapticStrength.Light to "Light",
                com.zavtrak.keyboard.data.HapticStrength.Medium to "Medium",
                com.zavtrak.keyboard.data.HapticStrength.Strong to "Strong",
            ),
        ) { v -> vm.update { it.copy(hapticStrength = v) } }
        SwitchRow("Key sounds", s.sound) { vm.update { it.copy(sound = !s.sound) } }
        SliderRow("Sound volume", s.soundVolume, 0f..1f, valueText = "%.0f%%".format(s.soundVolume * 100f)) { v ->
            vm.update { it.copy(soundVolume = v) }
        }
    }
}

@Composable
private fun ErgonomicsSection(s: com.zavtrak.keyboard.data.AppSettings, vm: SettingsViewModel) {
    SectionHeader("Ergonomics")
    ElevatedCardSection {
        EnumRow(
            "One-handed mode", s.oneHanded,
            listOf(
                com.zavtrak.keyboard.data.OneHandedMode.Off to "Off",
                com.zavtrak.keyboard.data.OneHandedMode.Left to "Left",
                com.zavtrak.keyboard.data.OneHandedMode.Right to "Right",
            ),
        ) { v -> vm.update { it.copy(oneHanded = v) } }
        SwitchRow("Split keyboard (landscape / tablets)", s.splitKeyboard) { vm.update { it.copy(splitKeyboard = !s.splitKeyboard) } }
        SwitchRow("Floating keyboard", s.floatingKeyboard) { vm.update { it.copy(floatingKeyboard = !s.floatingKeyboard) } }
    }
}

@Composable
private fun AdvancedSection(s: com.zavtrak.keyboard.data.AppSettings, vm: SettingsViewModel) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val text = BufferedReader(InputStreamReader(input)).readText()
                vm.importJson(text)
            }
        }
    }

    SectionHeader("Advanced")
    ElevatedCardSection {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                showExport = true
            }) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("Export") }
            FilledTonalButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }) {
                Icon(Icons.Default.FileOpen, null); Spacer(Modifier.width(6.dp)); Text("Import")
            }
            OutlinedButton(onClick = { vm.reset() }) { Icon(Icons.Default.Restore, null); Spacer(Modifier.width(6.dp)); Text("Reset") }
        }
    }

    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("Settings JSON") },
            text = {
                val json = remember { vm.export() }
                OutlinedTextField(value = json, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(), maxLines = 12)
            },
            confirmButton = { TextButton(onClick = { showExport = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun AboutSection() {
    SectionHeader("About")
    ElevatedCardSection {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Zavtra Keyboard 1.0.0", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "A privacy-friendly, fully-customizable Android keyboard. All data stays on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
