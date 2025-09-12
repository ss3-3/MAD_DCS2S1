// 4. Updated Database Initializer (now simpler)
package com.example.taiwanesehouse.database

import android.content.Context
import android.util.Log
import com.example.taiwanesehouse.repository.MenuRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseInitializer {

    fun initializeDatabase(context: Context) {
        val database = TaiwaneseRestaurantDatabase.getDatabase(context)
        val repository = MenuRepository(database)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // The repository will handle Firebase sync and Room caching
                repository.initializeMenuData()
                Log.d("DatabaseInitializer", "Menu initialization completed")
            } catch (e: Exception) {
                Log.e("DatabaseInitializer", "Error initializing database", e)
            }
        }
    }
}