package com.example.pickletrack

import android.app.Application
import android.util.Log

class AppApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        Log.i("AppApplication", "Creating AppContainer with LOCAL implementations")
        container = AppContainer()
    }
}
