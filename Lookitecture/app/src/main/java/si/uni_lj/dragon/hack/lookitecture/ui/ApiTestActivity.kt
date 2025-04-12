package si.uni_lj.dragon.hack.lookitecture.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import si.uni_lj.dragon.hack.lookitecture.BuildConfig

class ApiTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ApiTestScreen()
                }
            }
        }
    }
}

@Composable
fun ApiTestScreen() {
    var inputText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("Response will appear here") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Input field
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter text") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button
        Button(
            onClick = {
                if (inputText.isNotEmpty()) {
                    isLoading = true
                    // Call the API in a coroutine
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = callGeminiApi("Give me a brief overview of the $inputText, " +
                                    "including its architecture style, purpose, location, height, and any interesting facts." +
                                    "Without that \"Okay, here's a brief overview ...,\", just give me the overview.")
                            withContext(Dispatchers.Main) {
                                responseText = response
                                isLoading = false
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                responseText = "Error: ${e.message}"
                                isLoading = false
                                Log.e("ApiTestActivity", "API call failed", e)
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && inputText.isNotEmpty()
        ) {
            Text(if (isLoading) "Loading..." else "Submit")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Response display
        Text(
            text = responseText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

// Function to call the Gemini API
private suspend fun callGeminiApi(prompt: String): String {
    val client = OkHttpClient()

    val apiKey = BuildConfig.GEMINI_API_KEY
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

    val jsonBody = JSONObject().apply {
        put("contents", JSONObject().apply {
            put("parts", JSONObject().apply {
                put("text", prompt)
            })
        })
    }.toString()

    val mediaType = "application/json; charset=utf-8".toMediaType()
    val requestBody = jsonBody.toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .addHeader("Content-Type", "application/json")
        .build()

    return withContext(Dispatchers.IO) {
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: "No response"

        if (response.isSuccessful) {
            // Parse the response JSON to extract the generated text
            try {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }
                "Could not parse response"
            } catch (e: Exception) {
                "Error parsing response: ${e.message}"
            }
        } else {
            "Error: ${response.code} - ${response.message}"
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ApiTestScreenPreview() {
    MaterialTheme {
        ApiTestScreen()
    }
}