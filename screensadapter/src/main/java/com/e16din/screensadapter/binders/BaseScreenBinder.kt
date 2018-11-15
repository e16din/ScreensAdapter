package com.e16din.screensadapter.binders

import android.app.Activity
import android.content.res.Resources
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensmodel.ScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

// Note! Hide supportScreens only through supportScreens adapter.hideCurrentScreen()
abstract class BaseScreenBinder(val adapter: ScreensAdapter<*, *>) :
        CoroutineScope,
        ScreenModel.System,
        ScreenModel.User {

    override val coroutineContext: CoroutineContext
        get() = GlobalScope.coroutineContext

    protected val data by lazy {
        adapter.getCurrentData()
    }

    protected lateinit var screensForBinder: Collection<Any>

    private fun Activity.getContentView() =
            this.findViewById<View>(android.R.id.content)!!

    protected val activity: BaseActivity
        get() = adapter.getCurrentActivity()
                ?: throw NullPointerException("Activity must be not null!")

    protected val view: View
        get() = activity.getContentView()

    protected val resources: Resources
        get() = activity.resources

    var onBackPressed: (() -> Unit)? = null

    fun getString(id: Int): String = resources.getString(id)
    fun getString(id: Int, formatArgs: Array<Any> = emptyArray()): String {
        return resources.getString(id, formatArgs)
    }

    fun getColor(id: Int) = ContextCompat.getColor(activity, id)

    open fun isEnabled() = true

    fun setScreens(screens: Collection<Any>) {
        screensForBinder = screens
    }

    override fun hideScreen() {
        adapter.hideCurrentScreen()
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

    fun <T : Fragment?> findFragmentById(id: Int): T {
        return activity.supportFragmentManager.findFragmentById(id) as T
    }

    open fun onBind() {}

    open fun onShow() {}
    open fun onFocus() {}

    open fun onLostFocus() {}
    open fun onHide() {}
}