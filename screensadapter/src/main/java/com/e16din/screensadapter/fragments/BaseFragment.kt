package com.e16din.screensadapter.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.e16din.screensadapter.ScreensAdapter
import com.e16din.screensadapter.ScreensAdapterApplication

class BaseFragment : Fragment() {

    companion object {
        const val KEY_SCREEN_CLS = "KEY_SCREEN_CLS"
        const val KEY_LAYOUT_ID = "KEY_LAYOUT_ID"
        const val KEY_HAS_OPTIONS_MENU = "KEY_HAS_OPTIONS_MENU"

        fun create(screenCls: Class<*>, layoutId: Int, hasOptionsMenu: Boolean = false): BaseFragment {
            val fragment = BaseFragment()
            val bundle = Bundle()

            bundle.putSerializable(KEY_SCREEN_CLS, screenCls)
            bundle.putInt(KEY_LAYOUT_ID, layoutId)
            bundle.putBoolean(KEY_HAS_OPTIONS_MENU, hasOptionsMenu)
            fragment.arguments = bundle
            return fragment
        }
    }

    private val screensAdapter: ScreensAdapter<*, *>
        get() = (activity?.application as ScreensAdapterApplication).screensAdapter

    private lateinit var screenCls: Class<*>

    private var onFocusCalled = false

    override fun onCreateView(inflater: LayoutInflater, vContainer: ViewGroup?, savedInstanceState: Bundle?): View? {
        screenCls = arguments!!.getSerializable(KEY_SCREEN_CLS) as Class<*>

        val hasOptionsMenu = arguments!!.getBoolean(KEY_HAS_OPTIONS_MENU)
        setHasOptionsMenu(hasOptionsMenu)

        val layoutId = arguments!!.getInt(KEY_LAYOUT_ID)
        return inflater.inflate(layoutId, vContainer, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        screensAdapter.onFragmentCreate(screenCls)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (!isResumed) {
            return
        }

        if (isVisibleToUser) {
            if (!onFocusCalled) {
                screensAdapter.onFragmentFocus(screenCls)
            }
            onFocusCalled = false

        } else {
            screensAdapter.onFragmentLostFocus(screenCls)
        }
    }

    override fun onResume() {
        super.onResume()
        onFocusCalled = true
        screensAdapter.onFragmentFocus(screenCls)
    }

    override fun onStart() {
        super.onStart()
        screensAdapter.onFragmentStart(screenCls)
    }

    override fun onStop() {
        screensAdapter.onFragmentStop(screenCls)
        super.onStop()
    }
}