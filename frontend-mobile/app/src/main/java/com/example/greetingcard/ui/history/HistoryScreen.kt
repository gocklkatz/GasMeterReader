package com.example.greetingcard.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.greetingcard.BuildConfig
import com.example.greetingcard.data.api.ReadingDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("History", style = MaterialTheme.typography.headlineSmall)
        }

        when (val s = state) {
            is ReadingsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ReadingsState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = viewModel::loadReadings) { Text("Retry") }
                    }
                }
            }
            is ReadingsState.Success -> {
                if (s.readings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No readings yet", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn {
                        items(s.readings) { reading ->
                            ReadingItem(reading)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingItem(reading: ReadingDto) {
    val imageUrl = "${BuildConfig.BACKEND_URL}images/${reading.imagePath}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Meter reading",
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = formatTimestamp(reading.timestamp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val datePart = timestamp.take(19)
        val inputFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        inputFmt.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFmt.parse(datePart) ?: return timestamp
        SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        timestamp
    }
}
