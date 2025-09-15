//Room Database
package com.example.taiwanesehouse.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.taiwanesehouse.database.dao.FeedbackDao
import com.example.taiwanesehouse.database.dao.FoodItemDao
import com.example.taiwanesehouse.database.dao.OrderDao
import com.example.taiwanesehouse.database.dao.PaymentDao
import com.example.taiwanesehouse.database.entities.UserEntity
import com.example.taiwanesehouse.database.dao.UserDao
import com.example.taiwanesehouse.database.entities.FeedbackEntity
import com.example.taiwanesehouse.database.entities.FoodItemEntity
import com.example.taiwanesehouse.database.entities.OrderEntity
import com.example.taiwanesehouse.database.entities.OrderItemEntity
import com.example.taiwanesehouse.database.entities.PaymentEntity

@Database(
    entities = [
        UserEntity::class,
        FeedbackEntity::class,
        FoodItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentEntity::class
    ],
    version = 6, // Increment version for migration
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun orderDao(): OrderDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taiwanese_house_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}