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

    // Map of common misspellings or variations to their correct names
    private val landmarkNameMap = mapOf(
        "effile tower" to "Eiffel Tower",
        "eifle tower" to "Eiffel Tower",
        "eiffle tower" to "Eiffel Tower",
        "eifal tower" to "Eiffel Tower",
        "statue of liberty" to "Statue of Liberty",
        "big ben" to "Big Ben",
        "colloseum" to "Colosseum",
        "coliseum" to "Colosseum",
        "tajmahal" to "Taj Mahal",
        "taj-mahal" to "Taj Mahal",
        "sydney opera" to "Sydney Opera House",
        "burj khalifa" to "Burj Khalifa",
        "burg khalifa" to "Burj Khalifa"
        // Add more common misspellings as needed
    )

    /**
     * Normalize landmark name by checking against known variations
     */
    private fun normalizeLandmarkName(detectedName: String): String {
        val lowerName = detectedName.lowercase().trim()

        // Check if it's a known variant first
        landmarkNameMap[lowerName]?.let {
            return it
        }

        // Check if any key contains the detected name (for partial matches)
        landmarkNameMap.entries.forEach { (key, value) ->
            if (lowerName.contains(key) || key.contains(lowerName)) {
                return value
            }
        }

        // If no match found, capitalize each word as a basic normalization
        return detectedName.split(" ")
            .map { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }
            .joinToString(" ")
    }

    /**
     * Fetches landmark information from the Gemini API
     */
    suspend fun getLandmarkInfo(detectedLandmarkName: String): LandmarkData {
        try {
            // Normalize the landmark name first
            val normalizedName = normalizeLandmarkName(detectedLandmarkName)
            Log.d(TAG, "Detected: $detectedLandmarkName, Normalized: $normalizedName")

            val prompt = """
                I need information about a landmark that was detected as "$detectedLandmarkName".
                
                First, tell me the correct official name of this landmark.
                
                Then, provide a JSON response with the following fields:
                - name: the official/correct name of the landmark
                - location: specific location like "Venice, Italy"
                - architectureStyle: the architectural style
                - yearBuilt: when it was constructed
                - height: height in meters
                - description: a concise description (1-2 sentences)
                - interestingFacts: array of 3 short, interesting facts
                
                Format your response as valid JSON only - no additional text or explanation.
            """.trimIndent()

            val response = callGeminiApi(prompt)
            return parseLandmarkResponse(response, normalizedName)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting landmark info: ${e.message}", e)
            // Return fallback data if API call fails
            return getFallbackLandmarkData(detectedLandmarkName)
        }
    }

    // Function to call the Gemini API
    private suspend fun callGeminiApi(prompt: String): String {
        val client = OkHttpClient()

//        val apiKey = BuildConfig.GEMINI_API_KEY
        val apiKey = "AIzaSyDfiyy-JXPml4EYftsMC4FNs8iQ5F1URIc"
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

            // Get the official name or use the normalized name as fallback
            val officialName = jsonObject.optString("name", defaultName)

            // Extract location
            val location = jsonObject.optString("location", "Unknown location")

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

            // Log the mapping outcome
            Log.d(TAG, "API returned landmark name: $officialName")

            return LandmarkData(
                name = officialName,
                location = location,
                architectureStyle = jsonObject.optString("architectureStyle", "Unknown style"),
                yearBuilt = jsonObject.optString("yearBuilt", "Unknown year"),
                height = jsonObject.optString("height", "Unknown height"),
                description = jsonObject.optString("description",
                    "No description available for this landmark."),
                interestingFacts = interestingFacts,
                // Add coordinates based on location if possible
                coordinates = getCoordinatesFromLocation(location, officialName)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing landmark JSON: ${e.message}", e)
            // If parsing fails, return fallback data
            return getFallbackLandmarkData(defaultName)
        }
    }

    // Try to extract coordinates from location if possible
    private fun getCoordinatesFromLocation(location: String, landmarkName: String): Pair<Double, Double> {
        // First try to get coordinates from our predefined list
        val knownCoordinates = getCoordinatesForLandmark(landmarkName)
        if (knownCoordinates.first != 0.0 || knownCoordinates.second != 0.0) {
            return knownCoordinates
        }

        // In a real app, you might call a geocoding API here to get coordinates from the location string
        // For this example, we'll return default coordinates
        return Pair(0.0, 0.0)
    }

    // Get coordinates for known landmarks
    private fun getCoordinatesForLandmark(landmarkName: String): Pair<Double, Double> {
        return when {
            landmarkName.contains("Eiffel Tower", ignoreCase = true) -> Pair(48.8584, 2.2945)
            landmarkName.contains("Great Wall of China", ignoreCase = true) -> Pair(40.4319, 116.5704)
            landmarkName.contains("Statue of Liberty", ignoreCase = true) -> Pair(40.6892, -74.0445)
            landmarkName.contains("Taj Mahal", ignoreCase = true) -> Pair(27.1751, 78.0421)
            landmarkName.contains("Colosseum", ignoreCase = true) -> Pair(41.8902, 12.4922)
            landmarkName.contains("Sydney Opera House", ignoreCase = true) -> Pair(-33.8568, 151.2153)
            landmarkName.contains("Burj Khalifa", ignoreCase = true) -> Pair(25.1972, 55.2744)
            landmarkName.contains("Empire State Building", ignoreCase = true) -> Pair(40.7484, -73.9857)
            landmarkName.contains("Leaning Tower of Pisa", ignoreCase = true) -> Pair(43.7230, 10.3966)
            landmarkName.contains("Machu Picchu", ignoreCase = true) -> Pair(-13.1631, -72.5450)
            landmarkName.contains("Sagrada Familia", ignoreCase = true) -> Pair(41.4036, 2.1744)
            landmarkName.contains("Big Ben", ignoreCase = true) -> Pair(51.5007, -0.1246)
            landmarkName.contains("Petra", ignoreCase = true) -> Pair(30.3285, 35.4444)
            landmarkName.contains("Louvre", ignoreCase = true) -> Pair(48.8606, 2.3376)
            else -> Pair(0.0, 0.0)
        }
    }

    // Provide fallback data in case the API call or parsing fails
    private fun getFallbackLandmarkData(name: String): LandmarkData {
        // Normalize the name even for fallback data
        val normalizedName = normalizeLandmarkName(name)

        return LandmarkData(
            name = normalizedName,
            location = "Information unavailable",
            architectureStyle = "Information unavailable",
            yearBuilt = "Information unavailable",
            height = "Information unavailable",
            description = "We couldn't retrieve information about this landmark. " +
                    "Please check your internet connection and try again.",
            interestingFacts = listOf(
                "No interesting facts available at the moment."
            ),
            coordinates = getCoordinatesForLandmark(normalizedName)
        )
    }
}
