package com.e16din.screensadapter.binders.android

import android.view.View
import androidx.fragment.app.Fragment
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.fragments.BaseFragment

abstract class FragmentScreenBinder<SCREEN : Any>(adapter: ScreensAdapter<*, *>) :
        ScreenBinder<SCREEN>(adapter) {

    var fragmentId: Long = -2

    override val view: View
        get() = fragment.view!!

    protected val fragment: BaseFragment
        get() = findCurrentFragment()
                ?: throw NullPointerException("Fragment must be not null!")

    fun findCurrentFragment(fragments: List<Fragment>): BaseFragment? {
        val allFragments = getAllFragments(fragments)
        val currentFragment = allFragments.find {
            it is BaseFragment && it.fragmentId == fragmentId
        }
        return currentFragment as BaseFragment?
    }

    fun findCurrentFragment(): BaseFragment? {
        val fragments = getParentFragmentManager().fragments
        return findCurrentFragment(fragments)
    }

    override fun isVisible(): Boolean {
        return findCurrentFragment()?.isVisible ?: false
    }

    open fun onSelectedInPager() {
        // override it
    }

    open fun onDeselectedInPager() {
        // override it
    }
}