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
        val onBackPressedEvent = activity.events.onBackPressedEvent
        onBackPressedEvent.setListener {
            onBackPressedEvent.result = false

            ScreensAdapter.get.onBackPressedListener?.let {
                onBackPressedEvent.result = true
                it.invoke()
            }
        }
    }
}