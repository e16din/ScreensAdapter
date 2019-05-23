package com.e16din.screensadapter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.ScreensAdapterApplication



class BaseFragment : Fragment() {

    companion object {
        const val KEY_FRAGMENT_ID = "KEY_FRAGMENT_ID"
        const val KEY_SCREEN_CLS = "KEY_SCREEN_CLS"
        const val KEY_LAYOUT_ID = "KEY_LAYOUT_ID"
        const val KEY_HAS_OPTIONS_MENU = "KEY_HAS_OPTIONS_MENU"

        fun create(screenCls: Class<*>,
                   layoutId: Int,
                   hasOptionsMenu: Boolean = false,
                   fragmentId: Long = -1): BaseFragment {
            val fragment = BaseFragment()
            val bundle = Bundle()

            bundle.putLong(KEY_FRAGMENT_ID, fragmentId)
            bundle.putSerializable(KEY_SCREEN_CLS, screenCls)
            bundle.putInt(KEY_LAYOUT_ID, layoutId)
            bundle.putBoolean(KEY_HAS_OPTIONS_MENU, hasOptionsMenu)
            fragment.arguments = bundle
            return fragment
        }
    }

    private val screensAdapter: ScreensAdapter<*, *>?
        get() = (activity?.application as ScreensAdapterApplication?)?.screensAdapter

    lateinit var screenCls: Class<*>

    var fragmentId: Long = -1

    override fun onCreateView(inflater: LayoutInflater, vContainer: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentId = arguments!!.getLong(KEY_FRAGMENT_ID)
        screenCls = arguments!!.getSerializable(KEY_SCREEN_CLS) as Class<*>

        val hasOptionsMenu = arguments!!.getBoolean(KEY_HAS_OPTIONS_MENU)
        setHasOptionsMenu(hasOptionsMenu)

        val layoutId = arguments!!.getInt(KEY_LAYOUT_ID)
        return inflater.inflate(layoutId, vContainer, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        screensAdapter?.onFragmentCreate(screenCls, fragmentId)
    }

    override fun onResume() {
        super.onResume()
        screensAdapter?.onFragmentFocus(screenCls, fragmentId)
    }

    override fun onPause() {
        screensAdapter?.onFragmentLostFocus(screenCls, fragmentId)
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        screensAdapter?.onFragmentStart(screenCls, fragmentId)
    }

    override fun onStop() {
        screensAdapter?.onFragmentStop(screenCls, fragmentId)
        super.onStop()
    }

    override fun onDestroy() {
        screensAdapter?.onFragmentDestroy(screenCls, fragmentId)
        super.onDestroy()
    }

    fun onFragmentSelected() {
        screensAdapter?.onFragmentSelected(screenCls, fragmentId)
    }

    override fun onDetach() {
        super.onDetach()
        try {
            val childFragmentManager = Fragment::class.java
                    .getDeclaredField("mChildFragmentManager")
            childFragmentManager.isAccessible = true
            childFragmentManager.set(this, null)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }
}