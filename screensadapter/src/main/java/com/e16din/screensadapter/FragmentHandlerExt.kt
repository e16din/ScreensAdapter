package com.e16din.screensadapter

import android.util.Log
import com.e16din.screensadapter.binders.android.BaseAndroidScreenBinder
import com.e16din.screensadapter.binders.android.FragmentScreenBinder


private const val TAG = "SA.FragmentHandler"

fun ScreensAdapter<*, *>.onFragmentStart(screenCls: Class<*>, fragmentId: Long) {
    Log.d(TAG, "     onFragmentStart: ${screenCls.simpleName}")
    val fragmentBindersById =
            getFragmentBindersByIdMap(screenCls, fragmentId)

    fragmentBindersById[fragmentId]?.forEach { binder ->
        binder.onShow()
    }
}

fun ScreensAdapter<*, *>.onFragmentFocus(screenCls: Class<*>, fragmentId: Long) {
    Log.d(TAG, "     onFragmentFocus: ${screenCls.simpleName}")
    val fragmentBindersById =
            getFragmentBindersByIdMap(screenCls, fragmentId)

    fragmentBindersById[fragmentId]?.forEach { binder ->
        binder.onFocus()
    }
}

fun ScreensAdapter<*, *>.onFragmentSelected(screenCls: Class<*>, fragmentId: Long) {
    Log.d(TAG, "     onFragmentSelected: ${screenCls.simpleName}")
    val fragmentBindersById =
            getFragmentBindersByIdMap(screenCls, fragmentId)

    fragmentBindersById[fragmentId]?.forEach { binder ->
        (binder as FragmentScreenBinder<*>).onSelectedInPager()
    }
}

fun ScreensAdapter<*, *>.onFragmentLostFocus(screenCls: Class<*>, fragmentId: Long) {
    Log.d(TAG, "     onFragmentLostFocus: ${screenCls.simpleName}")
    val fragmentBindersById =
            getFragmentBindersByIdMap(screenCls, fragmentId)

    fragmentBindersById[fragmentId]?.forEach { binder ->
        binder.onLostFocus()
    }
}

fun ScreensAdapter<*, *>.onFragmentStop(screenCls: Class<*>, fragmentId: Long) {
    Log.d(TAG, "     onFragmentStop: ${screenCls.simpleName}")
    val fragmentBindersById =
            getFragmentBindersByIdMap(screenCls, fragmentId)

    fragmentBindersById[fragmentId]?.forEach { binder ->
        binder.onHide()
    }
}

fun ScreensAdapter<*, *>.onFragmentCreate(screenCls: Class<*>, fragmentId: Long) {
    Log.d(TAG, "     onFragmentCreate: ${screenCls.simpleName}")
    val fragmentBindersById =
            getFragmentBindersByIdMap(screenCls, fragmentId)

    fragmentBindersById[fragmentId]?.forEach { binder ->
        binder.onBind()
    }
}

fun ScreensAdapter<*, *>.onFragmentDestroy(screenCls: Class<*>, fragmentId: Long) {
    fragmentBindersByScreenClsMap[screenCls.javaClass]?.clear()
}

private fun ScreensAdapter<*, *>.getFragmentBindersByIdMap(screenCls: Class<*>, fragmentId: Long): MutableMap<Long, List<BaseAndroidScreenBinder>> {
    if (screenCls.name == "java.lang.Object" && fragmentId <= 0) {
        throw NullPointerException(createFragmentBindersExceptionData(screenCls, fragmentId) +
                "NOTE: try to use  the screensAdapter.createFragment(...) method to create any fragment")
    }
    return fragmentBindersByScreenClsMap[screenCls]
            ?: throw NullPointerException(createFragmentBindersExceptionData(screenCls, fragmentId))
}

private fun createFragmentBindersExceptionData(screenCls: Class<*>, fragmentId: Long): String {
    return "data:\n" +
            "[ screenCls = ${screenCls.name} ]\n" +
            "[ fragmentId = $fragmentId ]\n"
}