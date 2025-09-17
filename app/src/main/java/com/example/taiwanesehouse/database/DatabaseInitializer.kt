// 4. Updated Database Initializer
package com.example.taiwanesehouse.database

import android.content.Context
import android.util.Log
import com.example.taiwanesehouse.repository.MenuRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object DatabaseInitializer {
    private var isInitialized = false

    fun initializeDatabase(context: Context) {
        if (isInitialized) return
        
        val database = AppDatabase.getDatabase(context)
        val repository = MenuRepository(database)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // The repository will handle Firebase sync and Room caching
                repository.initializeMenuData()
                isInitialized = true
                Log.d("DatabaseInitializer", "Menu initialization completed")
            } catch (e: Exception) {
                Log.e("DatabaseInitializer", "Error initializing database", e)
                // Try to initialize local data as fallback
                try {
                    repository.initializeLocalData()
                    isInitialized = true
                    Log.d("DatabaseInitializer", "Local data initialization completed as fallback")
                } catch (fallbackError: Exception) {
                    Log.e("DatabaseInitializer", "Error initializing local data", fallbackError)
                }
            }
        }
    }

    // Synchronous initialization for critical data
    fun initializeDatabaseSync(context: Context) {
        if (isInitialized) return
        
        val database = AppDatabase.getDatabase(context)
        val repository = MenuRepository(database)

        runBlocking {
            try {
                // Force local data initialization first
                repository.initializeLocalData()
                isInitialized = true
                Log.d("DatabaseInitializer", "Synchronous local data initialization completed")
                
                // Then try Firebase sync in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        repository.syncFromFirebase()
                        Log.d("DatabaseInitializer", "Firebase sync completed in background")
                    } catch (e: Exception) {
                        Log.e("DatabaseInitializer", "Background Firebase sync failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("DatabaseInitializer", "Error in synchronous initialization", e)
            }
        }
    }

    fun isDatabaseInitialized(): Boolean = isInitialized
}