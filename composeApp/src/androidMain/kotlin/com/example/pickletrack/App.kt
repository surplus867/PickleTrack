package com.example.pickletrack

import android.app.Application

// Android Application subclass that holds the container. Renamed to avoid collision with the Composable `App()`.
class AppApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(DatabaseProvider(DatabaseFactory(this)))
    }
}