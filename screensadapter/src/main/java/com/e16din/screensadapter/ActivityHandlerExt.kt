package com.e16din.screensadapter

import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.binders.IScreenBinder
import com.e16din.screensadapter.binders.android.BaseAndroidScreenBinder
import java.lang.ref.WeakReference


private const val TAG = "SA.ActivityHandler"

private var isScreenShown = false

fun ScreensAdapter<*, *>.onActivityCreateBeforeSuperCalled(activity: BaseActivity, mainScreenCls: Class<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityCreateBeforeSuperCalled()")

    currentActivityRef = WeakReference(activity)
    val binder = mainBindersMap[mainScreenCls]
    binder!!.onPrepare()

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityCreateBeforeSuperCalled()")
        childBinder.onPrepare()
    }
}

fun ScreensAdapter<*, *>.onActivityCreated(activity: BaseActivity, mainScreenCls: Class<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityCreated()")

    currentActivityRef = WeakReference(activity)
    val binder = mainBindersMap[mainScreenCls]
    binder!!.onBind()

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityCreated()")
        childBinder.onBind()
    }
}

fun ScreensAdapter<*, *>.onActivityStart(activity: BaseActivity, mainScreenCls: Class<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityStart()")
    isScreenShown = true
    currentActivityRef = WeakReference(activity)

    val binder = mainBindersMap[mainScreenCls]
    binder!!.counter += 1
    binder!!.onShow(binder.counter)

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityStart()")
        childBinder.counter += 1
        childBinder.onShow(childBinder.counter)
    }
}

fun ScreensAdapter<*, *>.onActivityResume(mainScreenCls: Class<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityResume()")
    val binder = mainBindersMap[mainScreenCls]
    binder!!.onFocus(binder.counter)

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityResume()")
        childBinder.onFocus(childBinder.counter)
    }
}

fun ScreensAdapter<*, *>.beforeNextActivityStart(mainScreenCls: Class<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.beforeNextActivityStart()")
    val binder = mainBindersMap[mainScreenCls]
    binder!!.onNextScreen()

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.beforeNextActivityStart()")
        childBinder.onNextScreen()
    }
}

fun ScreensAdapter<*, *>.onActivityPause(mainScreenCls: Class<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityPause()")

    val binder = mainBindersMap[mainScreenCls]
    binder!!.onLostFocus(binder.counter)

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityPause()")
        childBinder.onLostFocus(binder.counter)
    }
}

fun ScreensAdapter<*, *>.onActivityStopAfterTransition(activity: BaseActivity, mainScreenCls: Class<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityStopAfterTransition()")
    val binder = mainBindersMap[mainScreenCls]
    binder!!.onHide(binder.counter)

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityStopAfterTransition()")
        childBinder.onHide(binder.counter)
    }

    val isSameActivity = getCurrentActivity()?.equals(activity) == true
    if (isSameActivity) {
        isScreenShown = false
    }

    if (!isScreenShown) {
        getApp().onHideAllScreens(screenSettingsStack.size)
    }
}

fun ScreensAdapter<*, *>.onActivityResult(activity: BaseActivity, requestCode: Int, resultCode: Int, data: Intent?, mainScreenCls: Class<*>) {
    Log.w(TAG, "${mainScreenCls.simpleName}.onActivityResult(requestCode = $requestCode, resultCode = $resultCode)")

    isScreenShown = true
    currentActivityRef = WeakReference(activity)

    val binder = mainBindersMap[mainScreenCls]
    (binder as BaseAndroidScreenBinder<*>).onActivityResult(requestCode, resultCode, data)

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityResult()")
        (childBinder as BaseAndroidScreenBinder<*>).onActivityResult(requestCode, resultCode, data)
    }
}

fun ScreensAdapter<*, *>.onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, mainScreenCls: Class<*>) {
    Log.w(TAG, "${mainScreenCls.simpleName}.onRequestPermissionsResult(requestCode = $requestCode, grantResults = $grantResults)")

    val binder = mainBindersMap[mainScreenCls]
    (binder as BaseAndroidScreenBinder<*>).onRequestPermissionsResult(requestCode, permissions, grantResults)

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onRequestPermissionsResult()")
        (childBinder as BaseAndroidScreenBinder<*>).onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

fun ScreensAdapter<*, *>.onBackPressed() {
    Log.d(TAG, "onBackPressed()")
    Log.w(TAG, "screenSettingsStack: $screenSettingsStack")

    if (onBackPressedListener != null) {
        onBackPressedListener!!.invoke()
        return
    }

    if (screenSettingsStack.isNotEmpty()) {
        backToPreviousScreenOrClose()
    }
}

fun ScreensAdapter<*, *>.onOptionsItemSelected(item: MenuItem): Boolean {
    Log.d(TAG, "onOptionsItemSelected(itemId = ${item.itemId})")
    return onOptionsItemSelectedListener?.invoke(item) ?: false
}

fun ScreensAdapter<*, *>.onPrepareOptionsMenu(menu: Menu?): Boolean {
    Log.d(TAG, "onPrepareOptionsMenu()")
    return onPrepareOptionsMenuListener?.invoke(menu) ?: true
}

private fun ScreensAdapter<*, *>.callForActualChildBinders(mainScreenCls: Class<*>, call: (IScreenBinder) -> Unit) {
    childBindersMap[mainScreenCls]!!.forEach { (childScreenCls, childBinder) ->
        call.invoke(childBinder)
    }
}
