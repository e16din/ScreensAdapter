package com.e16din.screensadapter

import android.content.pm.ActivityInfo
import com.e16din.screensadapter.activities.DefaultActivity
import kotlin.reflect.KClass

open class ScreenSettings(
        open val activityCls: KClass<*> = DefaultActivity::class,
        open val isFullscreen: Boolean = false,
        open val isDialog: Boolean = false,
        open val isTranslucent: Boolean = false,

        open var finishOnNextScreen: Boolean = false,
        open var finishPreviousScreen: Boolean = false,
        open var finishAllPreviousScreens: Boolean = false,

        open val layoutId: Int? = null,
        open val menuId: Int? = null,
        open val orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        open val themeId: Int = R.style.Theme_AppCompat,
        open val requestCode: Int? = null,
        open var screenId: Int = NO_ID,

        open var data: Any? = null
) {

    companion object {
        const val NO_ID = -1
    }
}