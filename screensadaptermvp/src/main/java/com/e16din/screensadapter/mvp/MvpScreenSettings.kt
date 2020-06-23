package com.e16din.screensadapter.mvp

import android.content.pm.ActivityInfo
import com.e16din.screensadapter.R
import com.e16din.screensadapter.ScreenSettings
import com.e16din.screensadapter.activities.DefaultActivity
import kotlin.reflect.KClass

class MvpScreenSettings(
        override var activityCls: KClass<*> = DefaultActivity::class,
        val presenterCls: KClass<*>,
        override var isFullscreen: Boolean = false,
        override var isDialog: Boolean = false,
        override var isTranslucent: Boolean = false,
        override var finishOnNextScreen: Boolean = false,
        override var finishPreviousScreen: Boolean = false,
        override var finishAllPreviousScreens: Boolean = false,
        override var layoutId: Int? = null,
        override var menuId: Int? = null,
        override var orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        override var themeId: Int = R.style.Theme_AppCompat,
        override var requestCode: Int? = null,
        override var screenId: Int = -1,
        override var data: Any? = null,
        var reuseScreen: Boolean = false,
        var parent: Any? = null

) : ScreenSettings(activityCls = activityCls,
        isFullscreen = isFullscreen,
        isDialog = isDialog,
        isTranslucent = isTranslucent,
        finishOnNextScreen = finishOnNextScreen,
        finishPreviousScreen = finishPreviousScreen,
        finishAllPreviousScreens = finishAllPreviousScreens,
        layoutId = layoutId,
        menuId = menuId,
        orientation = orientation,
        themeId = themeId,
        requestCode = requestCode,
        screenId = screenId,
        data = data)