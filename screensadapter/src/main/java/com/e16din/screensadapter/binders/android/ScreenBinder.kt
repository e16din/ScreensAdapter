package com.e16din.screensadapter.binders.android

import com.e16din.screensadapter.ScreensAdapter

abstract class ScreenBinder<SCREEN>(adapter: ScreensAdapter<*, *>) :
        BaseAndroidScreenBinder(adapter) {

    val screen: SCREEN
        get() = screensForBinder.first() as SCREEN


    val supportScreens: Collection<Any>
        get() = screensForBinder

    inline fun <reified T> getSupportScreen(): T {
        return supportScreens.first { T::class.java.isInstance(it) }
                as T
    }
}