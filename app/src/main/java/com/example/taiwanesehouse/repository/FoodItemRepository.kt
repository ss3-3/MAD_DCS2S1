// 4. Repository
package com.example.taiwanesehouse.repository

import com.example.taiwanesehouse.database.dao.FoodItemDao
import com.example.taiwanesehouse.database.entities.FoodItemEntity
import kotlinx.coroutines.flow.Flow

class FoodItemRepository(private val foodItemDao: FoodItemDao) {

    fun getAllFoodItems(): Flow<List<FoodItemEntity>> = foodItemDao.getAllFoodItems()

    fun getFoodItemsByCategory(category: String): Flow<List<FoodItemEntity>> =
        foodItemDao.getFoodItemsByCategory(category)

    suspend fun getFoodItemById(id: String): FoodItemEntity? =
        foodItemDao.getFoodItemById(id)

    suspend fun getFoodItemByName(name: String): FoodItemEntity? =
        foodItemDao.getFoodItemByName(name)

    fun searchFoodItems(searchQuery: String): Flow<List<FoodItemEntity>> =
        foodItemDao.searchFoodItems(searchQuery)

    suspend fun insertAll(foodItems: List<FoodItemEntity>) =
        foodItemDao.insertAll(foodItems)

    suspend fun insert(foodItem: FoodItemEntity) =
        foodItemDao.insert(foodItem)

    suspend fun delete(foodItem: FoodItemEntity) =
        foodItemDao.delete(foodItem)

    suspend fun deleteAll() =
        foodItemDao.deleteAll()
}