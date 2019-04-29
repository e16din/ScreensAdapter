package com.e16din.screensadapter.fragments

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

open class RichViewPager @kotlin.jvm.JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : ViewPager(context, attrs) {

    interface AdapterInterface {
        fun getFragment(position: Int): BaseFragment?
    }

    private val onPageChangeListener = object : SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            adapter?.let {
                val fragment = (adapter as AdapterInterface).getFragment(position)
                fragment?.onFragmentSelected()
            }
        }
    }

    override fun setAdapter(adapter: PagerAdapter?) {
        when (adapter) {
            null -> {
                removeOnPageChangeListener(onPageChangeListener)
                super.setAdapter(null)
            }
            is AdapterInterface -> {
                super.setAdapter(adapter)
                addOnPageChangeListener(onPageChangeListener)
            }
            else -> throw IllegalStateException("Please, use the AdapterInterface")
        }
    }
}