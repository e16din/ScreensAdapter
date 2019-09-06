package com.e16din.screensadapter.helpers

import android.util.Log

class EventHolder<D, R>(
    var it: ((data: D?) -> R)? = null,
    var resultData: D? = null,
    var name: String? = null
) {
    fun invoke(newData: D? = null): R? {
        Log.w("Event", "$name")

        if (newData != null) {
            resultData = newData
        }
        return it?.invoke(resultData)
    }

    fun getCommonDataProvider(): () -> D? = { resultData }
}

fun <D, R> EventHolder<D, R>.setListener(function: ((data: D?) -> R)?) {
    this.it = function
    this.resultData = null
}

fun <D, R> EventHolder<D, R>.clearAll() = setListener(null)

fun <D, R> EventHolder<D, R>.addListener(function: (data: D?) -> R) {
    val previousFunc = it
    it = combineFunctions(function, previousFunc, false, getCommonDataProvider())
}

fun <D, R> combineFunctions(
    newFunc: (data: D?) -> R,
    currFunc: ((data: D?) -> R)?,
    callNewBeforeCurrent: Boolean,
    dataProvider: () -> D?
): ((data: D?) -> R)? = {
    val commonData = dataProvider.invoke()

    if (callNewBeforeCurrent) {
        val newFuncResult = newFunc.invoke(commonData)
        currFunc?.invoke(commonData) ?: newFuncResult

    } else {
        currFunc?.invoke(commonData)
        newFunc.invoke(commonData)
    }
}