package com.e16din.screensadapter.binders

import android.support.v4.app.Fragment
import com.e16din.screensadapter.ScreensAdapter
import java.util.*


abstract class SupportScreenBinder(adapter: ScreensAdapter<*, *>) :
        BaseScreenBinder(adapter) {

    val screens: Collection<Any>
        get() = screensForBinder

    inline fun <reified SCREEN> getScreen(): SCREEN {
        return screens.first { SCREEN::class.java.isInstance(it) }
                as SCREEN
    }

    protected fun <T : Fragment> findFragmentByClass(cls: Class<T>): Fragment {
        return getAllFragments().first { it.javaClass.isInstance(cls) }
    }

    protected fun getAllFragments(): List<Fragment> {
        return activity.supportFragmentManager.fragments
    }

    protected fun getVisibleFragments(): List<Fragment> {
        val allFragments = getAllFragments()
        val visibleFragments = ArrayList<Fragment>()
        for (fragment in allFragments) {
            if (fragment.isVisible) {
                visibleFragments.add(fragment)
            }
        }
        return visibleFragments
    }
}