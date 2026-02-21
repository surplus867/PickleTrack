package com.example.pickletrack

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SessionDetailScreen(vm: SessionDetailViewModel, onBack: () -> Unit) {
    Column {
        Text("Session detail (placeholder)")
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

