package com.e16din.screensadapter.binders.android

import android.view.View
import com.e16din.screensadapter.ScreensAdapter

abstract class ChildScreenBinder<SCREEN>(adapter: ScreensAdapter<*, *>,
                                         private val viewId: Int) : ScreenBinder<SCREEN>(adapter) {

    override val view: View
        get() = activity.findViewById(viewId)

}