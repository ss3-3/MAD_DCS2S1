// 3. Updated ViewModel
package com.example.taiwanesehouse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taiwanesehouse.database.FoodItemEntity
import com.example.taiwanesehouse.database.TaiwaneseRestaurantDatabase
import com.example.taiwanesehouse.repository.MenuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FoodItemViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MenuRepository

    init {
        val database = TaiwaneseRestaurantDatabase.getDatabase(application)
        repository = MenuRepository(database)

        // Initialize data when ViewModel is created
        viewModelScope.launch {
            repository.initializeMenuData()
        }
    }

    // Get all food items
    suspend fun getAllFoodItems(): Flow<List<FoodItemEntity>> {
        return repository.getAllFoodItems()
    }

    // Get food items by category
    fun getFoodItemsByCategory(category: String): Flow<List<FoodItemEntity>> {
        return repository.getFoodItemsByCategory(category)
    }

    // Search food items
    fun searchFoodItems(query: String): Flow<List<FoodItemEntity>> {
        return repository.searchFoodItems(query)
    }

    // Get food item by ID
    suspend fun getFoodItemById(id: String): FoodItemEntity? {
        return repository.getFoodItemById(id)
    }

    // Get food item by name
    suspend fun getFoodItemByName(name: String): FoodItemEntity? {
        return repository.getFoodItemByName(name)
    }

    // Refresh data from Firebase
    fun refreshFromFirebase() {
        viewModelScope.launch {
            repository.refreshFromFirebase()
        }
    }

    // Add new food item
    fun addFoodItem(item: FoodItemEntity) {
        viewModelScope.launch {
            repository.addFoodItem(item)
        }
    }

    // Update food item
    fun updateFoodItem(item: FoodItemEntity) {
        viewModelScope.launch {
            repository.updateFoodItem(item)
        }
    }

    // Delete food item
    fun deleteFoodItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteFoodItem(itemId)
        }
    }
}