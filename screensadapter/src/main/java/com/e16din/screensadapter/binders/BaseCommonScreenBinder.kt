package com.e16din.screensadapter.binders

import com.e16din.screensadapter.ScreensAdapter
import java.lang.ref.WeakReference

// Note! Hide supportScreens only through supportScreens screensAdapter.hideCurrentScreen()
abstract class BaseCommonScreenBinder(screensAdapter: ScreensAdapter<*, *>) {

    private var screensAdapterRef = WeakReference(screensAdapter)

    val screensAdapter: ScreensAdapter<*, *>
        get() = screensAdapterRef.get()!!

    protected lateinit var screensForBinder: Collection<Any>

    open fun isEnabled() = true

    fun setScreens(screens: Collection<Any>) {
        screensForBinder = screens
    }

    open fun onPrepare() {}

    open fun onBind() {}

    open fun onShow() {}
    open fun onFocus() {}

    open fun onLostFocus() {}
    open fun onHide() {}
}