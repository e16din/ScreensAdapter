package com.e16din.screensadapter.mvp

import android.content.Intent
import android.util.Log
import androidx.fragment.app.Fragment
import com.e16din.screensadapter.mvp.binders.android.FragmentScreenBinder
import com.e16din.screensadapter.mvp.fragments.BaseFragment
import com.e16din.screensadapter.mvp.helpers.foreach
import kotlin.reflect.KClass


private const val TAG = "SA.Mvp.FragmentHandler"

fun MvpScreensAdapter<*, *>.onFragmentActivityResult(requestCode: Int,
                                                     resultCode: Int,
                                                     data: Intent?,
                                                     fragmentScreenCls: KClass<*>,
                                                     fragmentScreenId: Int) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentScreenId)
    val logPostfix = ".onFragmentActivityResult(requestCode = $requestCode, resultCode = $resultCode) | fragmentId = $fragmentScreenId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.counter += 1
    binder.onActivityResult(requestCode, resultCode, data)

    callForActualChildBinders(fragmentScreenId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onActivityResult(requestCode, resultCode, data)
    }
}

fun MvpScreensAdapter<*, *>.onFragmentStart(fragmentScreenCls: KClass<*>, fragmentScreenId: Int) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentScreenId)
    val logPostfix = ".onFragmentStart() | fragmentId = $fragmentScreenId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.counter += 1
    binder.onShow()

    callForActualChildBinders(fragmentScreenId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onShow()
    }
}

fun MvpScreensAdapter<*, *>.onFragmentFocus(fragmentScreenCls: KClass<*>, fragmentScreenId: Int) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentScreenId)
    val logPostfix = ".onFragmentFocus() | fragmentId = $fragmentScreenId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onFocus()

    callForActualChildBinders(fragmentScreenId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onFocus()
    }
}

fun MvpScreensAdapter<*, *>.onFragmentDeselected(fragmentScreenCls: KClass<*>, fragmentScreenId: Int) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentScreenId)
    val logPostfix = ".onFragmentDeselected() | fragmentId = $fragmentScreenId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onDeselectedInPager()

    callForActualChildBinders(fragmentScreenId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onDeselectedInPager()
    }
}

fun MvpScreensAdapter<*, *>.onFragmentSelected(fragmentScreenCls: KClass<*>, fragmentScreenId: Int) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentScreenId)
    val logPostfix = ".onFragmentSelected() | fragmentId = $fragmentScreenId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onSelectedInPager()

    callForActualChildBinders(fragmentScreenId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onSelectedInPager()
    }
}

fun MvpScreensAdapter<*, *>.onFragmentLostFocus(fragmentScreenCls: KClass<*>, fragmentScreenId: Int) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentScreenId)
    val logPostfix = ".onFragmentLostFocus() | fragmentId = $fragmentScreenId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onLostFocus()

    callForActualChildBinders(fragmentScreenId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onLostFocus()
    }
}

fun MvpScreensAdapter<*, *>.onFragmentStop(fragment: BaseFragment, fragmentScreenCls: KClass<*>, fragmentScreenId: Int) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentScreenId)
    val logPostfix = ".onFragmentStop() | fragmentId = $fragmentScreenId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onHide()

    fragmentChildBindersMap[fragmentScreenId]!!.foreach { (childScreenCls, childBinder) ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onHide()
        removeChildBinder(childScreenCls, fragmentScreenId)
    }

    clearFinishedBinders(fragment, fragmentScreenId, fragmentScreenCls)
}

private fun MvpScreensAdapter<*, *>.clearFinishedBinders(fragment: Fragment, fragmentScreenId: Int, fragmentScreenCls: KClass<*>) {
    if (finishedFragmentsIds.contains(fragmentScreenId)) {
        removeFragmentBinders(fragmentScreenCls, fragmentScreenId)

        clearChildFragments(fragment)
    }
}

private fun MvpScreensAdapter<*, *>.clearChildFragments(fragment: Fragment) {
    fragment.childFragmentManager.fragments.forEach {
        if (it is BaseFragment?) {
            val childFragment = it
            childFragment?.let {
                removeFragmentBinders(childFragment.screenCls, childFragment.fragmentId)
                clearChildFragments(childFragment)
            }

        } else {
            clearChildFragments(it)
        }
    }
}

private fun MvpScreensAdapter<*, *>.removeFragmentBinders(fragmentScreenCls: KClass<*>, fragmentScreenId: Int) {
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}.clearFinishedBinders() | binder.fragmentId = $fragmentScreenId)")
    fragmentBindersMap.remove(fragmentScreenId)
    fragmentChildBindersMap.remove(fragmentScreenId)
    finishedFragmentsIds.remove(fragmentScreenId)
}

fun MvpScreensAdapter<*, *>.onFragmentCreate(fragmentScreenCls: KClass<*>, fragmentScreenId: Int) {
    val binder = getFragmentBinderByIdMap(fragmentScreenCls, fragmentScreenId)
    val logPostfix = ".onFragmentCreate() | fragmentId = $fragmentScreenId | binder.fragmentId = ${binder.fragmentId}"
    Log.d(TAG, "     ${fragmentScreenCls.simpleName}$logPostfix")
    binder.onBind()

    callForActualChildBinders(fragmentScreenId) { childBinder ->
        Log.d(TAG, "     ${childBinder.javaClass.simpleName}$logPostfix")
        childBinder.onBind()
    }
}

private fun MvpScreensAdapter<*, *>.getFragmentBinderByIdMap(fragmentScreenCls: KClass<*>, fragmentScreenId: Int): FragmentScreenBinder<*> {
    if (fragmentScreenCls.qualifiedName == "java.lang.Object" && fragmentScreenId <= 0) {
        throw NullPointerException(createFragmentBindersExceptionData(fragmentScreenCls, fragmentScreenId) +
                "NOTE: try to use  the screensAdapter.createFragment(...) method to create any fragment")
    }
    return fragmentBindersMap[fragmentScreenId]?.second as FragmentScreenBinder<*>?
            ?: throw NullPointerException(createFragmentBindersExceptionData(fragmentScreenCls, fragmentScreenId))
}

private fun createFragmentBindersExceptionData(fragmentScreenCls: KClass<*>, fragmentScreenId: Int): String {
    return "data:\n" +
            "[ fragmentScreenCls = ${fragmentScreenCls.qualifiedName} ]\n" +
            "[ fragmentId = $fragmentScreenId ]\n"
}

private fun MvpScreensAdapter<*, *>.callForActualChildBinders(fragmentScreenId: Int, call: (FragmentScreenBinder<*>) -> Unit) {
    fragmentChildBindersMap[fragmentScreenId]!!.foreach { (childScreenCls, childBinder) ->
        call.invoke(childBinder as FragmentScreenBinder<*>)
    }
}