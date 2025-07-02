package com.mssdepas.meteoesp.util

import android.util.Log

object AppLogger {
    private const val TAG = "MeteoEspApp" // Define a default tag for your app

    fun d(message: String, tag: String = TAG) {
        Log.d(tag, message)
    }

    fun i(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }

    fun w(message: String, tag: String = TAG, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }

    fun e(message: String, tag: String = TAG, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }

    fun v(message: String, tag: String = TAG) {
        Log.v(tag, message)
    }
}