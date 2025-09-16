// 7. Firebase Admin Upload Script (Optional - for initial data setup)
// You can run this once to populate Firebase with your initial data

package com.example.taiwanesehouse.admin

import android.content.Context
import android.util.Log
import com.example.taiwanesehouse.R
import com.example.taiwanesehouse.dataclass.FirebaseFoodItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseMenuUploader {

    private val firestore = FirebaseFirestore.getInstance()
    private val menuCollection = firestore.collection("menu_items")

    suspend fun uploadAllMenuItems() {
        try {
            val menuItems = getAllInitialFoodItems()
            val batch = firestore.batch()

            menuItems.forEach { item ->
                val docRef = menuCollection.document(item.id)
                batch.set(docRef, item)
            }

            batch.commit().await()
            Log.d("FirebaseUpload", "Successfully uploaded ${menuItems.size} menu items")
        } catch (e: Exception) {
            Log.e("FirebaseUpload", "Error uploading menu items", e)
        }
    }

    private fun getAllInitialFoodItems(): List<FirebaseFoodItem> {
        return listOf(
            // Rice Items
            FirebaseFoodItem(
                id = "R1",
                name = "Signature Braised Pork Rice",
                description = "Japanese Pearl Rice - Signature Braised Pork - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 15.90,
                imageRes = R.drawable.signature_braised_pork_rice,
                category = "Rice"
            ),
            FirebaseFoodItem(
                id = "R2",
                name = "High CP Salted Chicken Rice",
                description = "Japanese Pearl Rice - Minced Pork - Salted Fried Chicken - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 17.90,
                imageRes = R.drawable.signature_braised_pork_rice,
                category = "Rice"
            ),
            FirebaseFoodItem(
                id = "R2",
                name = "High CP Salted Chicken Rice",
                description = "Japanese Pearl Rice - Minced Pork - Salted Fried Chicken - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 17.90,
                imageRes = R.drawable.signature_braised_pork_rice,
                category = "Rice"
            ),
            FirebaseFoodItem(
                id = "R3",
                name = "Meatball & Sausage Minced Pork Rice",
                description = "Japanese Pearl Rice - Minced Pork - Taiwan Sausage - Pork Meatball - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 17.90,
                imageRes = R.drawable.meatball_and_sausage_minced_pork_rice,
                category = "Rice"
            ),
            FirebaseFoodItem(
                id = "R4",
                name = "House Crispy Chicken Chop Rice",
                description = "Japanese Pearl Rice - Taiwan Style Chicken Chop - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 20.90,
                imageRes = R.drawable.house_crispy_chicken_chop_rice,
                category = "Rice"
            ),
            FirebaseFoodItem(
                id = "R5",
                name = "Taiwanese Pork Chop Rice",
                description = "Jasmine Pearl Rice - Taiwanese Pork Chop - First Egg - Taiwanese Pickled Vegetable - Sour Chili",
                price = 21.90,
                imageRes = R.drawable.taiwanese_belly_pork_chop_rice,
                category = "Rice"
            ),
            FirebaseFoodItem(
                id = "R6",
                name = "Khong Bah Peng",
                description = "Jasmine Pearl Rice - Stewed Pork Belly - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 21.90,
                imageRes = R.drawable.khong_bah_peng,
                category = "Rice"
            ),
            FirebaseFoodItem(
                id = "R7",
                name = "Three Cup Chicken Rice",
                description = "Japanese Pearl Rice - 3 Cup Chicken - Stewed Egg (Half) - Side Dish (Daily) - Sour Chili",
                price = 25.90,
                imageRes = R.drawable.three_cup_chicken_rice,
                category = "Rice"
            ),

            // Noodle Items
            FirebaseFoodItem(
                id = "N1",
                name = "Signature Braised Pork QQ Noodle",
                description = "Handmade Noodle - Signature Braised Pork - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 15.90,
                imageRes = R.drawable.signature_braised_pork_qq_noodle,
                category = "Noodles"
            ),
            FirebaseFoodItem(
                id = "N2",
                name = "High CP Salted Chicken QQ Noodle",
                description = "Handmade Noodle - Minced Pork - Salted Fried Chicken - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 17.90,
                imageRes = R.drawable.high_cp_salted_chicken_qq_noodle,
                category = "Noodles"
            ),
            FirebaseFoodItem(
                id = "N3",
                name = "Meatball & Sausage Minced Pork QQ Noodle",
                description = "Handmade Noodle - Minced Pork - Taiwan Sausage - Pork Meatball - Stewed Egg (Half) - Side Dish (Daily) - Sour Chili",
                price = 17.90,
                imageRes = R.drawable.meatball_and_sausage_minced_pork_qq_noodle,
                category = "Noodles"
            ),
            FirebaseFoodItem(
                id = "N4",
                name = "House Crispy Chicken Chop QQ Noodle",
                description = "Handmade Noodle - Taiwan Style Chicken Chop - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 19.90,
                imageRes = R.drawable.house_chicken_chop_qq_noodle,
                category = "Noodles"
            ),
            FirebaseFoodItem(
                id = "N5",
                name = "Taiwanese Belly Pork Chop QQ Noodle",
                description = "Handmade Noodle - Pork Chop - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 20.90,
                imageRes = R.drawable.taiwanese_belly_pork_chop_qq_noodle,
                category = "Noodles"
            ),
            FirebaseFoodItem(
                id = "N6",
                name = "Gozhabi Stewed Belly QQ Noodle",
                description = "Handmade Noodle - Stewed Pork Belly - Fried Egg - Side Dish (Daily) - Sour Chili",
                price = 20.90,
                imageRes = R.drawable.gozhabi_stewed_belly_qq_noodle,
                category = "Noodles"
            ),
            FirebaseFoodItem(
                id = "N7",
                name = "Twice Egg Scallion Noodle",
                description = "Handmade Noodle - Twice Wallet Egg - Side Dish (Daily) - Sour Chili",
                price = 11.90,
                imageRes = R.drawable.twice_egg_scallion_noodle,
                category = "Noodles"
            ),

            // Not Too Full Items
            FirebaseFoodItem(
                id = "E1",
                name = "Yam Floss Egg Crepe",
                description = "Yam Paste - Chicken Floss - Egg - Crepe",
                price = 8.90,
                imageRes = R.drawable.yam_floss_egg_crepe,
                category = "Not Too Full"
            ),
            FirebaseFoodItem(
                id = "E2",
                name = "Cheese Floss Egg Crepe",
                description = "Cheese - Chicken Floss - Egg - Crepe - Mayonnaise - Sweet Chili Sauce",
                price = 8.90,
                imageRes = R.drawable.cheese_floss_egg_crepe,
                category = "Not Too Full"
            ),
            FirebaseFoodItem(
                id = "E3",
                name = "Cheese Ham Egg Crepe",
                description = "Chicken Ham, Cheese - Egg - Crepe - Mayonnaise - Sweet Chili Sauce",
                price = 8.90,
                imageRes = R.drawable.cheese_ham_egg_crepe,
                category = "Not Too Full"
            ),
            FirebaseFoodItem(
                id = "E4",
                name = "Double Cheese Egg Scallion Sandwich",
                description = "Double Cheese - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce",
                price = 12.90,
                imageRes = R.drawable.double_cheese_egg_scallion_sandwich,
                category = "Not Too Full"
            ),
            FirebaseFoodItem(
                id = "E5",
                name = "Floss Egg Scallion Sandwich",
                description = "Chicken Floss - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce",
                price = 12.90,
                imageRes = R.drawable.floss_egg_scallion_sandwich,
                category = "Not Too Full"
            ),
            FirebaseFoodItem(
                id = "E6",
                name = "Ham Egg Scallion Sandwich",
                description = "Chicken Ham - Egg - Scallion Sandwich - Salad - Sweet Chili Sauce",
                price = 12.90,
                imageRes = R.drawable.ham_egg_scallion_sandwich,
                category = "Not Too Full"
            ),

            // Snack Items
            FirebaseFoodItem(
                id = "S1",
                name = "Garlic Slice Taiwanese Sausage",
                description = "Taiwan Sausage 2 Pcs",
                price = 8.90,
                imageRes = R.drawable.garlic_slice_taiwanese_sausage,
                category = "Snacks"
            ),
            FirebaseFoodItem(
                id = "S2",
                name = "Tempura Oyster Mushroom",
                description = "Fried Oyster Mushroom (Spicy / Original)",
                price = 9.90,
                imageRes = R.drawable.tempura_oyster_mushroom,
                category = "Snacks"
            ),
            FirebaseFoodItem(
                id = "S3",
                name = "Sweet Plum Potato Fries",
                description = "Fired Sweet Orange Potato",
                price = 9.90,
                imageRes = R.drawable.sweet_plum_potato_fried,
                category = "Snacks"
            ),
            FirebaseFoodItem(
                id = "S4",
                name = "High CP Salted Chicken",
                description = "Fried Salted Chicken (Spicy / Original)",
                price = 12.90,
                imageRes = R.drawable.high_cp_salted_chicken,
                category = "Snacks"
            ),
            FirebaseFoodItem(
                id = "S5",
                name = "Taiwanese Belly Pork Chop",
                description = "Fried Juicy Pork Chop (Spicy / Original)",
                price = 14.90,
                imageRes = R.drawable.taiwanese_belly_pork_chop,
                category = "Snacks"
            ),
            FirebaseFoodItem(
                id = "S6",
                name = "House Crispy Chicken Chop",
                description = "Fried Juicy Chicken Chop (Spicy / Original)",
                price = 13.90,
                imageRes = R.drawable.house_crispy_chicken_chop,
                category = "Snacks"
            ),
            FirebaseFoodItem(
                id = "S7",
                name = "Sweet Not Spicy",
                description = "Tempura (No Spicy)",
                price = 12.90,
                imageRes = R.drawable.sweet_not_spicy,
                category = "Snacks"
            ),

            // Drink Items
            FirebaseFoodItem(
                id = "D1",
                name = "Aloe Yakult Tea",
                description = "",
                price = 8.90,
                imageRes = R.drawable.aloe_yakult_tea,
                category = "Drinks"
            ),
            FirebaseFoodItem(
                id = "D2",
                name = "TW Aiyu Jelly",
                description = "",
                price = 7.90,
                imageRes = R.drawable.tw_aiyu_tea,
                category = "Drinks"
            ),
            FirebaseFoodItem(
                id = "D3",
                name = "Dark Aroma Lemon Tea",
                description = "",
                price = 5.90,
                imageRes = R.drawable.dark_aroma_lemon_tea,
                category = "Drinks"
            ),
            FirebaseFoodItem(
                id = "D4",
                name = "Original Lemon Tea",
                description = "",
                price = 5.90,
                imageRes = R.drawable.original_lemon_tea,
                category = "Drinks"
            ),
            FirebaseFoodItem(
                id = "D5",
                name = "Earl Grey Milk Tea",
                description = "",
                price = 7.90,
                imageRes = R.drawable.earl_grey_milk_tea,
                category = "Drinks"
            ),
            FirebaseFoodItem(
                id = "D6",
                name = "Pearl Earl Milk Tea",
                description = "",
                price = 8.90,
                imageRes = R.drawable.pearl_earl_milk_tea,
                category = "Drinks"
            ),
            FirebaseFoodItem(
                id = "D7",
                name = "White Peach Milk Tea",
                description = "",
                price = 7.90,
                imageRes = R.drawable.white_peach_milk_tea,
                category = "Drinks"
            ),
            FirebaseFoodItem(
                id = "D8",
                name = "Jasmine Milk Tea",
                description = "",
                price = 7.90,
                imageRes = R.drawable.jasmine_milk_tea,
                category = "Drinks"
            )
        )
    }
}