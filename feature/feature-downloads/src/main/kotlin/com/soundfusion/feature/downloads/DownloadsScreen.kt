package com.soundfusion.feature.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundfusion.core.database.entity.DownloadStatus

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle(emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Pobrane",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (downloads.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("Brak pobranych utworów", style = MaterialTheme.typography.bodyLarge)
                        Text("Pobierz utwory, aby słuchać offline", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        items(downloads, key = { it.id }) { download ->
            ElevatedCard {
                ListItem(
                    headlineContent = { Text(download.trackId, maxLines = 1) },
                    supportingContent = {
                        Column {
                            if (download.status == DownloadStatus.DOWNLOADING) {
                                LinearProgressIndicator(
                                    progress = { download.progress },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                )
                            }
                            Text("${download.status} • ${download.fileSize / 1024 / 1024} MB")
                        }
                    },
                    leadingContent = {
                        Icon(
                            when (download.status) {
                                DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                                DownloadStatus.DOWNLOADING -> Icons.Default.Downloading
                                DownloadStatus.PAUSED -> Icons.Default.PauseCircle
                                DownloadStatus.ERROR -> Icons.Default.Error
                                DownloadStatus.QUEUED -> Icons.Default.Schedule
                            },
                            contentDescription = null,
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeDownload(download.id) }) {
                            Icon(Icons.Default.Delete, "Remove")
                        }
                    },
                )
            }
        }
    }
}
