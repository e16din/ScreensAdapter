package com.e16din.screensadapter.mvp.model

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

open class BaseScreen {

    enum class LogType { Debug, Warning, Info, Error }

    interface UserAgent {
        fun hideScreen(result: Any? = null, withAnimation: Boolean = true, resultCode: Int? = null)
    }

    interface SystemAgent {
        fun log(message: String,
                tag: String = "SystemAgent",
                type: LogType = LogType.Debug)

        fun log(e: Throwable,
                message: String = "",
                tag: String = "SystemAgent")

        fun isVisible(): Boolean

        fun isOnline(): Boolean

        fun runOnBackgroundThread(runnable: suspend () -> Unit): Deferred<*>

        fun runOnUiThread(runnable: suspend () -> Unit): Job

        fun performOnBackPressed()

        fun getString(id: Int, formatArgs: Array<Any> = emptyArray()): String
        fun getColor(id: Int): Int
    }
}