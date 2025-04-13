package si.uni_lj.dragon.hack.lookitecture.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import si.uni_lj.dragon.hack.lookitecture.util.HistoryLandmarkData
import si.uni_lj.dragon.hack.lookitecture.util.LandmarkHistoryManager
import java.text.SimpleDateFormat
import java.util.*

// Define the theme colors based on the provided color #459282
val PrimaryColor = Color(0xFF459282)
val LighterPrimaryColor = Color(0xFF5BA99A)
val DarkerPrimaryColor = Color(0xFF367A6C)
val BackgroundColor = Color(0xFFF9FCFB)
val CardBackgroundColor = Color.White
val SurfaceColor = Color(0xFFF5F9F8)
val TextPrimaryColor = Color(0xFF212121)
val TextSecondaryColor = Color(0xFF757575)
val DividerColor = Color(0xFFEEEEEE)
val ShadowColor = Color(0x1A000000)

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Custom theme with our primary color
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = PrimaryColor,
                    secondary = LighterPrimaryColor,
                    tertiary = DarkerPrimaryColor,
                    onPrimary = Color.White,
                    background = BackgroundColor,
                    surface = SurfaceColor,
                    surfaceVariant = Color(0xFFE1EBE8)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundColor
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BackgroundColor,
                            Color(0xFFEEF5F3)
                        )
                    )
                )
        )
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Viewing History",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { context.startActivity(Intent(context, MainActivity::class.java)) }) {
                            Icon(
                                Icons.Default.ArrowBack, 
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = "Clear History",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PrimaryColor
                    ),
                    modifier = Modifier.shadow(elevation = 4.dp)
                )
            },
            containerColor = Color.Transparent // Make scaffold background transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                if (history.isEmpty()) {
                    // Enhanced empty state with animation
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    color = PrimaryColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(60.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = PrimaryColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "No History Yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimaryColor,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Explore landmarks to see them appear here",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondaryColor,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                } else {
                    // Enhanced history list with smooth animations and spacing
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        itemsIndexed(
                            items = history,
                            key = { _, item -> item.id }
                        ) { index, landmark ->
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically(
                                    initialOffsetY = { 100 },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) + fadeIn(
                                    animationSpec = tween(300, delayMillis = index * 50)
                                )
                            ) {
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
                }

                // Enhanced clear history confirmation dialog
                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { 
                            Text(
                                "Clear History", 
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryColor
                            ) 
                        },
                        text = { 
                            Text(
                                "Are you sure you want to clear all history?",
                                color = TextSecondaryColor
                            ) 
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    LandmarkHistoryManager.clearHistory(context)
                                    history.clear()
                                    showClearDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryColor
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear All")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showClearDialog = false },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = PrimaryColor
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel")
                            }
                        },
                        containerColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(landmark: HistoryLandmarkData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = ShadowColor
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Image section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                // Image
                landmark.imageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(Uri.parse(it)),
                        contentDescription = landmark.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LighterPrimaryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No Image Available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkerPrimaryColor
                        )
                    }
                }

                // Gradient overlay at the bottom for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(80.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // Title overlay on the image
                Text(
                    text = landmark.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Details section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Location with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = landmark.location,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimaryColor
                    )
                }

                // Architecture style with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.Style,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PrimaryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = landmark.architectureStyle,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimaryColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = DividerColor)
                Spacer(modifier = Modifier.height(8.dp))

                // Date and time section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextSecondaryColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDate(landmark.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TextSecondaryColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatTime(landmark.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryColor
                        )
                    }
                }
            }
        }
    }
}

// ...existing code...
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

private fun formatDate(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(timestamp) ?: return timestamp

        val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        timestamp
    }
}

private fun formatTime(timestamp: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(timestamp) ?: return timestamp

        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        timestamp
    }
}

