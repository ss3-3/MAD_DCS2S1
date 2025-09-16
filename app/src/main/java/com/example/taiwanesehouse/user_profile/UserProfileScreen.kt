package com.example.taiwanesehouse.user_profile

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taiwanesehouse.BottomNavigationBar
import com.example.taiwanesehouse.R
import com.example.taiwanesehouse.enumclass.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavController,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Your Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107),
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top
        ) {
            item {
                UserProfileHeader()
            }

            item {
                ProfileOptionItem(
                    icon = Icons.Default.Edit,
                    title = "Edit User Name",
                    onClick = {navController.navigate(UserProfile.EditName.name)}
                )
            }

            item {
                ProfileOptionItem(
                    icon = Icons.Default.Lock,
                    title = "Password Update",
                    onClick = {navController.navigate(UserProfile.PasswordUpdate.name)}
                )
            }

            item {
                ProfileOptionItem(
                    icon = Icons.Default.Info,
                    title = "Verify Email",
                    onClick = { navController.navigate("VerifyEmail") }
                )
            }

            item {
                ProfileOptionItem(
                    icon = Icons.Default.Edit,
                    title = "Change Email Address",
                    onClick = { navController.navigate("ChangeEmail") }
                )
            }

            item {
                ProfileOptionItem(
                    icon = Icons.AutoMirrored.Default.List,
                    title = "Payment History",
                    onClick = {navController.navigate(UserProfile.PaymentHistory.name)}
                )
            }

            item {
                ProfileOptionItem(
                    icon = Icons.Default.Info,
                    title = "Feedback",
                    onClick = {navController.navigate(UserProfile.Feedback.name)}
                )
            }

            item {
                ProfileOptionItem(
                    icon = Icons.AutoMirrored.Default.ExitToApp,
                    title = "Logout",
                    onClick = { navController.navigate(UserProfile.Logout.name)}
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
fun ProfileOptionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier
                    .size(42.dp)
                    .padding(end = 16.dp),
                tint = Color(0xFFFFC107)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun UserProfileHeader() {
    // State variables for user data
    var userName by remember { mutableStateOf("Loading...") }
    var userPhone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    // Fetch user data when composable is first created
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // First try to get display name from Firebase Auth
                    val displayName = currentUser.displayName
                    if (!displayName.isNullOrBlank()) {
                        userName = displayName
                        userPhone = currentUser.phoneNumber ?: ""
                    }

                    // Then try to get more detailed info from Firestore
                    try {
                        val userDoc = firestore.collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()

                        if (userDoc.exists()) {
                            val username = userDoc.getString("username")
                            val phoneNumber = userDoc.getString("phoneNumber")

                            if (!username.isNullOrBlank()) {
                                userName = username
                            }
                            if (!phoneNumber.isNullOrBlank()) {
                                userPhone = phoneNumber
                            }
                        }
                    } catch (e: Exception) {
                        // If Firestore fails, stick with Auth data
                        // This ensures we still show something even if Firestore is unavailable
                    }
                } else {
                    userName = "Guest User"
                }
            } catch (e: Exception) {
                userName = "Unknown User"
            } finally {
                isLoading = false
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .padding(end = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.user_avatar),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 16.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Column {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFFFFC107)
                )
            } else {
                Text(
                    userName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                // Show phone number if available
                if (userPhone.isNotBlank()) {
                    Text(
                        userPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}