package com.e16din.screensmodel

open class BaseScreen {

    interface UserAgent {
        fun hideScreen()
    }

    interface SystemAgent {
        fun log(message: String)

        fun runOnBackgroundThread(runnable: suspend () -> Unit)

        fun runOnUiThread(runnable: suspend () -> Unit)
    }
}