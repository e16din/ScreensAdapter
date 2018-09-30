package com.e16din.screensadapter.settings

import android.content.pm.ActivityInfo
import com.e16din.screensadapter.R
import java.io.Serializable

//todo: use Parcelize
open class ScreenSettings(val screenCls: Class<*>,
                          val layoutId: Int? = null,
                          val orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                          val themeId: Int = R.style.Theme_AppCompat,
                          val finishOnNextScreen: Boolean = false,
                          var finishAllPreviousScreens: Boolean = false,
                          val activityCls: Class<*> = Any::class.java) : Serializable