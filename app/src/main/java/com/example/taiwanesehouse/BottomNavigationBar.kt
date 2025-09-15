package com.example.taiwanesehouse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taiwanesehouse.enumclass.Screen

@Composable
fun BottomNavigationBar(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 8.dp)
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable {
                    navController.navigate(Screen.Menu.name) {
                        popUpTo(Screen.Menu.name) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                .padding(8.dp)
        ) {
            Text("📋", fontSize = 28.sp)
            Text("Menu", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }

        // Cart 按钮
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable {
                    navController.navigate(Screen.Cart.name) {
                        popUpTo(Screen.Cart.name) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                .padding(8.dp)
        ) {
            Text("\uD83E\uDDFE", fontSize = 28.sp)
            Text("Order", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable {
                    navController.navigate(Screen.Profile.name) {
                        popUpTo(Screen.Profile.name) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                .padding(8.dp)
        ) {
            Text("👤", fontSize = 28.sp)
            Text("Me", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}