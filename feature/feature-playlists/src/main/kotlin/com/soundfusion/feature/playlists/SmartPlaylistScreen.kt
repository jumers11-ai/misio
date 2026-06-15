package com.soundfusion.feature.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.soundfusion.core.database.model.MusicSource
import com.soundfusion.core.database.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPlaylistScreen(
    onNavigateBack: () -> Unit,
    onPlayTrack: (String) -> Unit,
    viewModel: SmartPlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRuleBuilder by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Smart Playlist", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.matchedTracks.size} utworów",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.playAll() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play all")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showRuleBuilder = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add rule")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Active rules
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Aktywne reguły", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.activeRules.forEach { rule ->
                            AssistChip(
                                onClick = { viewModel.removeRule(rule) },
                                label = { Text(ruleLabel(rule), style = MaterialTheme.typography.labelSmall) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) },
                            )
                        }
                        if (state.activeRules.isEmpty()) {
                            Text("Brak reguł — pokaże wszystkie utwory", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Presets
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Presety", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.presetNames.take(4).forEach { preset ->
                            FilterChip(
                                selected = state.activePreset == preset,
                                onClick = { viewModel.applyPreset(preset) },
                                label = { Text(preset) },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp)) },
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Matched tracks
            items(state.matchedTracks, key = { it.id }) { track ->
                ListItem(
                    headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(track.artist, maxLines = 1) },
                    leadingContent = {
                        AsyncImage(
                            model = track.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                        )
                    },
                    trailingContent = {
                        Text(formatMs(track.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier.clickable { onPlayTrack(track.id) }.padding(horizontal = 8.dp),
                )
            }
        }

        if (showRuleBuilder) {
            RuleBuilderSheet(
                onDismiss = { showRuleBuilder = false },
                onAddRule = { rule -> viewModel.addRule(rule); showRuleBuilder = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleBuilderSheet(
    onDismiss: () -> Unit,
    onAddRule: (SmartRule) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text("Dodaj regułę", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            val rules = listOf(
                "Ulubione" to SmartRule.LikedRule(),
                "Nigdy nie grane" to SmartRule.NeverPlayedRule(),
                "Ostatnio dodane (14 dni)" to SmartRule.RecentlyAddedRule(14),
                "Ostatnio grane (7 dni)" to SmartRule.RecentlyPlayedRule(7),
                "Tylko offline" to SmartRule.OfflineOnlyRule(),
                "Krótkie (<3 min)" to SmartRule.DurationRule(maxMs = 180_000),
                "Długie (>5 min)" to SmartRule.DurationRule(minMs = 300_000),
                "Grane >5 razy" to SmartRule.PlayCountRule(minPlays = 5),
                "YouTube" to SmartRule.SourceRule(MusicSource.YOUTUBE),
                "Spotify" to SmartRule.SourceRule(MusicSource.SPOTIFY),
                "Lokalne" to SmartRule.SourceRule(MusicSource.LOCAL),
            )

            rules.forEach { (label, rule) ->
                Card(
                    onClick = { onAddRule(rule) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun ruleLabel(rule: SmartRule): String = when (rule) {
    is SmartRule.SourceRule -> "Source: ${rule.source.name}"
    is SmartRule.LikedRule -> "Ulubione"
    is SmartRule.PlayCountRule -> "Plays: ${rule.minPlays}-${rule.maxPlays}"
    is SmartRule.RecentlyAddedRule -> "Added <${rule.withinDays}d"
    is SmartRule.RecentlyPlayedRule -> "Played <${rule.withinDays}d"
    is SmartRule.NeverPlayedRule -> "Never played"
    is SmartRule.OfflineOnlyRule -> "Offline"
    is SmartRule.DurationRule -> "Duration filter"
    is SmartRule.ArtistContainsRule -> "Artist: ${rule.query}"
    is SmartRule.TitleContainsRule -> "Title: ${rule.query}"
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
