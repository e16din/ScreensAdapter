package com.e16din.screensadapter.mvp.binders.android

import android.view.View
import com.e16din.screensadapter.mvp.MvpScreensAdapter

abstract class ChildScreenBinder<SCREEN : Any>(adapter: MvpScreensAdapter<*, *>,
                                               private val viewId: Int) : ScreenBinder<SCREEN>(adapter) {

    override val view: View
        get() = activity.findViewById(viewId)

} //todo: что с этим делать?