package si.uni_lj.dragon.hack.lookitecture.util

import android.content.Context
import android.util.Log
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager class for handling landmark history storage and retrieval with improved performance
 */
object LandmarkHistoryManager {
    private const val PREFS_NAME = "LandmarkHistoryPrefs"
    private const val HISTORY_KEY = "landmark_history"
    private const val TAG = "LandmarkHistoryManager"
    
    // In-memory cache to avoid repeated SharedPreferences reads
    private val historyCache = ConcurrentHashMap<String, List<HistoryLandmarkData>>()
    
    // Lazily initialized Gson instance for better performance
    private val gson by lazy { Gson() }

    /**
     * Adds a landmark to history with optimized performance
     */
    fun addToHistory(context: Context, landmarkData: HistoryLandmarkData) {
        try {
            // Get current history list using the cache if available
            val cacheKey = context.packageName
            val currentHistory = historyCache[cacheKey] ?: getHistory(context)
            val history = currentHistory.toMutableList()

            // Add new entry at the beginning (most recent first)
            history.add(0, landmarkData)
            
            // Update the cache immediately
            historyCache[cacheKey] = history
            
            // Save in the background to avoid blocking UI
            Thread {
                try {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val json = gson.toJson(history)
                    prefs.edit().putString(HISTORY_KEY, json).apply() // Using apply() for background saving
                    Log.d(TAG, "History saved asynchronously, size: ${history.size}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in background history save", e)
                }
            }.start()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to history", e)
        }
    }

    /**
     * Gets all landmarks in history with memory caching for better performance
     */
    fun getHistory(context: Context): List<HistoryLandmarkData> {
        val cacheKey = context.packageName
        
        // Return from cache if available
        historyCache[cacheKey]?.let { cachedHistory ->
            return cachedHistory
        }
        
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(HISTORY_KEY, null)

            if (json.isNullOrEmpty()) {
                Log.d(TAG, "No history found in SharedPreferences")
                // Update cache with empty list
                historyCache[cacheKey] = emptyList()
                return emptyList()
            }

            val type = object : TypeToken<List<HistoryLandmarkData>>() {}.type
            val history = gson.fromJson<List<HistoryLandmarkData>>(json, type) ?: emptyList()

            // Update cache
            historyCache[cacheKey] = history
            
            Log.d(TAG, "Retrieved history items: ${history.size}")
            return history
        } catch (e: Exception) {
            Log.e(TAG, "Error getting history", e)
            return emptyList()
        }
    }

    /**
     * Clears all history and cache
     */
    fun clearHistory(context: Context) {
        try {
            val cacheKey = context.packageName
            
            // Clear the cache immediately
            historyCache.remove(cacheKey)
            
            // Clear shared preferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(HISTORY_KEY).apply()
            
            Log.d(TAG, "History cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing history", e)
        }
    }

    /**
     * Gets the number of items in history efficiently
     */
    fun getHistorySize(context: Context): Int {
        val cacheKey = context.packageName
        return historyCache[cacheKey]?.size ?: getHistory(context).size
    }

    /**
     * Gets landmark data from history by name, returns null if not found
     */
    fun getLandmarkFromHistory(context: Context, landmarkName: String): HistoryLandmarkData? {
        return getHistory(context).find { it.name == landmarkName }
    }
}

/**
 * Data class representing a landmark in history with complete information
 */
data class HistoryLandmarkData(
    val name: String,
    val imageUri: String?,
    val location: String,
    val architectureStyle: String,
    val yearBuilt: String,
    val height: String = "Information unavailable", // Added from LandmarkData
    val description: String = "No description available", // Added from LandmarkData
    val interestingFacts: List<String> = emptyList(), // Added from LandmarkData
    val timestamp: String = getCurrentTimestamp(),
    val id: String = UUID.randomUUID().toString()
) {
    companion object {
        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        fun getCurrentTimestamp(): String {
            return dateFormatter.format(Date())
        }
    }
}

