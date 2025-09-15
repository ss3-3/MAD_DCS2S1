package com.example.taiwanesehouse.user_profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var payments by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        if (userId == null) {
            error = "Please log in to view payment history"
            isLoading = false
            return@LaunchedEffect
        }

        scope.launch {
            try {
                isLoading = true
                error = null
                // Avoid composite index by filtering client-side
                val snapshot = firestore.collection("payments")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                payments = snapshot.documents
                    .mapNotNull { it.data }
                    .sortedByDescending { (it["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L }
                    .take(50)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Payment History") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFC107)
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFFC107))
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = error ?: "Error", color = Color.Red)
                }
            }
            payments.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = "No payments yet")
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(payments) { p ->
                        val amount = (p["amount"] as? Number)?.toDouble() ?: 0.0
                        val method = p["paymentMethod"] as? String ?: ""
                        val status = p["paymentStatus"] as? String ?: ""
                        val paymentId = p["paymentId"] as? String ?: ""
                        ListItem(
                            headlineContent = { Text("RM %.2f".format(amount)) },
                            supportingContent = { Text("$method • $status") },
                            overlineContent = { Text(paymentId) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}