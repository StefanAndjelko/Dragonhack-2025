package si.uni_lj.dragon.hack.lookitecture.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import si.uni_lj.dragon.hack.lookitecture.services.LandmarkApiService
import si.uni_lj.dragon.hack.lookitecture.util.HistoryLandmarkData
import si.uni_lj.dragon.hack.lookitecture.util.LandmarkHistoryManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

// Define our brand color
val LookitectureGreen = Color(0xfffa7850)

class LandmarkDetailsActivity : ComponentActivity() {
    
    // Track bookmarked landmarks
    companion object {
        private val bookmarkedLandmarks = mutableSetOf<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the image URI and other data from the intent
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val imageUri = imageUriString?.let { Uri.parse(it) }

        // Check if coming from history or new capture
        val fromHistory = intent.getBooleanExtra("FROM_HISTORY", false)

        // Get the landmark name from the intent - no longer use a hardcoded default
        val landmarkName = intent.getStringExtra("LANDMARK_NAME") ?: ""

        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = LookitectureGreen,
                    secondary = LookitectureGreen.copy(alpha = 0.7f),
                    tertiary = LookitectureGreen.copy(alpha = 0.5f)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Using state to hold landmark data while loading
                    var landmarkData by remember { mutableStateOf<LandmarkData?>(null) }
                    var isLoading by remember { mutableStateOf(true) }
                    val coroutineScope = rememberCoroutineScope()

                    // Load data from history or API as appropriate
                    LaunchedEffect(landmarkName) {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                if (fromHistory) {
                                    // First try to get data from history
                                    val historyData = LandmarkHistoryManager.getLandmarkFromHistory(
                                        this@LandmarkDetailsActivity,
                                        landmarkName
                                    )

                                    if (historyData != null) {
                                        // Convert history data to landmark data
                                        landmarkData = LandmarkData(
                                            name = historyData.name,
                                            location = historyData.location,
                                            architectureStyle = historyData.architectureStyle,
                                            yearBuilt = historyData.yearBuilt,
                                            height = historyData.height,
                                            description = historyData.description,
                                            interestingFacts = historyData.interestingFacts,
                                            coordinates = Pair(0.0, 0.0) // Default coordinates
                                        )
                                        Log.d("LandmarkDetailsActivity", "Loaded from history: ${historyData.name}")
                                    } else {
                                        // Fall back to API if not found in history
                                        Log.d("LandmarkDetailsActivity", "Not found in history, fetching from API: $landmarkName")
                                        val apiData = LandmarkApiService.getLandmarkInfo(landmarkName)
                                        // API data already includes coordinates
                                        landmarkData = apiData
                                        Log.d("LandmarkDetailsActivity", "Got API data with coordinates: ${apiData.coordinates}")
                                    }
                                } else {
                                    // New capture - get landmark data from API
                                    Log.d("LandmarkDetailsActivity", "Fetching from API: $landmarkName")
                                    val apiData = LandmarkApiService.getLandmarkInfo(landmarkName)
                                    landmarkData = apiData
                                    Log.d("LandmarkDetailsActivity", "Got API data with coordinates: ${apiData.coordinates}")
                                }
                            } catch (e: Exception) {
                                Log.e("LandmarkDetailsActivity", "Error fetching landmark data", e)
                                // Fallback to basic data if API fails
                                landmarkData = LandmarkData(
                                    name = landmarkName,
                                    location = "Information unavailable",
                                    architectureStyle = "Information unavailable",
                                    yearBuilt = "Information unavailable",
                                    height = "Information unavailable",
                                    description = "Unable to load landmark information. Please check your internet connection.",
                                    interestingFacts = listOf("Information unavailable"),
                                    coordinates = Pair(0.0, 0.0)
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    }

                    if (isLoading) {
                        LoadingAnimation(modifier = Modifier.fillMaxSize())
                    } else {
                        landmarkData?.let { data ->
                            LandmarkDetailsScreen(
                                imageUri = imageUri,
                                landmarkData = data,
                                fromHistory = fromHistory,
                                isBookmarked = bookmarkedLandmarks.contains(data.name),
                                onToggleBookmark = { name, isBookmarked ->
                                    if (isBookmarked) {
                                        bookmarkedLandmarks.add(name)
                                        Toast.makeText(this@LandmarkDetailsActivity, "Landmark bookmarked", Toast.LENGTH_SHORT).show()
                                    } else {
                                        bookmarkedLandmarks.remove(name)
                                        Toast.makeText(this@LandmarkDetailsActivity, "Bookmark removed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onShareLandmark = { name, description ->
                                    shareLandmarkInfo(name, description)
                                },
                                onSaveToHistory = {
                                    // Save to history when "Okay" is clicked
                                    if (imageUriString != null) {
                                        saveLandmarkToHistory(data, imageUriString)
                                        Toast.makeText(this@LandmarkDetailsActivity, "Added to history", Toast.LENGTH_SHORT).show()
                                    }
                                    finish()
                                },
                                onBack = { finish() },
                                onOpenMap = { lat, lng, name ->
                                    openMap(lat, lng, name)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openMap(latitude: Double, longitude: Double, landmarkName: String) {
        try {
            Log.d("LandmarkDetailsActivity", "Opening map with coordinates: lat=$latitude, lng=$longitude")
            
            // Use URI encoding for the landmark name to handle special characters
            val encodedName = java.net.URLEncoder.encode(landmarkName, "UTF-8")
            
            // Check if we have valid coordinates (not both 0,0)
            val useCoordinates = latitude != 0.0 || longitude != 0.0
            
            // If we don't have coordinates, just search by name
            val gmmIntentUri = if (useCoordinates) {
                Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedName)")
            } else {
                // Fallback to searching by name if no coordinates
                Uri.parse("geo:0,0?q=$encodedName")
            }
            
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            // Check if Google Maps is installed
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Fallback to browser if Google Maps isn't installed
                val browserUri = if (useCoordinates) {
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                } else {
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedName")
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e("LandmarkDetailsActivity", "Error opening map", e)
            Toast.makeText(this, "Could not open map application", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareLandmarkInfo(name: String, description: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this landmark: $name")
            putExtra(Intent.EXTRA_TEXT, "I discovered $name using Lookitecture!\n\n$description")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun saveLandmarkToHistory(landmarkData: LandmarkData, imageUriString: String) {
        try {
            // Copy image to internal storage for persistence
            val persistentImageUri = saveImageToInternalStorage(Uri.parse(imageUriString))

            val historyData = HistoryLandmarkData(
                name = landmarkData.name,
                imageUri = persistentImageUri,
                location = landmarkData.location,
                architectureStyle = landmarkData.architectureStyle,
                yearBuilt = landmarkData.yearBuilt,
                height = landmarkData.height,
                description = landmarkData.description,
                interestingFacts = landmarkData.interestingFacts
            )

            // Add to history and show a log for debugging
            Log.d("LandmarkDetailsActivity", "Saving to history: ${historyData.name}")
            LandmarkHistoryManager.addToHistory(this, historyData)

            // Check if history was saved correctly
            val historySize = LandmarkHistoryManager.getHistorySize(this)
            Log.d("LandmarkDetailsActivity", "History size after saving: $historySize")

        } catch (e: Exception) {
            Log.e("LandmarkDetailsActivity", "Error saving to history", e)
            Toast.makeText(this, "Error saving to history", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves an image from the given Uri to the app's internal storage
     * @return String path to the saved image that will persist
     */
    private fun saveImageToInternalStorage(uri: Uri): String {
        try {
            // Create a unique filename
            val filename = "landmark_${UUID.randomUUID()}.jpg"
            val file = File(filesDir, filename)

            // Copy the image to internal storage
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            // Return the file path as a string
            Log.d("LandmarkDetailsActivity", "Image saved to: ${file.absolutePath}")
            return file.absolutePath

        } catch (e: IOException) {
            Log.e("LandmarkDetailsActivity", "Failed to save image", e)
            return uri.toString() // Fallback to original URI if saving fails
        }
    }
}

@Composable
fun LoadingAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "rotate"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier
                .scale(scale)
                .size(80.dp),
            color = LookitectureGreen,
            strokeWidth = 8.dp
        )

        Text(
            text = "Loading landmark info...",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(top = 100.dp)
                .animateContentSize(),
            color = LookitectureGreen
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandmarkDetailsScreen(
    imageUri: Uri?,
    landmarkData: LandmarkData,
    fromHistory: Boolean,
    isBookmarked: Boolean,
    onToggleBookmark: (String, Boolean) -> Unit,
    onShareLandmark: (String, String) -> Unit,
    onSaveToHistory: () -> Unit,
    onBack: () -> Unit,
    onOpenMap: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    var currentIsBookmarked by remember { mutableStateOf(isBookmarked) }
    var showMapPreview by remember { mutableStateOf(false) }

    val imageScale = remember { Animatable(0.8f) }

    LaunchedEffect(true) {
        imageScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        landmarkData.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Bookmark button with animation
                    IconButton(onClick = {
                        currentIsBookmarked = !currentIsBookmarked
                        onToggleBookmark(landmarkData.name, currentIsBookmarked)
                    }) {
                        Icon(
                            imageVector = if (currentIsBookmarked)
                                            Icons.Filled.Bookmark
                                         else
                                            Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = Color.White,
                            modifier = Modifier
                                .scale(if (currentIsBookmarked) 1.2f else 1.0f)
                                .animateContentSize()
                        )
                    }

                    // Share button
                    IconButton(onClick = {
                        onShareLandmark(landmarkData.name, landmarkData.description)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LookitectureGreen
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display the image with animation and gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                imageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = landmarkData.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(imageScale.value),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay for better text visibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.3f)
                                    ),
                                    startY = 0f,
                                    endY = 500f
                                )
                            )
                    )

                    // Location badge at the bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(LookitectureGreen.copy(alpha = 0.8f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = "Location",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = landmarkData.location,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Content area with enhanced styling
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Key information card with improved styling
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LANDMARK DETAILS",
                            style = MaterialTheme.typography.labelLarge,
                            color = LookitectureGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Divider(color = LookitectureGreen.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow(
                            icon = Icons.Filled.Architecture,
                            label = "Architecture Style",
                            value = landmarkData.architectureStyle
                        )
                        InfoRow(
                            icon = Icons.Filled.CalendarToday,
                            label = "Year Built",
                            value = landmarkData.yearBuilt
                        )
                        InfoRow(
                            icon = Icons.Filled.Height,
                            label = "Height",
                            value = landmarkData.height
                        )

                        // Always show map button - we'll open map even with default coordinates
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onOpenMap(
                                    landmarkData.coordinates.first,
                                    landmarkData.coordinates.second,
                                    landmarkData.name
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LookitectureGreen
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = "Map",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                "View on Map",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description section with improved styling
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ABOUT",
                            style = MaterialTheme.typography.labelLarge,
                            color = LookitectureGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Divider(color = LookitectureGreen.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = landmarkData.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interesting facts with improved styling
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "INTERESTING FACTS",
                            style = MaterialTheme.typography.labelLarge,
                            color = LookitectureGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Divider(color = LookitectureGreen.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        landmarkData.interestingFacts.forEachIndexed { index, fact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(LookitectureGreen)
                                        .align(Alignment.Top),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = fact,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            if (index < landmarkData.interestingFacts.size - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                // Action button - Enhanced styling
                if (!fromHistory) {
                    Button(
                        onClick = onSaveToHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LookitectureGreen
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "Save to History",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LookitectureGreen
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            "Back to History",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = LookitectureGreen,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = LookitectureGreen,
                fontWeight = FontWeight.Medium
            )
            Text(
                // Make sure we never display "null" as text
                text = when {
                    value.isNullOrBlank() -> "Information unavailable"
                    value.equals("null", ignoreCase = true) -> "Information unavailable"
                    else -> value
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

data class LandmarkData(
    val name: String,
    val location: String,
    val architectureStyle: String,
    val yearBuilt: String,
    val height: String,
    val description: String,
    val interestingFacts: List<String>,
    val coordinates: Pair<Double, Double> = Pair(0.0, 0.0)
)

