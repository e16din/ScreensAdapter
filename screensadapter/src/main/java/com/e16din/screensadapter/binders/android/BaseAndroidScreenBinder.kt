package com.e16din.screensadapter.binders.android

import android.app.Activity
import android.content.res.Resources
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.binders.BaseCommonScreenBinder
import com.e16din.screensadapter.fragments.BaseFragment
import com.e16din.screensmodel.BaseScreen
import kotlinx.coroutines.Dispatchers.Main
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

    var onBackPressed: (() -> Unit)? = null

    fun getString(id: Int): String = resources.getString(id)
    fun getString(id: Int, formatArgs: Array<Any> = emptyArray()): String {
        return resources.getString(id, formatArgs)
    }

    fun getColor(id: Int) = ContextCompat.getColor(activity, id)

    override fun hideScreen() {
        screensAdapter.hideCurrentScreen()
    }

    override fun runOnBackgroundThread(runnable: suspend () -> Unit) {
        launch {
            runnable()
        }
    }

    override fun runOnUiThread(runnable: suspend () -> Unit) {
        launch(Main) {
            runnable()
        }
    }

    override fun log(message: String) {
        Log.d("logs", message)
    }

    fun setOnBackPressedListener(listener: (() -> Unit)?) {
        onBackPressed = listener
    }

    fun resetOnBackPressedListener() {
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
        val fragments = activity.supportFragmentManager.fragments
        return findCurrentFragment(fragments)
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