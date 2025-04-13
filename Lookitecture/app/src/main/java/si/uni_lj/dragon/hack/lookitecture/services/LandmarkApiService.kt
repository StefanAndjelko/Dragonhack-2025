package si.uni_lj.dragon.hack.lookitecture.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import si.uni_lj.dragon.hack.lookitecture.ui.LandmarkData
import si.uni_lj.dragon.hack.lookitecture.BuildConfig

/**
 * This class has been refactored to serve as an API utility class
 * rather than a UI Activity
 */
object LandmarkApiService {
    private const val TAG = "LandmarkApiService"

    /**
     * Fetches landmark information from the Gemini API
     */
    suspend fun getLandmarkInfo(landmarkName: String): LandmarkData {
        try {
            val prompt = "Give me a short JSON response about $landmarkName with: " +
                    "location for example \"Venice, Italy\", architectureStyle, yearBuilt, height (in meters), " +
                    "a short description, and 3 interesting facts. Be concise, no long text."


            val response = callGeminiApi(prompt)
            return parseLandmarkResponse(response, landmarkName)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting landmark info: ${e.message}", e)
            // Return fallback data if API call fails
            return getFallbackLandmarkData(landmarkName)
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
                    Log.e(TAG, "Error parsing API response", e)
                    "Error parsing response: ${e.message}"
                }
            } else {
                Log.e(TAG, "API request failed: ${response.code} - ${response.message}")
                "Error: ${response.code} - ${response.message}"
            }
        }
    }

    // Parse the API response into a LandmarkData object
    private fun parseLandmarkResponse(response: String, defaultName: String): LandmarkData {
        try {
            // Attempt to extract JSON from the response
            val jsonPattern = """\{.*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val jsonMatch = jsonPattern.find(response)
            val jsonString = jsonMatch?.value ?: response

            val jsonObject = JSONObject(jsonString)

            // Extract arrays
            val factsArray = jsonObject.optJSONArray("interestingFacts")
            val interestingFacts = mutableListOf<String>()

            if (factsArray != null) {
                for (i in 0 until factsArray.length()) {
                    interestingFacts.add(factsArray.getString(i))
                }
            } else {
                // Add default facts if missing
                interestingFacts.add("No interesting facts available.")
            }

            return LandmarkData(
                name = jsonObject.optString("name", defaultName),
                location = jsonObject.optString("location", "Unknown location"),
                architectureStyle = jsonObject.optString("architectureStyle", "Unknown style"),
                yearBuilt = jsonObject.optString("yearBuilt", "Unknown year"),
                height = jsonObject.optString("height", "Unknown height"),
                description = jsonObject.optString("description",
                    "No description available for this landmark."),
                interestingFacts = interestingFacts
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing landmark JSON: ${e.message}", e)
            // If parsing fails, return fallback data
            return getFallbackLandmarkData(defaultName)
        }
    }

    // Provide fallback data in case the API call or parsing fails
    private fun getFallbackLandmarkData(name: String): LandmarkData {
        return LandmarkData(
            name = name,
            location = "Information unavailable",
            architectureStyle = "Information unavailable",
            yearBuilt = "Information unavailable",
            height = "Information unavailable",
            description = "We couldn't retrieve information about this landmark. " +
                    "Please check your internet connection and try again.",
            interestingFacts = listOf(
                "No interesting facts available at the moment."
            )
        )
    }
}
