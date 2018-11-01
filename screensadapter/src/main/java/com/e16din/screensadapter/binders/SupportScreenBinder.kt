package com.e16din.screensadapter.binders

import com.e16din.screensadapter.ScreensAdapter

abstract class SupportScreenBinder(adapter: ScreensAdapter<*, *>) :
        BaseScreenBinder(adapter) {

    val screens: Collection<Any>
        get() = screensForBinder

    inline fun <reified SCREEN> getScreen(): SCREEN {
        return screens.first { SCREEN::class.java.isInstance(it) }
                as SCREEN
    }
}