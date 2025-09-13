// AuthExtensions.kt
package com.example.taiwanesehouse.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.taiwanesehouse.viewmodel.AuthViewModel

/**
 * Extension to handle lifecycle-aware operations for AuthViewModel
 */
@Composable
fun AuthViewModel.OnLifecycleEvent(onEvent: (event: Lifecycle.Event) -> Unit) {
    val eventHandler by rememberUpdatedState(onEvent)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            eventHandler(event)
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Helper function to sync pending users when app comes to foreground
 */
@Composable
fun AuthViewModel.HandleOfflineSync() {
    OnLifecycleEvent { event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                // Sync pending users when app comes to foreground
                syncPendingUsers()
            }
            else -> {}
        }
    }
}