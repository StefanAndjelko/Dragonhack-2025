package si.uni_lj.dragon.hack.lookitecture.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import si.uni_lj.dragon.hack.lookitecture.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import org.tensorflow.lite.task.vision.classifier.Classifications
import si.uni_lj.dragon.hack.lookitecture.helpers.ImageClassifierHelper

/**
 * MainActivity for the Lookitecture application
 * Displays a photo capture screen that allows users to take or upload photos
 */
class MainActivity : ComponentActivity(), ImageClassifierHelper.ClassifierListener {
    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private var classificationResult by mutableStateOf("")
    private var detectedLandmarkName by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView, View.TRANSLATION_Y, 0f, -splashScreenView.height.toFloat()
            )
            slideUp.interpolator = AnticipateInterpolator()
            slideUp.duration = 500L
            slideUp.doOnEnd { splashScreenView.remove() }
            slideUp.start()
        }
        super.onCreate(savedInstanceState)
        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            imageClassifierListener = this
        )
        setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.7f))) {

                    Image(
                        painter = painterResource(id = R.drawable.main_background),
                        contentDescription = "Background Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        PhotoCaptureScreen(imageClassifierHelper = imageClassifierHelper,
                            classificationResult = classificationResult,
                            detectedLandmarkName = detectedLandmarkName,
                            onClearResult = { classificationResult = "" })
                    }
                }
            }
        }
    }

    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
        val topResult = results?.firstOrNull()?.categories?.firstOrNull()
        if (topResult != null) {
            detectedLandmarkName = topResult.label
            classificationResult = "Prediction: ${topResult.label} (${(topResult.score * 100).toInt()}%)\nTook ${inferenceTime}ms"
        } else {
            detectedLandmarkName = ""
            classificationResult = "No results."
        }
    }

    override fun onError(error: String) {
        classificationResult = "Error: $error"
    }
}

/**
 * Screen that allows users to capture or upload photos
 * Displays the selected image in high quality
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureScreen(
    imageClassifierHelper: ImageClassifierHelper,
    classificationResult: String,
    detectedLandmarkName: String,
    onClearResult: () -> Unit
) {
    // ---- STATE ----
    // Image that will be displayed
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    // Current URI for camera captures
    var currentCameraUri by remember { mutableStateOf<Uri?>(null) }


    val context = LocalContext.current


    fun prepareNewCameraUri(): Uri {
        val newImageFile = createImageFile(context)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            newImageFile
        )
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentCameraUri != null) {
            selectedImageUri = currentCameraUri
        }
    }

    fun takePhoto() {
        currentCameraUri = prepareNewCameraUri()
        currentCameraUri?.let { takePictureLauncher.launch(it) }
    }


    fun navigateToLandmarkDetails(imageUri: Uri) {
        val intent = Intent(context, LandmarkDetailsActivity::class.java).apply {
            putExtra("IMAGE_URI", imageUri.toString())
            putExtra("LANDMARK_NAME", detectedLandmarkName)
        }
        context.startActivity(intent)
    }

    fun navigateToHistory() {
        try {
            val intent = Intent(context, HistoryActivity::class.java)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error navigating to history", e)
            Toast.makeText(context, "Error opening history", Toast.LENGTH_SHORT).show()
        }
    }

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

    val orientation = LocalConfiguration.current.orientation
    LaunchedEffect(selectedImageUri) {
        if (selectedImageUri != null) {
            val source = ImageDecoder.createSource(context.contentResolver, selectedImageUri!!)
            val bitmap = ImageDecoder.decodeBitmap(source)
            val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            imageClassifierHelper.classify(argbBitmap, orientation)
        }
    }

    // ---- UI ----
    Box(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("") },
            actions = {
                IconButton(onClick = { navigateToHistory() }) {
                    Icon(Icons.Default.History, contentDescription = "View History")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (selectedImageUri != null) {
                            Modifier.background(Color(0xFF769AB2), RoundedCornerShape(12.dp))
                        } else {
                            Modifier
                        }
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

                    Image(
                        painter = rememberAsyncImagePainter(model = selectedImageUri),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 8.dp,
                                    bottomEnd = 8.dp
                                )
                            ),
                        contentDescription = "Selected Image",
                        contentScale = ContentScale.Fit
                    )


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
                }
            }


            ActionButtons(
                onTakePhotoClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        takePhoto()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onUploadPhotoClick = {
                    galleryLauncher.launch("image/*")
                },
                onViewHistoryClick = {
                    navigateToHistory()
                }
            )
        }
    }
}


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


@Composable
fun ActionButtons(
    onTakePhotoClick: () -> Unit,
    onUploadPhotoClick: () -> Unit,
    onViewHistoryClick: () -> Unit
) {

    val buttonColor = Color( 0xfffa7850)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        CircularIconButton(
            onClick = onTakePhotoClick,
            icon = R.drawable.icon_camera,
            contentDescription = "Take Photo",
            backgroundColor = buttonColor
        )

        CircularIconButton(
            onClick = onUploadPhotoClick,
            icon = R.drawable.icon_arrow_up,
            contentDescription = "Upload Photo",
            backgroundColor = buttonColor
        )
    }
}

@Composable
fun CircularIconButton(
    onClick: () -> Unit,
    icon: Int,
    contentDescription: String,
    backgroundColor: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .aspectRatio(1f)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = Color.Gray.copy(alpha = 0.3f)
            )
            .border(
                width = 2.dp,
                color = Color(0xFF5E7B8E),
                shape = CircleShape
            ),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

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
