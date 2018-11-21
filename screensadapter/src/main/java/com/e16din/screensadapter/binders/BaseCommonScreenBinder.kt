package com.e16din.screensadapter.binders

import com.e16din.screensadapter.ScreensAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlin.coroutines.CoroutineContext

// Note! Hide supportScreens only through supportScreens screensAdapter.hideCurrentScreen()
abstract class BaseCommonScreenBinder(val screensAdapter: ScreensAdapter<*, *>) :
        CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

    protected lateinit var screensForBinder: Collection<Any>

    open fun isEnabled() = true

    fun setScreens(screens: Collection<Any>) {
        screensForBinder = screens
    }

    open fun onBind() {}

    open fun onShow() {}
    open fun onFocus() {}

    open fun onLostFocus() {}
    open fun onHide() {}
}