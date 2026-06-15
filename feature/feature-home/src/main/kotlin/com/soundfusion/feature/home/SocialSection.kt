package com.soundfusion.feature.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soundfusion.core.database.model.Track

data class SharedPlaylist(
    val id: String,
    val name: String,
    val owner: String,
    val trackCount: Int,
    val artworkUrl: String?,
    val shareUrl: String,
)

@Composable
fun SocialSection(
    friendActivity: List<FriendActivity> = emptyList(),
    sharedPlaylists: List<SharedPlaylist> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Friends listening
        if (friendActivity.isNotEmpty()) {
            Text(
                "Znajomi słuchają",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(friendActivity, key = { it.id }) { activity ->
                    FriendActivityCard(activity)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Shared playlists
        if (sharedPlaylists.isNotEmpty()) {
            Text(
                "Udostępnione playlisty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            sharedPlaylists.forEach { playlist ->
                SharedPlaylistItem(playlist)
            }
        }
    }
}

data class FriendActivity(
    val id: String,
    val friendName: String,
    val friendAvatar: String?,
    val trackTitle: String,
    val trackArtist: String,
    val trackArtwork: String?,
    val timestamp: String,
)

@Composable
private fun FriendActivityCard(activity: FriendActivity) {
    Card(modifier = Modifier.width(200.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = activity.friendAvatar,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(MaterialTheme.shapes.extraLarge),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(8.dp))
                Text(activity.friendName, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            Spacer(Modifier.height(8.dp))
            Row {
                AsyncImage(
                    model = activity.trackArtwork,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(activity.trackTitle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(activity.trackArtist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(activity.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SharedPlaylistItem(playlist: SharedPlaylist) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = playlist.artworkUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("${playlist.owner} • ${playlist.trackCount} utworów", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { clipboard.setText(AnnotatedString(playlist.shareUrl)) }) {
                Icon(Icons.Default.ContentCopy, "Copy link", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Posłuchaj: ${playlist.name}\n${playlist.shareUrl}")
                }
                context.startActivity(Intent.createChooser(intent, "Udostępnij"))
            }) {
                Icon(Icons.Default.Share, "Share", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ShareTrackButton(track: Track, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val shareText = "🎵 ${track.title} — ${track.artist}\nsoundfusion://track/${track.id}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(intent, "Udostępnij utwór"))
        },
        modifier = modifier,
    ) {
        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Udostępnij")
    }
}
