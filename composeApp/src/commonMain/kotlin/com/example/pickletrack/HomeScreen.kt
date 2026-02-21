package com.example.pickletrack

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Minimal placeholder HomeScreen which can accept an optional ViewModel from the app container.
@Composable
fun HomeScreen(vm: HomeViewModel? = null, onAdd: () -> Unit, onOpen: (String) -> Unit) {
    Column {
        Button(onClick = onAdd) {
            Text("Go to Add")
        }
        // example open button
        Button(onClick = { onOpen("example-id") }) {
            Text("Open detail")
        }
    }
}
