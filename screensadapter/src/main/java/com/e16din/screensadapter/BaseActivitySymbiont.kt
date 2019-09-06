package com.e16din.screensadapter

import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.helpers.setListener


open class BaseActivitySymbiont {

    init {
        BaseActivity.symbiontListener = { activity ->
            initListeners(activity)
        }
    }

    open fun initListeners(activity: BaseActivity) {
        activity.onBackPressedEvent.setListener {
            ScreensAdapter.get.onBackPressedListener?.invoke()
        }
    }
}