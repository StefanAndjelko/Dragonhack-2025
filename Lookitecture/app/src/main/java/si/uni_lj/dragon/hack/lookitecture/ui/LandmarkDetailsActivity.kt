package si.uni_lj.dragon.hack.lookitecture.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
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
        val landmarkName = intent.getStringExtra("LANDMARK_NAME") ?: "Eiffel Tower"

        // For demonstration, always show Eiffel Tower data
        // In a real app, you would have different landmark data based on recognition
        val landmarkData = getLandmarkData(landmarkName)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LandmarkDetailsScreen(
                        imageUri = imageUri,
                        landmarkData = landmarkData,
                        fromHistory = fromHistory,
                        onSaveToHistory = {
                            // Save to history when "Okay" is clicked
                            if (imageUriString != null) {
                                saveLandmarkToHistory(landmarkData, imageUriString)
                                Toast.makeText(this, "Added to history", Toast.LENGTH_SHORT).show()
                            }
                            finish()
                        },
                        onBack = { finish() }
                    )
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
                architect = landmarkData.architect,
                architectureStyle = landmarkData.architectureStyle,
                yearBuilt = landmarkData.yearBuilt
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

    // Return hardcoded data based on the landmark name
    // In a real app, you would have different data for different landmarks
    private fun getLandmarkData(name: String): LandmarkData {
        // For now, just return Eiffel Tower data
        return LandmarkData(
            name = "Eiffel Tower",
            location = "Paris, France",
            architect = "Gustave Eiffel",
            architectureStyle = "Structural Expressionism / Modern Architecture",
            yearBuilt = "1889",
            height = "330 meters (1,083 feet)",
            description = "The Eiffel Tower is a wrought-iron lattice tower located on the Champ de Mars in Paris. " +
                    "It is one of the most recognizable structures in the world and has become an iconic symbol of Paris and France. " +
                    "It was originally built as the entrance arch for the 1889 World's Fair.\n\n" +
                    "The tower features an innovative design with exposed structural elements, showcasing the beauty of its engineering. " +
                    "It represents early modern architecture where the structure itself becomes the aesthetic rather than being hidden. " +
                    "Its distinctive shape with four curved lattice legs anchored into concrete foundations was revolutionary for its time.\n\n" +
                    "The tower is composed of 18,000 metallic parts joined together by 2.5 million rivets. It weighs 10,100 tons " +
                    "but exerts less ground pressure than a person sitting in a chair.",
            interestingFacts = listOf(
                "The Eiffel Tower was initially criticized by many of France's leading artists and intellectuals for its design.",
                "It was originally intended to be a temporary structure and was scheduled to be dismantled in 1909.",
                "The tower shrinks by about 6 inches (15 cm) in cold weather due to thermal contraction of the metal.",
                "The Eiffel Tower is repainted every 7 years, requiring 60 tons of paint.",
                "There are 1,665 steps to the top of the tower, though visitors typically use elevators."
            )
        )
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
                        InfoRow("Architect", landmarkData.architect)
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
    val architect: String,
    val architectureStyle: String,
    val yearBuilt: String,
    val height: String,
    val description: String,
    val interestingFacts: List<String>
)
