package com.e16din.screensmodel

import kotlinx.coroutines.Job

open class BaseScreen {

    enum class LogType { Debug, Warning, Info, Error }

    interface UserAgent {
        fun hideScreen()
    }

    interface SystemAgent {
        fun log(message: String,
                tag: String = "SystemAgent",
                type: LogType = LogType.Debug)

        fun log(e: Throwable,
                message: String = "",
                tag: String = "SystemAgent")

        fun runOnBackgroundThread(runnable: suspend () -> Unit): Job

        fun runOnUiThread(runnable: suspend () -> Unit): Job

        fun resetOnBackPressedListener()

        fun getString(id: Int, formatArgs: Array<Any> = emptyArray()): String
        fun getColor(id: Int): Int
    }
}