package com.e16din.screensadapter.mvp.binders.android

import com.e16din.screensadapter.mvp.MvpScreensAdapter

abstract class ScreenBinder<SCREEN : Any>(adapter: MvpScreensAdapter<*, *>) :
        BaseAndroidScreenBinder<SCREEN>(adapter) {
}