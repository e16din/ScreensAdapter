package com.e16din.screensadapter.mvp

import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.mvp.binders.IScreenBinder
import com.e16din.screensadapter.mvp.binders.android.BaseAndroidScreenBinder
import com.e16din.screensadapter.mvp.helpers.foreach
import java.lang.ref.WeakReference
import kotlin.reflect.KClass


private const val TAG = "SA.Mvp.ActivityHandler"

private var isScreenShown = false

fun MvpScreensAdapter<*, *>.onActivityCreateBeforeSuperCalled(activity: BaseActivity, mainScreenCls: KClass<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityCreateBeforeSuperCalled()")

    currentActivityRef = WeakReference(activity)
    val binder = mainBindersMap[mainScreenCls]
    binder!!.onPrepare()

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityCreateBeforeSuperCalled()")
        childBinder.onPrepare()
    }
}

fun MvpScreensAdapter<*, *>.onActivityCreated(activity: BaseActivity, mainScreenCls: KClass<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityCreated()")

    currentActivityRef = WeakReference(activity)
    val binder = mainBindersMap[mainScreenCls]
    binder!!.onBind()

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityCreated()")
        childBinder.onBind()
    }
}

fun MvpScreensAdapter<*, *>.onActivityStart(activity: BaseActivity, mainScreenCls: KClass<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityStart()")
    isScreenShown = true
    currentActivityRef = WeakReference(activity)

    val binder = mainBindersMap[mainScreenCls]
    binder!!.counter += 1
    binder!!.onShow()

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityStart()")
        childBinder.counter += 1
        childBinder.onShow()
    }
}

fun MvpScreensAdapter<*, *>.onActivityResume(activity: BaseActivity, mainScreenCls: KClass<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityResume()")
    currentActivityRef = WeakReference(activity)
    val binder = mainBindersMap[mainScreenCls]
    binder!!.onFocus()

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityResume()")
        childBinder.onFocus()
    }
}

fun MvpScreensAdapter<*, *>.onActivityPause(mainScreenCls: KClass<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityPause()")

    val binder = mainBindersMap[mainScreenCls]
    binder!!.onLostFocus()

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityPause()")
        childBinder.onLostFocus()
    }
}

fun MvpScreensAdapter<*, *>.onActivityStopAfterTransition(activity: BaseActivity, mainScreenCls: KClass<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onActivityStopAfterTransition()")
    val binder = mainBindersMap[mainScreenCls]
    binder!!.onHide()

    childBindersMap[mainScreenCls]!!.foreach { (childScreenCls, childBinder) ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityStopAfterTransition()")
        childBinder.onHide()
        removeChildBinder(childScreenCls)
    }

    val isSameActivity = getCurrentActivity()?.equals(activity) == true
    if (isSameActivity) {
        isScreenShown = false
    }

    if (!isScreenShown) {
        getApp().onHideAllScreens()
    }

    screensMap.clear()
}

fun MvpScreensAdapter<*, *>.onActivityResult(activity: BaseActivity, requestCode: Int, resultCode: Int, data: Intent?, mainScreenCls: KClass<*>) {
    Log.w(TAG, "${mainScreenCls.simpleName}.onActivityResult(requestCode = $requestCode, resultCode = $resultCode)")

    isScreenShown = true
    currentActivityRef = WeakReference(activity)

    val binder = mainBindersMap[mainScreenCls]
    (binder as BaseAndroidScreenBinder<*>).onActivityResult(requestCode, resultCode, data)

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onActivityResult()")
        (childBinder as BaseAndroidScreenBinder<*>).onActivityResult(requestCode, resultCode, data)
    }

    // NOTE: Workaround for this issue https://stackoverflow.com/a/16449850/6445611
    fragmentBindersMap.foreach { (fragmentScreenId, pair) ->
        onFragmentActivityResult(requestCode, resultCode, data, pair.first, fragmentScreenId)
    }
}

fun MvpScreensAdapter<*, *>.onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, mainScreenCls: KClass<*>) {
    Log.w(TAG, "${mainScreenCls.simpleName}.onRequestPermissionsResult(requestCode = $requestCode, grantResults = $grantResults)")

    val binder = mainBindersMap[mainScreenCls]
    (binder as BaseAndroidScreenBinder<*>).onRequestPermissionsResult(requestCode, permissions, grantResults)

    callForActualChildBinders(mainScreenCls) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}.onRequestPermissionsResult()")
        (childBinder as BaseAndroidScreenBinder<*>).onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

fun MvpScreensAdapter<*, *>.onOptionsItemSelected(item: MenuItem): Boolean {
    Log.d(TAG, "onOptionsItemSelected(itemId = ${item.itemId})")
    return onOptionsItemSelectedListener?.invoke(item) ?: false
}

fun MvpScreensAdapter<*, *>.onPrepareOptionsMenu(menu: Menu?): Boolean {
    Log.d(TAG, "onPrepareOptionsMenu()")
    return onPrepareOptionsMenuListener?.invoke(menu) ?: true
}

// NOTE: call only for current Activity
fun MvpScreensAdapter<*, *>.onBackPressed(mainScreenCls: KClass<*>) {
    Log.d(TAG, "${mainScreenCls.simpleName}.onBackPressed()")

    val binder = mainBindersMap[mainScreenCls]
    binder!!.onBackPressed(null)
}

internal fun MvpScreensAdapter<*, *>.callForActualChildBinders(mainScreenCls: KClass<*>, call: (IScreenBinder) -> Unit) {
    childBindersMap[mainScreenCls]!!.foreach { (childScreenCls, childBinder) ->
        call.invoke(childBinder)
    }
}
