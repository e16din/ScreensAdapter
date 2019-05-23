package com.e16din.screensadapter

import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.e16din.screensadapter.activities.BaseActivity
import com.e16din.screensadapter.binders.android.BaseAndroidScreenBinder
import java.lang.ref.WeakReference


private const val TAG = "SA.ActivityHandler"

private var isScreenShown = false

fun ScreensAdapter<*, *>.onActivityCreateBeforeSuperCalled(activity: BaseActivity, screenCls: Class<*>) {
    Log.d(TAG, "onActivityCreateBeforeSuperCalled: ${screenCls.simpleName}")

    currentActivityRef = WeakReference(activity)
    val binders = getCurrentBindersByMainScreen(screenCls)
    binders.forEach { binder ->
        binder.onPrepare()
    }

    callForAllChildBinders(screenCls) { binder ->
        binder.onPrepare()
    }
}

fun ScreensAdapter<*, *>.onActivityCreated(activity: BaseActivity, screenCls: Class<*>) {
    Log.d(TAG, "onActivityCreated: ${screenCls.simpleName}")

    currentActivityRef = WeakReference(activity)
    val binders = getCurrentBindersByMainScreen(screenCls)
    binders.forEach { binder ->
        binder.onBind()
    }

    callForAllChildBinders(screenCls) { binder ->
        binder.onBind()
    }
}

fun ScreensAdapter<*, *>.onActivityStart(activity: BaseActivity, screenCls: Class<*>) {
    Log.d(TAG, "onActivityStart: ${screenCls.simpleName}")
    isScreenShown = true
    currentActivityRef = WeakReference(activity)

    val binders = getBindersByScreen(screenCls)
    binders.forEach { binder ->
        binder.onShow()
    }

    callForAllChildBinders(screenCls) { binder ->
        binder.onShow()
    }
}

private fun ScreensAdapter<*, *>.callForAllChildBinders(currentScreenCls: Class<*>, call: (BaseAndroidScreenBinder) -> Unit) {
    val childBindersByChildScreenClsMap = childBindersByMainScreenClsMap[currentScreenCls]
    childBindersByChildScreenClsMap?.forEach { childBindersEntry ->
        val childBinders = childBindersEntry.value
        childBinders.forEach { binder ->
            call.invoke(binder)
        }
    }
}

fun ScreensAdapter<*, *>.onActivityResume(screenCls: Class<*>) {
    Log.d(TAG, "onActivityResume: ${screenCls.simpleName}")
    val binders = getCurrentBindersByMainScreen(screenCls)
    binders.forEach { binder ->
        binder.onFocus()
    }

    callForAllChildBinders(screenCls) { binder ->
        binder.onFocus()
    }
}

fun ScreensAdapter<*, *>.onActivityPause(screenCls: Class<*>) {
    Log.d(TAG, "onActivityPause: ${screenCls.simpleName}")
    val binders = hiddenBinders.takeIf { hiddenBinders.isNotEmpty() }
            ?: getCurrentBindersByMainScreen(screenCls)
    binders.forEach { binder ->
        binder.onLostFocus()
    }

    callForAllChildBinders(screenCls) { binder ->
        binder.onLostFocus()
    }
}

fun ScreensAdapter<*, *>.onActivityStop(activity: BaseActivity, screenCls: Class<*>) {
    Log.d(TAG, "onActivityStop: ${screenCls.simpleName}")
    val binders = hiddenBinders.takeIf { hiddenBinders.isNotEmpty() }
            ?: getCurrentBindersByMainScreen(screenCls)
    binders.forEach { binder ->
        binder.onHide()
    }

    callForAllChildBinders(screenCls) { binder ->
        binder.onHide()
    }

    hiddenBinders.clear()

    val isSameActivity = getCurrentActivity()?.equals(activity) == true
    if (isSameActivity) {
        isScreenShown = false
    }

    if (!isScreenShown) {
        getApp().onHideAllScreens(screenSettingsStack.size)
    }
}

fun ScreensAdapter<*, *>.onActivityResult(activity: BaseActivity, requestCode: Int, resultCode: Int, data: Intent?, screenCls: Class<*>) {
    Log.w(TAG, "onActivityResult: ${screenCls.simpleName} | requestCode = $requestCode | resultCode = $resultCode")

    isScreenShown = true
    currentActivityRef = WeakReference(activity)

    val binders = getCurrentBindersByMainScreen(screenCls)
    binders.forEach { binder ->
        binder.onActivityResult(requestCode, resultCode, data)
    }

    callForAllChildBinders(screenCls) { binder ->
        binder.onActivityResult(requestCode, resultCode, data)
    }
}

fun ScreensAdapter<*, *>.onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, screenCls: Class<*>) {
    Log.w(TAG, "onRequestPermissionsResult: ${screenCls.simpleName} | requestCode = $requestCode | grantResults = $grantResults")

    val binders = getCurrentBindersByMainScreen(screenCls)
    binders.forEach { binder ->
        binder.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    callForAllChildBinders(screenCls) { binder ->
        binder.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

fun ScreensAdapter<*, *>.onBackPressed() {
    Log.w(TAG, "screenSettingsStack: $screenSettingsStack")

    if (onBackPressedListener != null) {
        onBackPressedListener?.invoke()
        return
    }
    if (screenSettingsStack.isNotEmpty()) {
        backToPreviousScreenOrClose()
    }
}

fun ScreensAdapter<*, *>.onOptionsItemSelected(item: MenuItem): Boolean {
    return onOptionsItemSelectedListener?.invoke(item) ?: false
}

fun ScreensAdapter<*, *>.onPrepareOptionsMenu(menu: Menu?): Boolean {
    return onPrepareOptionsMenuListener?.invoke(menu) ?: true
}

private fun ScreensAdapter<*, *>.getCurrentBindersByMainScreen(mainScreenCls: Class<*>): Collection<BaseAndroidScreenBinder> {
    if (screenSettingsStack.isEmpty()) {
        return emptyList()
    }

    val screens = getSupportScreensByMainScreen(mainScreenCls)
    val allBinders = ArrayList<BaseAndroidScreenBinder>()
    screens.forEach { screen ->
        val bindersForScreen = getBindersByScreen(screen.javaClass)

        bindersForScreen.forEach { newBinder ->
            var isUnique = true
            allBinders.forEach { binder ->
                if (binder.javaClass == newBinder.javaClass) {
                    isUnique = false
                }
            }

            if (isUnique) {
                allBinders.add(newBinder)
            }
        }
    }

    return allBinders
}

private fun ScreensAdapter<*, *>.getSupportScreensByMainScreen(mainScreenCls: Class<*>): Collection<Any> {
    return supportScreensByMainScreenClsMap[mainScreenCls]
            ?: throw NullPointerException("Init supportScreens before use")
}

private fun ScreensAdapter<*, *>.getBindersByScreen(screenCls: Class<*>): Collection<BaseAndroidScreenBinder> {
    return bindersByScreenClsMap[screenCls]
            ?: throw NullPointerException("screenCls:${screenCls.simpleName} | Array of binders must not be null!")
}