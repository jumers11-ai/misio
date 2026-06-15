package com.soundfusion.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun LibraryScreen(
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val likedTracks by viewModel.likedCount.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Biblioteka",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Playlisty") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Albumy") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Artyści") })
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Liked tracks shortcut
            item {
                ListItem(
                    headlineContent = { Text("Ulubione") },
                    supportingContent = { Text("$likedTracks utworów") },
                    leadingContent = { Text("❤️", style = MaterialTheme.typography.headlineMedium) },
                    modifier = Modifier.clickable { },
                )
            }

            items(playlists, key = { it.id }) { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.name, maxLines = 1) },
                    supportingContent = { Text("${playlist.trackCount} utworów • ${playlist.source.name}") },
                    leadingContent = {
                        AsyncImage(
                            model = playlist.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium),
                        )
                    },
                    modifier = Modifier.clickable { onNavigateToPlaylist(playlist.id) },
                )
            }
        }
    }
}
