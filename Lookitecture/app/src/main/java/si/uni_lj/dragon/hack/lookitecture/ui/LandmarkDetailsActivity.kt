package si.uni_lj.dragon.hack.lookitecture.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

class LandmarkDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the image URI from the intent
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val imageUri = imageUriString?.let { Uri.parse(it) }

        // Hardcoded data for the Eiffel Tower
        val landmarkData = LandmarkData(
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

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LandmarkDetailsScreen(imageUri, landmarkData)
                }
            }
        }
    }
}

@Composable
fun LandmarkDetailsScreen(imageUri: Uri?, landmarkData: LandmarkData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display the landmark name as a title
        Text(
            text = landmarkData.name,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display the image
        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = landmarkData.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

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