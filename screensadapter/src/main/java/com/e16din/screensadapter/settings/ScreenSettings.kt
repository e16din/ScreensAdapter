package com.e16din.screensadapter.settings

import android.content.pm.ActivityInfo
import com.e16din.screensadapter.R
import kotlin.reflect.KClass

open class ScreenSettings(
        val screenCls: KClass<*>,
        val layoutId: Int? = null,
        val menuId: Int? = null,
        val orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        val themeId: Int = R.style.Theme_AppCompat,

        //todo: try to manage screens independent of the activity stack
        //todo: and move  finishPreviousScreen and finishAllPreviousScreens to SystemAgent
        var finishOnNextScreen: Boolean = false,
        var finishPreviousScreen: Boolean = false,
        var finishAllPreviousScreens: Boolean = false,

        val activityCls: KClass<*> = Any::class,
        val isFullscreen: Boolean = false,
        val isDialog: Boolean = false,
        val requestCode: Int? = null) {
}