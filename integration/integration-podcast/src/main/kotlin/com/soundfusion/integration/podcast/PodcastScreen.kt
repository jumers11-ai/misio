package com.soundfusion.integration.podcast

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class PodcastShow(
    val id: String,
    val title: String,
    val author: String,
    val artworkUrl: String?,
    val episodeCount: Int,
    val feedUrl: String,
)

data class PodcastEpisodeUi(
    val id: String,
    val title: String,
    val showTitle: String,
    val duration: String,
    val date: String,
    val artworkUrl: String?,
    val isDownloaded: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    subscriptions: List<PodcastShow> = emptyList(),
    episodes: List<PodcastEpisodeUi> = emptyList(),
    onPlayEpisode: (String) -> Unit = {},
    onSubscribe: (String) -> Unit = {},
    onDownload: (String) -> Unit = {},
) {
    var feedUrl by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Icon(Icons.Default.Podcasts, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Podcasty")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Dodaj podcast")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Subscriptions
            if (subscriptions.isNotEmpty()) {
                item {
                    Text(
                        "Subskrypcje",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(subscriptions, key = { it.id }) { show ->
                            Card(onClick = { }, modifier = Modifier.width(140.dp)) {
                                Column {
                                    AsyncImage(
                                        model = show.artworkUrl,
                                        contentDescription = show.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(140.dp),
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(show.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("${show.episodeCount} odcinków", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Latest episodes
            item {
                Text(
                    "Najnowsze odcinki",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(16.dp),
                )
            }

            if (episodes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Default.Podcasts, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(16.dp))
                        Text("Brak podcastów", style = MaterialTheme.typography.bodyLarge)
                        Text("Dodaj RSS feed aby zacząć", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(episodes, key = { it.id }) { episode ->
                ListItem(
                    headlineContent = { Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text("${episode.showTitle} • ${episode.duration} • ${episode.date}")
                    },
                    leadingContent = {
                        AsyncImage(
                            model = episode.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
                        )
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onDownload(episode.id) }) {
                                Icon(
                                    Icons.Default.Download,
                                    "Download",
                                    tint = if (episode.isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { onPlayEpisode(episode.id) }) {
                                Icon(Icons.Default.PlayArrow, "Play")
                            }
                        }
                    },
                    modifier = Modifier.clickable { onPlayEpisode(episode.id) }.padding(horizontal = 8.dp),
                )
            }

            // Add feed
            if (showAddDialog) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Dodaj podcast", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = feedUrl,
                                onValueChange = { feedUrl = it },
                                label = { Text("RSS Feed URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                androidx.compose.material3.TextButton(onClick = { showAddDialog = false }) { Text("Anuluj") }
                                Spacer(Modifier.width(8.dp))
                                androidx.compose.material3.Button(
                                    onClick = { onSubscribe(feedUrl); feedUrl = ""; showAddDialog = false },
                                    enabled = feedUrl.isNotBlank(),
                                ) { Text("Dodaj") }
                            }
                        }
                    }
                }
            }
        }
    }
}
