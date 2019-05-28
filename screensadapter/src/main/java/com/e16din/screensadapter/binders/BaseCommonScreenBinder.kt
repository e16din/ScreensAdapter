package com.e16din.screensadapter.binders

import com.e16din.screensadapter.ScreensAdapter
import java.lang.ref.WeakReference

// Note! Hide supportScreens only through supportScreens screensAdapter.hideCurrentScreen()
abstract class BaseCommonScreenBinder<SCREEN : Any>(screensAdapter: ScreensAdapter<*, *>) : IScreenBinder {

    override var counter: Int = 0

    private var screensAdapterRef = WeakReference(screensAdapter)

    val screensAdapter: ScreensAdapter<*, *>
        get() = screensAdapterRef.get()!!

    lateinit var screen: SCREEN

    override fun initScreen(screen: Any) {
        this.screen = screen as SCREEN
    }
}