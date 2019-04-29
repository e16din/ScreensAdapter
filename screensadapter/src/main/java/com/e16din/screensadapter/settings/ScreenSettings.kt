package com.e16din.screensadapter.settings

import android.content.pm.ActivityInfo
import com.e16din.screensadapter.R

open class ScreenSettings(
        val screenCls: Class<*>,
        val layoutId: Int? = null,
        val menuId: Int? = null,
        val orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        val themeId: Int = R.style.Theme_AppCompat,
        val finishOnNextScreen: Boolean = false,
        var finishPreviousScreen: Boolean = false,
        var finishAllPreviousScreens: Boolean = false,
        val activityCls: Class<*> = Any::class.java,
        val isFullscreen: Boolean = false,
        val isDialog: Boolean = false,
        val requestCode: Int? = null) {
}