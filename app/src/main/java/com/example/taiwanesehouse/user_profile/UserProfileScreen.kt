package com.example.taiwanesehouse.user_profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.taiwanesehouse.BottomNavigationBar
import com.example.taiwanesehouse.R
import com.example.taiwanesehouse.enumclass.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavController) {
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
                Spacer(modifier = Modifier.height(16.dp))
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
                    title = "Edit User Name",
                    onClick = {navController.navigate(UserProfile.EditName.name)}
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
                    icon = Icons.Default.Lock,
                    title = "Password Update",
                    onClick = {navController.navigate(UserProfile.PasswordUpdate.name)}
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
                Spacer(modifier = Modifier.height(16.dp))
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
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color(0xFFFFC107).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFFFFC107)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f),
                color = Color(0xFF333333)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun UserProfileHeader() {
    // State variables for user data
    var userName by remember { mutableStateOf("Loading...") }
    var userPhone by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with image selection
        } else {
            showToast(context, "Permission needed to access photos")
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            profileImageUri = selectedUri
            // Upload to Firebase Storage
            scope.launch {
                uploadProfileImage(selectedUri, auth, storage, firestore, context) { success, url ->
                    isUploadingImage = false
                    if (success && url != null) {
                        profileImageUrl = url
                        showToast(context, "Profile picture updated successfully!")
                    } else {
                        showToast(context, "Failed to update profile picture")
                        profileImageUri = null
                    }
                }
            }
            isUploadingImage = true
        }
    }

    // Check permission and launch image picker
    fun selectImage() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                imagePickerLauncher.launch("image/*")
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    // Fetch user data when composable is first created
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Get email from Firebase Auth
                    userEmail = currentUser.email ?: ""

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
                            val profileImage = userDoc.getString("profileImageUrl")

                            if (!username.isNullOrBlank()) {
                                userName = username
                            }
                            if (!phoneNumber.isNullOrBlank()) {
                                userPhone = phoneNumber
                            }
                            if (!profileImage.isNullOrBlank()) {
                                profileImageUrl = profileImage
                            }
                        }
                    } catch (e: Exception) {
                        // If Firestore fails, stick with Auth data
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                // Profile Image
                if (profileImageUri != null) {
                    AsyncImage(
                        model = profileImageUri,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFFFC107), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else if (profileImageUrl != null) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFFFC107), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.user_avatar),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFFFC107), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                // Camera icon overlay
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFFFFC107), CircleShape)
                        .align(Alignment.BottomEnd)
                        .clickable { selectImage() }
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "📸",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                        ),
                        color = Color(0xFF333333),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Show email
                    if (userEmail.isNotBlank()) {
                        Text(
                            userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Show phone number if available
                    if (userPhone.isNotBlank()) {
                        Text(
                            userPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF999999),
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// Helper function to upload profile image
suspend fun uploadProfileImage(
    imageUri: Uri,
    auth: FirebaseAuth,
    storage: FirebaseStorage,
    firestore: FirebaseFirestore,
    context: Context,
    onComplete: (Boolean, String?) -> Unit
) {
    try {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val storageRef = storage.reference
                .child("profile_images")
                .child("${currentUser.uid}.jpg")

            // Upload image
            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Update Firestore document
            firestore.collection("users")
                .document(currentUser.uid)
                .update("profileImageUrl", downloadUrl)
                .await()

            onComplete(true, downloadUrl)
        } else {
            onComplete(false, null)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(false, null)
    }
}