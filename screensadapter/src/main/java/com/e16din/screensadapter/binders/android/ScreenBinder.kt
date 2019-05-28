package com.e16din.screensadapter.binders.android

import com.e16din.screensadapter.ScreensAdapter

abstract class ScreenBinder<SCREEN : Any>(adapter: ScreensAdapter<*, *>) :
        BaseAndroidScreenBinder<SCREEN>(adapter) {
}