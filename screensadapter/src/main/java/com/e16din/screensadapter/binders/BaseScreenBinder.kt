package com.e16din.screensadapter.binders

import android.app.Activity
import android.util.Log
import android.view.View
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensmodel.ScreenModel
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

// Note! Hide supportScreens only through supportScreens adapter.hideCurrentScreen()
abstract class BaseScreenBinder(open val adapter: ScreensAdapter<*, *>) :
        ScreenModel.System,
        ScreenModel.User {

    protected lateinit var screensForBinder: Collection<Any>

    private fun Activity.getContentView() =
            this.findViewById<View>(android.R.id.content)!!

    protected val activity: BaseActivity
        get() = adapter.getCurrentActivity()
                ?: throw NullPointerException("Activity must be not null!")

    protected val view: View
        get() = adapter.getCurrentActivity()?.getContentView()
                ?: throw NullPointerException("Activity must be not null!")

    open fun isEnabled() = true

    fun setScreens(screens: Collection<Any>) {
        screensForBinder = screens
    }

    override fun hideScreen() {
        adapter.hideCurrentScreen()
    }

    override fun runOnBackgroundThread(runnable: suspend () -> Unit) {
        launch(CommonPool) {
            runnable()
        }
    }

    override fun runOnUiThread(runnable: suspend () -> Unit) {
        launch(UI) {
            runnable()
        }
    }

    override fun log(message: String) {
        Log.d("logs", message)
    }

    open fun onBind() {}

    open fun onShow() {}
    open fun onFocus() {}

    open fun onLostFocus() {}
    open fun onHide() {}
}