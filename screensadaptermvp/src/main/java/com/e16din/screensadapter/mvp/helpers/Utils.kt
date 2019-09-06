package com.e16din.screensadapter.mvp.helpers

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.e16din.screensadapter.mvp.binders.android.BaseAndroidScreenBinder

fun BaseAndroidScreenBinder<*>.showToastMessage(message: CharSequence) =
        Toast.makeText(this.screensAdapter.getAndroidApp(), message, Toast.LENGTH_SHORT).show()

fun BaseAndroidScreenBinder<*>.showToastMessageLong(message: CharSequence?) =
        Toast.makeText(this.screensAdapter.getAndroidApp(), message, Toast.LENGTH_LONG).show()

fun Int.getDrawable(context: Context): Drawable? {
    return ContextCompat.getDrawable(context, this)
}

fun FragmentManager.replaceNow(containerId: Int, fragment: Fragment, tag: String) {
    this.beginTransaction().replace(containerId, fragment, tag)
            .addToBackStack(null)
            .commit()
}

fun FragmentManager.addNow(containerId: Int, fragment: Fragment, tag: String, name: String?) {
    this.beginTransaction().add(containerId, fragment, tag)
            .addToBackStack(name)
            .commit()
}

fun FragmentManager.removeNow(fragment: Fragment) {
    this.beginTransaction().remove(fragment)
            .commit()
}

fun FragmentManager.removeNow(fragmentTag: String) {
    val fragment = this.findFragmentByTag(fragmentTag)
    fragment?.let {
        this.beginTransaction().remove(fragment)
                .commit()
    }
}

fun View.hideKeyboard() {
    val inputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

inline fun <K, V> Map<out K, V>.foreach(action: (Map.Entry<K, V>) -> Unit) {
    with(this.iterator()) {
        forEach {
            action.invoke(it)
        }
    }
}

