package com.example.pickletrack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Minimal placeholder HomeScreen which can accept an optional ViewModel from the app container.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel, onAdd: () -> Unit, onOpen: (String) -> Unit) {
    // Provide an explicit initial value for collectAsState so the delegate has a proper State<T> backing
    // in common multiplatform compilation contexts.
    val state by vm.state.collectAsState(initial = UiHomeState())

    Scaffold(
        topBar = { TopAppBar(title = { Text("PickleTrack") }) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Text("+") } }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("This week: ${state.minutesThisWeek} min")
                    Text("Total sessions: ${state.totalSessions}")
                    state.error?.let { Text(it) }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn {
                    items(state.sessions) { s ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                .clickable { onOpen(s.id) }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Duration: ${s.durationMinutes} min")
                                s.location?.let { Text("Location: $it") }
                            }
                        }
                    }
                }
            }
        }
    }
}
