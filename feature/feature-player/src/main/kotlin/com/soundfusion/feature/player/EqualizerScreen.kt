package com.soundfusion.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onNavigateBack: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel(),
) {
    val bands by viewModel.eqBands.collectAsStateWithLifecycle()
    val bassBoost by viewModel.bassBoostStrength.collectAsStateWithLifecycle()
    val eqEnabled by viewModel.eqEnabled.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = { viewModel.setEnabled(it) },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            // EQ bands
            Text(
                text = "Equalizer",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                bands.forEach { band ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp),
                    ) {
                        Text(
                            text = "${band.currentLevel}",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )

                        Slider(
                            value = band.currentLevel.toFloat(),
                            onValueChange = { viewModel.setBandLevel(band.index, it.toInt().toShort()) },
                            valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                            modifier = Modifier.weight(1f).width(36.dp),
                            enabled = eqEnabled,
                        )

                        Text(
                            text = if (band.frequencyHz >= 1000) "${band.frequencyHz / 1000}k" else "${band.frequencyHz}",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Bass Boost
            Text(
                text = "Bass Boost",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Slider(
                    value = bassBoost.toFloat(),
                    onValueChange = { viewModel.setBassBoost(it.toInt()) },
                    valueRange = 0f..1000f,
                    modifier = Modifier.weight(1f),
                    enabled = eqEnabled,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${(bassBoost / 10)}%",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
