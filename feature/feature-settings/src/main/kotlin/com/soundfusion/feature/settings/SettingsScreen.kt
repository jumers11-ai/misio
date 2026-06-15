package com.soundfusion.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Text(
                "Ustawienia",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Audio section
        item { SettingsHeader("Audio") }
        item { SettingsItem(icon = Icons.Default.Equalizer, title = "Equalizer", subtitle = "10-pasmowy EQ z presetami") }
        item { SettingsToggle(icon = Icons.Default.Tune, title = "Gapless Playback", subtitle = "Bez przerwy między utworami", default = true) }
        item { SettingsToggle(icon = Icons.Default.VolumeUp, title = "Replay Gain", subtitle = "Normalizacja głośności", default = true) }
        item { SettingsItem(icon = Icons.Default.Speed, title = "Crossfade", subtitle = "6 sekund") }

        // Appearance
        item { SettingsHeader("Wygląd") }
        item { SettingsItem(icon = Icons.Default.Palette, title = "Motyw", subtitle = "Material You (dynamiczny)") }
        item { SettingsToggle(icon = Icons.Default.DarkMode, title = "Dark Mode", subtitle = "Ciemny motyw", default = true) }

        // Accounts
        item { SettingsHeader("Konta") }
        item { SettingsItem(icon = Icons.Default.Person, title = "YouTube Music", subtitle = "Połączono") }
        item { SettingsItem(icon = Icons.Default.Person, title = "Spotify", subtitle = "Połączono") }
        item { SettingsItem(icon = Icons.Default.Person, title = "Last.fm", subtitle = "Połączono") }

        // Storage
        item { SettingsHeader("Pamięć") }
        item { SettingsItem(icon = Icons.Default.Storage, title = "Cache", subtitle = "512 MB / 5.0 GB użyte") }
        item { SettingsItem(icon = Icons.Default.Delete, title = "Wyczyść cache", subtitle = "Zwolnij miejsce") }

        // About
        item { SettingsHeader("O aplikacji") }
        item { SettingsItem(icon = Icons.Default.Info, title = "Wersja", subtitle = "1.0.0 (build 1)") }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
    )
}

@Composable
private fun SettingsToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, default: Boolean) {
    var checked by remember { mutableStateOf(default) }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = { checked = it }) },
    )
}
