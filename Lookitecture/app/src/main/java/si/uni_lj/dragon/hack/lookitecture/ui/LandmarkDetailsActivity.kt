package si.uni_lj.dragon.hack.lookitecture.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import si.uni_lj.dragon.hack.lookitecture.services.LandmarkApiService
import si.uni_lj.dragon.hack.lookitecture.util.HistoryLandmarkData
import si.uni_lj.dragon.hack.lookitecture.util.LandmarkHistoryManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class LandmarkDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the image URI and other data from the intent
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val imageUri = imageUriString?.let { Uri.parse(it) }

        // Check if coming from history or new capture
        val fromHistory = intent.getBooleanExtra("FROM_HISTORY", false)
        val landmarkName = intent.getStringExtra("LANDMARK_NAME") ?: "Great Wall of China"

        setContent {
            MaterialTheme {
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
                                            interestingFacts = historyData.interestingFacts
                                        )
                                        Log.d("LandmarkDetailsActivity", "Loaded from history: ${historyData.name}")
                                    } else {
                                        // Fall back to API if not found in history
                                        Log.d("LandmarkDetailsActivity", "Not found in history, fetching from API: $landmarkName")
                                        landmarkData = LandmarkApiService.getLandmarkInfo(landmarkName)
                                    }
                                } else {
                                    // New capture - get landmark data from API
                                    landmarkData = LandmarkApiService.getLandmarkInfo(landmarkName)
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
                                    interestingFacts = listOf("Information unavailable")
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        landmarkData?.let { data ->
                            LandmarkDetailsScreen(
                                imageUri = imageUri,
                                landmarkData = data,
                                fromHistory = fromHistory,
                                onSaveToHistory = {
                                    // Save to history when "Okay" is clicked
                                    if (imageUriString != null) {
                                        saveLandmarkToHistory(data, imageUriString)
                                        Toast.makeText(this@LandmarkDetailsActivity, "Added to history", Toast.LENGTH_SHORT).show()
                                    }
                                    finish()
                                },
                                onBack = { finish() }
                            )
                        }
                    }
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandmarkDetailsScreen(
    imageUri: Uri?,
    landmarkData: LandmarkData,
    fromHistory: Boolean,
    onSaveToHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(landmarkData.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
            // Display the image
            imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = landmarkData.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            // Content area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Key information card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("Location", landmarkData.location)
                        InfoRow("Architecture Style", landmarkData.architectureStyle)
                        InfoRow("Year Built", landmarkData.yearBuilt)
                        InfoRow("Height", landmarkData.height)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = landmarkData.description,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interesting facts
                Text(
                    text = "Interesting Facts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                landmarkData.interestingFacts.forEachIndexed { index, fact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = fact,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // "Okay" button - Only show if not from history
                if (!fromHistory) {
                    Button(
                        onClick = onSaveToHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Okay, Save to History")
                    }
                } else {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text("Back to History")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

data class LandmarkData(
    val name: String,
    val location: String,
    val architectureStyle: String,
    val yearBuilt: String,
    val height: String,
    val description: String,
    val interestingFacts: List<String>
)

