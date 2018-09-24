package com.e16din.screensmodel

open class ScreenModel {

    interface User {
        fun hideScreen()
    }

    interface System {
        fun log(message: String)

        fun runOnBackgroundThread(runnable: suspend () -> Unit)

        fun runOnUiThread(runnable: suspend () -> Unit)
    }

    open fun onBind() {}


    open fun onShow() {}

    open fun onFocus() {}


    open fun onLostFocus() {}

    open fun onHide() {}
}