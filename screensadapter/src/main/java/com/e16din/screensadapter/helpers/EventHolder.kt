package com.e16din.screensadapter.helpers

import android.util.Log


typealias EventListener<DATA> = ((data: DATA?) -> Unit)

class EventHolder<DATA, RESULT>(
        var listeners: ArrayList<EventListener<DATA>> = ArrayList(),
        var data: DATA? = null,
        var result: RESULT? = null,
        var name: String? = null
) {
    fun invoke(newData: DATA? = null): RESULT? {
        Log.w("EH.debug", "$name")

        if (newData != null) {
            data = newData
        }

        listeners.forEachIndexed { index, function ->
            Log.w("EH.debug", "$name: $index")
            function.invoke(data)
        }

        return result
    }
}

fun <DATA> EventHolder<DATA, *>.setListener(function: (data: DATA?) -> Unit) {
    listeners.clear()
    addListener(function)
}

fun EventHolder<*, *>.reset() {
    data = null
    result = null
    listeners.clear()
}

fun <DATA> EventHolder<DATA, *>.addListener(function: (data: DATA?) -> Unit) {
    listeners.add(function)
}

