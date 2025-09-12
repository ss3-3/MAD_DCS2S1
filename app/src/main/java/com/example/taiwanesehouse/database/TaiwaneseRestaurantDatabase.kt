// 3. Database
package com.example.taiwanesehouse.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [FoodItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TaiwaneseRestaurantDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao

    companion object {
        @Volatile
        private var INSTANCE: TaiwaneseRestaurantDatabase? = null

        fun getDatabase(context: Context): TaiwaneseRestaurantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaiwaneseRestaurantDatabase::class.java,
                    "taiwanese_restaurant_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}