package com.example.taiwanesehouse.database.converters

import androidx.room.TypeConverter
import com.example.taiwanesehouse.database.entities.OrderItemEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class TypeConverters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            Gson().fromJson<List<String>>(
                value,
                object : TypeToken<List<String>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromOrderItemsList(value: List<OrderItemEntity>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toOrderItemsList(value: String): List<OrderItemEntity> {
        return try {
            Gson().fromJson<List<OrderItemEntity>>(
                value,
                object : TypeToken<List<OrderItemEntity>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return try {
            Gson().fromJson<Map<String, String>>(
                value,
                object : TypeToken<Map<String, String>>() {}.type
            ) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}