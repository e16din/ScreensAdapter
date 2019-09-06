package com.e16din.screensadapter.mvp.binders.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.ConnectivityManager
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.mvp.MvpScreensAdapter
import com.e16din.screensadapter.mvp.binders.BaseCommonScreenBinder
import com.e16din.screensadapter.mvp.helpers.LocalCoroutineScope
import com.e16din.screensadapter.mvp.model.BaseScreen
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

// Note! Hide supportScreens only through supportScreens screensAdapter.hideCurrentScreen()
abstract class BaseAndroidScreenBinder<SCREEN : Any>(screensAdapter: MvpScreensAdapter<*, *>) :
        BaseCommonScreenBinder<SCREEN>(screensAdapter),
        LocalCoroutineScope,
        BaseScreen.SystemAgent,
        BaseScreen.UserAgent {

    companion object {
        private const val REQ_CODE_START = 10
        private var lastRequestCode = REQ_CODE_START

        fun generateRequestCode(): Int {
            lastRequestCode += 1
            return lastRequestCode
        }
    }

    fun Activity.getContentView() =
            this.findViewById<View>(android.R.id.content)!!

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

    override fun hideScreen(result: Any?, withAnimation: Boolean, resultCode: Int?) {
        Log.d("ScreensAdapter", "hideScreen() | result: $result")
        screensAdapter.hideCurrentScreen(withAnimation, resultCode)
    }

    override fun onHide() {
        coroutineContext.cancelChildren()
    }

    override fun runOnBackgroundThread(runnable: suspend () -> Unit) = async {
        runnable.invoke()
    }

    override fun runOnUiThread(runnable: suspend () -> Unit) = launch {
        runnable.invoke()
    }

    @SuppressLint("MissingPermission")
    override fun isOnline(): Boolean {
        try {
            val connectivityManager =
                    activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return connectivityManager.activeNetworkInfo?.isConnected == true
        } catch (e: Exception) {
            return false
        }
    }

    override fun isVisible() = true

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

    protected fun findFragmentById(fragmentScreenId: Int): Fragment? {
        return activity.supportFragmentManager.findFragmentById(fragmentScreenId)
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

    protected open fun getParentFragmentManager() = activity.supportFragmentManager

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

    open fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {}

    open fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {}

    override fun onBackPressed(overrideBackPressedFunction: (() -> Unit)?) {
        overrideBackPressedFunction?.invoke()
                ?: performOnBackPressed()
    }

    override fun performOnBackPressed() {
        Log.d(MvpScreensAdapter.TAG, "onBackPressed()")

        screensAdapter.backToPreviousScreenOrClose(true, Activity.RESULT_CANCELED)
    }
}


