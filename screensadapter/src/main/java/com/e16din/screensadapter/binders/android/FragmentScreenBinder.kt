package com.e16din.screensadapter.binders.android

import android.view.View
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.fragments.BaseFragment

abstract class FragmentScreenBinder<SCREEN>(adapter: ScreensAdapter<*, *>) :
        ScreenBinder<SCREEN>(adapter) {

    override val view: View
        get() = fragment.view!!

    protected val fragment: BaseFragment
        get() = findCurrentFragment()
                ?: throw NullPointerException("Fragment must be not null!")
}