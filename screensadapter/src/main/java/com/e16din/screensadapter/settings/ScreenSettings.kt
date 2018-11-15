package com.e16din.screensadapter.settings

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Parcelable
import com.e16din.screensadapter.R
import kotlinx.android.parcel.Parcelize

@Parcelize
open class ScreenSettings(val screenCls: Class<*>,
                          val layoutId: Int? = null,
                          val data: Bundle? = null,
                          val menuId: Int? = null,
                          val orientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                          val themeId: Int = R.style.Theme_AppCompat,
                          val finishOnNextScreen: Boolean = false,
                          var finishAllPreviousScreens: Boolean = false,
                          val activityCls: Class<*> = Any::class.java) : Parcelable