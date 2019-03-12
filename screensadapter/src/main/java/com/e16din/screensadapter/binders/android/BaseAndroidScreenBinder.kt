package com.e16din.screensadapter.binders.android

import android.app.Activity
import android.content.res.Resources
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.binders.BaseCommonScreenBinder
import com.e16din.screensadapter.fragments.BaseFragment
import com.e16din.screensmodel.BaseScreen
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Note! Hide supportScreens only through supportScreens screensAdapter.hideCurrentScreen()
abstract class BaseAndroidScreenBinder(screensAdapter: ScreensAdapter<*, *>) :
        BaseCommonScreenBinder(screensAdapter),
        BaseScreen.SystemAgent,
        BaseScreen.UserAgent {

    private fun Activity.getContentView() =
            this.findViewById<View>(android.R.id.content)!!

    var fragmentId: Long = -2

    protected val activity: BaseActivity
        get() = screensAdapter.getCurrentActivity()
                ?: throw NullPointerException("Activity must be not null!")

    protected open val view: View
        get() = activity.getContentView()

    protected val resources: Resources
        get() = activity.resources

    override fun getString(id: Int, formatArgs: Array<Any>): String {
        return if (formatArgs.isEmpty()) {
            resources.getString(id)
        } else {
            resources.getString(id, formatArgs)
        }
    }

    override fun getColor(id: Int) = ContextCompat.getColor(activity, id)

    override fun hideScreen() {
        Log.d("ScreensAdapter", "hideScreen()")
        screensAdapter.hideCurrentScreen()
    }

    override fun runOnBackgroundThread(runnable: suspend () -> Unit): Job {
        return launch {
            runnable()
        }
    }

    override fun runOnUiThread(runnable: suspend () -> Unit): Job {
        return launch(Main) {
            runnable()
        }
    }

    override fun log(message: String, tag: String, type: BaseScreen.LogType) {
        when (type) {
            BaseScreen.LogType.Debug -> Log.d(tag, message)
            BaseScreen.LogType.Info -> Log.i(tag, message)
            BaseScreen.LogType.Warning -> Log.w(tag, message)
            BaseScreen.LogType.Error -> Log.e(tag, message)
        }
    }

    override fun log(e: Throwable, message: String, tag: String) {
        Log.e(tag, message, e)
    }

    protected fun setOnBackPressedListener(listener: (() -> Unit)?) {
        screensAdapter.onBackPressed = listener
    }

    override fun resetOnBackPressedListener() {
        setOnBackPressedListener(null)
    }

    protected fun findFragmentById(fragmentId: Int): Fragment? {
        return activity.supportFragmentManager.findFragmentById(fragmentId)
    }

    protected fun findFragmentByTag(tag: String): Fragment? {
        return activity.supportFragmentManager.findFragmentByTag(tag)
    }

    protected fun <T : Fragment> findFragmentByClass(cls: Class<T>): Fragment? {
        return getAllFragments().first { it.javaClass.isInstance(cls) }
    }

    protected fun getAllFragments(fragments: List<Fragment>): List<Fragment> {
        val result = ArrayList<Fragment>()
        forEachFragmentRecursively(fragments) {
            result.add(it)
            return@forEachFragmentRecursively false
        }
        return result
    }

    protected fun getAllFragments(): List<Fragment> {
        val fragments = activity.supportFragmentManager.fragments
        return getAllFragments(fragments)
    }

    protected fun forEachFragmentRecursively(fragments: List<Fragment>,
                                             function: (fragment: Fragment) -> Boolean): MutableList<Fragment> {
        val result = ArrayList<Fragment>()
        fragments.forEach {
            val success = function.invoke(it)
            if (success) {
                return@forEach
            }

            val childFragments = it.childFragmentManager.fragments
            forEachFragmentRecursively(childFragments, function)
        }
        return result
    }

    fun findCurrentFragment(fragments: List<Fragment>): BaseFragment? {
        return getAllFragments(fragments).find { it is BaseFragment && it.fragmentId == fragmentId } as BaseFragment?
    }

    fun findCurrentFragment(): BaseFragment? {
        val fragments = getParentFragmentManager().fragments
        return findCurrentFragment(fragments)
    }

    protected fun getParentFragmentManager() = activity.supportFragmentManager

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


