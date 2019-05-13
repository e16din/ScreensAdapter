package com.e16din.screensadapter.settings

import android.content.pm.ActivityInfo
import com.e16din.screensadapter.R

open class ScreenSettings(
        val screenCls: Class<*>,
        val layoutId: Int? = null,
        val menuId: Int? = null,
        val orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        val themeId: Int = R.style.Theme_AppCompat,
        var finishPreviousScreen: Boolean = false, //todo: try to manage screens independent of the activity stack
        var finishAllPreviousScreens: Boolean = false, //todo: and move  finishPreviousScreen and finishAllPreviousScreens to SystemAgent
        val activityCls: Class<*> = Any::class.java,
        val isFullscreen: Boolean = false,
        val isDialog: Boolean = false,
        val requestCode: Int? = null) {
}