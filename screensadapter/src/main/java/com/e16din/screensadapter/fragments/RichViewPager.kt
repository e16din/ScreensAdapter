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

    protected val onPageChangeListener = object : SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            adapter?.let {
                val adapterInterface = adapter as AdapterInterface
                val fragment = adapterInterface.getFragment(position)
                fragment?.onFragmentSelected()

                if (position - 1 >= 0) {
                    val fragmentPrev = adapterInterface.getFragment(position - 1)
                    fragmentPrev?.onFragmentDeselected()
                }

                if (position + 1 < adapter?.count ?: 0) {
                    val fragmentNext = adapterInterface.getFragment(position + 1)
                    fragmentNext?.onFragmentDeselected()
                }
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
                addOnPageChangeListener(onPageChangeListener)
                super.setAdapter(adapter)

                post {
                    onPageChangeListener.onPageSelected(currentItem)
                }
            }
            else -> throw IllegalStateException("Please, use the AdapterInterface")
        }
    }
}