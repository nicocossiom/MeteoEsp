package com.mssdepas.meteoesp.util

import java.lang.Thread.UncaughtExceptionHandler

class GlobalExceptionHandler(
    private val defaultHandler: UncaughtExceptionHandler?
) : UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Log the exception using your AppLogger
        AppLogger.e("UncaughtException", "An uncaught exception occurred in thread: ${thread.name}", throwable)

        // You can add more handling here, like:
        // - Sending the crash report to a remote server (e.g., Firebase Crashlytics)
        // - Displaying a user-friendly error message before the app closes
        // - Attempting to gracefully save any pending data (use with caution)

        // It's important to call the original default handler (if one exists)
        // This ensures that the system can still perform its default crash handling,
        // which might include things like generating a system crash report or killing the process.
        defaultHandler?.uncaughtException(thread, throwable)
    }
}