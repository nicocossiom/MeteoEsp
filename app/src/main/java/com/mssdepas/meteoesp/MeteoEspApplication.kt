package com.mssdepas.meteoesp

import android.app.Application
import android.util.Log
import com.mssdepas.meteoesp.util.GlobalExceptionHandler // Ensure this import path is correct

class MeteoEspApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Get the current default uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // Set your custom handler
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(defaultHandler))

        // You can add other application-wide initializations here
        // For example, initializing logging libraries, dependency injection, etc.
        Log.i("MeteoEspApplication", "Custom UncaughtExceptionHandler initialized.")
    }
}