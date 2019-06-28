package com.e16din.screensadapter

import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import com.e16din.screensadapter.binders.android.FragmentScreenBinder
import com.e16din.screensadapter.fragments.BaseFragment
import kotlin.reflect.KClass


private const val TAG = "SA.FragmentHandler"

fun ScreensAdapter<*, *>.onFragmentActivityResult(requestCode: Int,
                                                  resultCode: Int,
                                                  data: Intent?,
                                                  fragmentScreenCls: KClass<*>,
                                                  fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    val logPostfix = ".onFragmentActivityResult(requestCode = $requestCode, resultCode = $resultCode) | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.counter += 1
    binder.onActivityResult(requestCode, resultCode, data)

    callForActualChildBinders(fragmentId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onActivityResult(requestCode, resultCode, data)
    }
}

fun ScreensAdapter<*, *>.onFragmentStart(fragmentScreenCls: KClass<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    val logPostfix = ".onFragmentStart() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.counter += 1
    binder.onShow()

    callForActualChildBinders(fragmentId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onShow()
    }
}

fun ScreensAdapter<*, *>.onFragmentFocus(fragmentScreenCls: KClass<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    val logPostfix = ".onFragmentFocus() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onFocus()

    callForActualChildBinders(fragmentId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onFocus()
    }
}

fun ScreensAdapter<*, *>.onFragmentDeselected(fragmentScreenCls: KClass<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    val logPostfix = ".onFragmentDeselected() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onDeselectedInPager()

    callForActualChildBinders(fragmentId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onDeselectedInPager()
    }
}

fun ScreensAdapter<*, *>.onFragmentSelected(fragmentScreenCls: KClass<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    val logPostfix = ".onFragmentSelected() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onSelectedInPager()

    callForActualChildBinders(fragmentId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onSelectedInPager()
    }
}

fun ScreensAdapter<*, *>.onFragmentLostFocus(fragmentScreenCls: KClass<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    val logPostfix = ".onFragmentLostFocus() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onLostFocus()

    callForActualChildBinders(fragmentId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onLostFocus()
    }
}

fun ScreensAdapter<*, *>.onFragmentStop(fragment: BaseFragment, fragmentScreenCls: KClass<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    val logPostfix = ".onFragmentStop() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onHide()

    callForActualChildBinders(fragmentId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onHide()
    }

    clearFinishedBinders(fragment, fragmentId, fragmentScreenCls)
}

private fun ScreensAdapter<*, *>.clearFinishedBinders(fragment: Fragment, fragmentId: Long, fragmentScreenCls: KClass<*>) {
    if (finishedFragmentsIds.contains(fragmentId)) {
        removeFragmentBinders(fragmentScreenCls, fragmentId)

        clearChildFragments(fragment)
    }
}

private fun ScreensAdapter<*, *>.clearChildFragments(fragment: Fragment) {
    fragment.childFragmentManager.fragments.forEach {
        val childFragment = it as BaseFragment?
        childFragment?.let {
            removeFragmentBinders(childFragment.screenCls, childFragment.fragmentId)
            clearChildFragments(childFragment)
        }
    }
}

private fun ScreensAdapter<*, *>.removeFragmentBinders(fragmentScreenCls: KClass<*>, fragmentId: Long) {
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}.clearFinishedBinders() | binder.fragmentId = $fragmentId)")
    fragmentBindersMap.remove(fragmentId)
    fragmentChildBindersMap.remove(fragmentId)
    finishedFragmentsIds.remove(fragmentId)
}

fun ScreensAdapter<*, *>.onFragmentCreate(fragmentScreenCls: KClass<*>, fragmentId: Long) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentId)
    val logPostfix = ".onFragmentCreate() | fragmentId = $fragmentId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onBind()

    callForActualChildBinders(fragmentId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onBind()
    }
}

private fun ScreensAdapter<*, *>.getFragmentBinderByIdMap(fragmentScreenCls: KClass<*>, fragmentId: Long): FragmentScreenBinder<*> {
    if (fragmentScreenCls.qualifiedName == "java.lang.Object" && fragmentId <= 0) {
        throw NullPointerException(createFragmentBindersExceptionData(fragmentScreenCls, fragmentId) +
                "NOTE: try to use  the screensAdapter.createFragment(...) method to create any fragment")
    }
    return fragmentBindersMap[fragmentId]?.second as FragmentScreenBinder<*>?
            ?: throw NullPointerException(createFragmentBindersExceptionData(fragmentScreenCls, fragmentId))
}

private fun createFragmentBindersExceptionData(fragmentScreenCls: KClass<*>, fragmentId: Long): String {
    return "data:\n" +
            "[ fragmentScreenCls = ${fragmentScreenCls.qualifiedName} ]\n" +
            "[ fragmentId = $fragmentId ]\n"
}

private fun ScreensAdapter<*, *>.callForActualChildBinders(fragmentId: Long, call: (FragmentScreenBinder<*>) -> Unit) {
    fragmentChildBindersMap[fragmentId]!!.forEach { (childScreenCls, childBinder) ->
        call.invoke(childBinder as FragmentScreenBinder<*>)
    }
}