package com.example.pickletrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as AppApplication

        setContent {
            val nav = rememberNavController()

            NavHost(navController = nav, startDestination = "home") {
                composable("home") {
                    val vm = remember<HomeViewModel> { app.container.homeViewModel() }
                    DisposableEffect(Unit) { vm.start(); onDispose { vm.clear() } }
                    HomeScreen(vm = vm, onAdd = { nav.navigate("add") }, onOpen = { nav.navigate("detail/$it") })
                }
                composable("add") {
                    val vm = remember { app.container.addSessionViewModel() }
                    DisposableEffect(Unit) { onDispose { vm.clear() } }
                    AddSessionScreen(vm = vm, onDone = { nav.popBackStack() })
                }

                composable("detail/{id}") { entry ->
                    val id = entry.arguments?.getString("id") ?: return@composable
                    val vm = remember { app.container.detailViewModel(id) }
                    DisposableEffect(id) { vm.load(id); onDispose { vm.clear() } }
                    SessionDetailScreen(vm, onBack = { nav.popBackStack()})
                }
            }
        }
    }
}
