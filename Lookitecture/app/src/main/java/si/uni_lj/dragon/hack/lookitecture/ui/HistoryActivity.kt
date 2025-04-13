package si.uni_lj.dragon.hack.lookitecture.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import si.uni_lj.dragon.hack.lookitecture.util.HistoryLandmarkData
import si.uni_lj.dragon.hack.lookitecture.util.LandmarkHistoryManager
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HistoryScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val history = remember { mutableStateListOf<HistoryLandmarkData>() }

    var refreshKey by remember { mutableStateOf(0) }
    // Show confirmation dialog for clearing history
    var showClearDialog by remember { mutableStateOf(false) }

    // Load history when the screen opens or when refreshed
    LaunchedEffect(refreshKey) {
        try {
            history.clear()
            val items = LandmarkHistoryManager.getHistory(context)
            Log.d("HistoryScreen", "Loaded ${items.size} history items")
            history.addAll(items)
        } catch (e: Exception) {
            Log.e("HistoryScreen", "Error loading history", e)
        }
    }

    // Use DisposableEffect to refresh the list when the composable is recomposed
    DisposableEffect(Unit) {
        refreshKey++
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viewing History") },
                navigationIcon = {
                    IconButton(onClick = { context.startActivity(Intent(context, MainActivity::class.java)) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (history.isEmpty()) {
                // Show empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No history yet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Landmarks you view will appear here",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            } else {
                // Show history list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = history,
                        key = { it.id }  // Use the unique ID as key for better list performance
                    ) { landmark ->
                        HistoryCard(landmark) {
                            val intent = Intent(context, LandmarkDetailsActivity::class.java).apply {
                                putExtra("IMAGE_URI", landmark.imageUri)
                                putExtra("FROM_HISTORY", true)
                                putExtra("LANDMARK_NAME", landmark.name)
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }

            // Clear history confirmation dialog
            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Clear History") },
                    text = { Text("Are you sure you want to clear all history?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                LandmarkHistoryManager.clearHistory(context)
                                history.clear()
                                showClearDialog = false
                            }
                        ) {
                            Text("Clear")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryCard(landmark: HistoryLandmarkData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Image thumbnail
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                landmark.imageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(Uri.parse(it)),
                        contentDescription = landmark.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    // Use Surface instead of Box with background for Material3 compatibility
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.LightGray
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("No Image")
                        }
                    }
                }
            }

            // Landmark details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = landmark.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = landmark.location,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Style: ${landmark.architectureStyle}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Format and display the timestamp in a more readable way
                Text(
                    text = "Viewed: ${formatTimestamp(landmark.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Display formatted date and time in a more user-friendly way
                Text(
                    text = "Date: ${formatDate(landmark.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                // Display time separately
                Text(
                    text = "Time: ${formatTime(landmark.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Formats a timestamp string into a more readable format
 */
private fun formatTimestamp(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(timestamp) ?: return timestamp

        val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        timestamp
    }
}

/**
 * Formats just the date portion of a timestamp
 */
private fun formatDate(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(timestamp) ?: return timestamp

        val outputFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        timestamp
    }
}

/**
 * Formats just the time portion of a timestamp
 */
private fun formatTime(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(timestamp) ?: return timestamp

        val outputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        timestamp
    }
}
