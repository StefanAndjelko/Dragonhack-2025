package si.uni_lj.dragon.hack.lookitecture.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.animation.doOnEnd
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import si.uni_lj.dragon.hack.lookitecture.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * MainActivity for the Lookitecture application
 * Displays a photo capture screen that allows users to take or upload photos
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Create your custom animation.
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView,
                View.TRANSLATION_Y,
                0f,
                -splashScreenView.height.toFloat()
            )
            slideUp.interpolator = AnticipateInterpolator()
            slideUp.duration = 500L

            // Call SplashScreenView.remove at the end of your custom animation.
            slideUp.doOnEnd { splashScreenView.remove() }

            // Run your animation.
            slideUp.start()
        }
        super.onCreate(savedInstanceState)
        setContent {
            Main()
        }
    }
    @Preview
    @Composable
    private fun Main() {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoCaptureScreen()
                }
            }
        }
    }

/**
 * Screen that allows users to capture or upload photos
 * Displays the selected image in high quality
 */
@Composable
fun PhotoCaptureScreen() {
    // ---- STATE ----
    // Image that will be displayed
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    // Current URI for camera captures
    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }

    // ---- CONTEXT ----
    val context = LocalContext.current

    // ---- FUNCTIONS ----
    // Creates a new URI for camera captures
    fun prepareNewCameraUri(): Uri {
        val newImageFile = createImageFile(context)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            newImageFile
        )
    }

    // ---- ACTIVITY LAUNCHERS ----
    // Launcher for taking photos with the camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // If the photo was taken successfully, update the displayed image
        if (success && currentCameraUri != null) {
            selectedImageUri = currentCameraUri
        }
    }

    // Takes a photo using the camera
    fun takePhoto() {
        // Create a new URI for this photo
        currentCameraUri = prepareNewCameraUri()
        // Launch camera with this URI
        currentCameraUri?.let { takePictureLauncher.launch(it) }
    }

    // Navigate to landmark details
    fun navigateToLandmarkDetails(imageUri: Uri) {
        val intent = Intent(context, LandmarkDetailsActivity::class.java).apply {
            putExtra("IMAGE_URI", imageUri.toString())
        }
        context.startActivity(intent)
    }

    // Launcher for requesting camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isPermissionGranted ->
        if (isPermissionGranted) {
            takePhoto()
        }
    }

    // Launcher for selecting images from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // Update the displayed image with the selected gallery image
        uri?.let { selectedImageUri = it }
    }

    // ---- UI ----
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App title
        Text(
            text = "Lookitecture",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Image display area - With click to navigate
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(alpha = 0.2f))
                .border(
                    width = 1.dp,
                    color = Color.LightGray,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(
                    enabled = selectedImageUri != null,
                    onClick = {
                        selectedImageUri?.let { navigateToLandmarkDetails(it) }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                // Display the selected image
                Image(
                    painter = rememberAsyncImagePainter(model = selectedImageUri),
                    contentDescription = "Selected Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Add a hint to tap for details
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Tap image to view landmark details",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Display a placeholder
                ImagePlaceholder()
            }
        }

        // Action buttons
        ActionButtons(
            onTakePhotoClick = {
                // Check for camera permission before taking photo
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                    takePhoto()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onUploadPhotoClick = {
                galleryLauncher.launch("image/*")
            }
        )
    }
}

/**
 * Placeholder shown when no image is selected
 */
@Composable
fun ImagePlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_photo_placeholder),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "No image selected",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Take or upload a photo of a landmark",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Action buttons for taking or uploading photos
 */
@Composable
fun ActionButtons(
    onTakePhotoClick: () -> Unit,
    onUploadPhotoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Take Photo button
        Button(
            onClick = onTakePhotoClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .padding(end = 8.dp)
        ) {
            Text("Take Photo")
        }

        // Upload Photo button
        Button(
            onClick = onUploadPhotoClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .padding(start = 8.dp)
        ) {
            Text("Upload Photo")
        }
    }
}

/**
 * Creates a temporary file for storing camera images
 */
private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = context.getExternalFilesDir(null)
    return File.createTempFile(
        imageFileName,
        ".jpg",
        storageDir
    )
}