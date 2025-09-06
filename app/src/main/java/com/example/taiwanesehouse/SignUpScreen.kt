package com.example.taiwanesehouse

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SignUpScreen(
    navController: NavController,
    name: String,
    message: String,
) {
    val logoImage = painterResource(R.drawable.taiwanesehouselogo)
    val coverImage = painterResource(R.drawable.coverpage)

    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Image(
                painter = logoImage,
                contentDescription = null,
                modifier = Modifier.height(90.dp).padding(8.dp)
            )
            Column(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 33.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = message,
                    fontSize = 10.sp
                )
            }
        }
        Box(modifier = Modifier){
            Button(onClick = { navController.navigate(Screen.Menu.name) }){
                //should validate user first
                //if true, go to menu
                Text(text = "Menu")
                //else, display a pop-up message
            }
            Image(
                painter = coverImage,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(800.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.5F
            )
        }
    }
}