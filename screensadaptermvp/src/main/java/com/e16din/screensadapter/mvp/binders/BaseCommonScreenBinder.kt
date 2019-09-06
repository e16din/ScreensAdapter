package com.e16din.screensadapter.mvp.binders

import com.e16din.screensadapter.mvp.MvpScreensAdapter
import java.lang.ref.WeakReference

// Note! Hide supportScreens only through supportScreens screensAdapter.hideCurrentScreen()
abstract class BaseCommonScreenBinder<SCREEN : Any>(screensAdapter: MvpScreensAdapter<*, *>) : IScreenBinder {

    override var counter: Int = 0

    private var screensAdapterRef = WeakReference(screensAdapter)

    val screensAdapter: MvpScreensAdapter<*, *>
        get() = screensAdapterRef.get()!!

    lateinit var screen: SCREEN

    override fun initScreen(screen: Any) {
        this.screen = screen as SCREEN
    }
}