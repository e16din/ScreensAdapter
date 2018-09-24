package com.e16din.screensadapter.binders

import com.e16din.screensadapter.ScreensAdapter

open class ScreenBinder<SCREEN>(adapter: ScreensAdapter<*, *>) :
        BaseScreenBinder(adapter) {

    val screen: SCREEN
        get() = screensForBinder.first() as SCREEN


    val supportScreens: Collection<Any>
        get() = screensForBinder

    inline fun <reified T> getSupportScreen(): T {
        return supportScreens.first { T::class.java.isInstance(it) }
                as T
    }
}