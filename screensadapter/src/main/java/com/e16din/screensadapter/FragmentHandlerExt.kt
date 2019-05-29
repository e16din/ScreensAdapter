package com.e16din.screensadapter

import android.util.Log
import com.e16din.screensadapter.binders.android.FragmentScreenBinder


private const val TAG = "SA.FragmentHandler"

fun ScreensAdapter<*, *>.onFragmentStart(fragmentScreenCls: Class<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}.onFragmentStart() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}")
    binder.counter += 1
    binder.onShow()
}

fun ScreensAdapter<*, *>.onFragmentFocus(fragmentScreenCls: Class<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}.onFragmentFocus() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}")
    binder.onFocus()
}

fun ScreensAdapter<*, *>.onFragmentSelected(fragmentScreenCls: Class<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}.onFragmentSelected() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}")
    binder.onSelectedInPager()
}

fun ScreensAdapter<*, *>.onFragmentLostFocus(fragmentScreenCls: Class<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}.onFragmentLostFocus() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}")
    binder.onLostFocus()
}

fun ScreensAdapter<*, *>.onFragmentStop(fragmentScreenCls: Class<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)

    Log.d(TAG, "     ${fragmentScreenCls.simpleName}.onFragmentStop() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}")
    binder.onHide()
}

fun ScreensAdapter<*, *>.onFragmentCreate(fragmentScreenCls: Class<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}.onFragmentCreate() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}")
    binder.onBind()
}

private fun ScreensAdapter<*, *>.getFragmentBinderByIdMap(fragmentScreenCls: Class<*>, fragmentId: Long): FragmentScreenBinder<*> {
    if (fragmentScreenCls.name == "java.lang.Object" && fragmentId <= 0) {
        throw NullPointerException(createFragmentBindersExceptionData(fragmentScreenCls, fragmentId) +
                "NOTE: try to use  the screensAdapter.createFragment(...) method to create any fragment")
    }
    return fragmentBindersMap[fragmentId]?.second as FragmentScreenBinder<*>?
            ?: throw NullPointerException(createFragmentBindersExceptionData(fragmentScreenCls, fragmentId))
}

private fun createFragmentBindersExceptionData(fragmentScreenCls: Class<*>, fragmentId: Long): String {
    return "data:\n" +
            "[ fragmentScreenCls = ${fragmentScreenCls.name} ]\n" +
            "[ fragmentId = $fragmentId ]\n"
}