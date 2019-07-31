package com.e16din.screensadapter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.e16din.screensadapter.*
import kotlin.reflect.KClass


class BaseFragment : Fragment() {

    companion object {
        const val KEY_FRAGMENT_ID = "KEY_FRAGMENT_ID"
        const val KEY_SCREEN_CLS = "KEY_SCREEN_CLS"
        const val KEY_LAYOUT_ID = "KEY_LAYOUT_ID"
        const val KEY_HAS_OPTIONS_MENU = "KEY_HAS_OPTIONS_MENU"

        fun create(screenCls: KClass<*>,
                   layoutId: Int,
                   hasOptionsMenu: Boolean = false,
                   fragmentScreenId: Int = -1): BaseFragment {
            val fragment = BaseFragment()
            val bundle = Bundle()

            bundle.putInt(KEY_FRAGMENT_ID, fragmentScreenId)
            bundle.putSerializable(KEY_SCREEN_CLS, screenCls.java)
            bundle.putInt(KEY_LAYOUT_ID, layoutId)
            bundle.putBoolean(KEY_HAS_OPTIONS_MENU, hasOptionsMenu)
            fragment.arguments = bundle
            return fragment
        }
    }

    private val screensAdapter: ScreensAdapter<*, *>?
        get() = (activity?.application as ScreensAdapterApplication?)?.screensAdapter

    lateinit var screenCls: KClass<*>

    var fragmentId: Int = -1

    private var isAlreadyStopped: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, vContainer: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentId = arguments!!.getInt(KEY_FRAGMENT_ID)
        screenCls = (arguments!!.getSerializable(KEY_SCREEN_CLS) as Class<*>).kotlin

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
        isAlreadyStopped = false
        screensAdapter?.onFragmentStart(screenCls, fragmentId)
    }

    override fun onDestroyView() {
        if (!isAlreadyStopped) {
            isAlreadyStopped = true
            screensAdapter?.onFragmentStop(this, screenCls, fragmentId)
        }
        super.onDestroyView()
    }

    override fun onStop() {
        if (!isAlreadyStopped) {
            isAlreadyStopped = true
            screensAdapter?.onFragmentStop(this, screenCls, fragmentId)
        }
        super.onStop()
    }

    fun onFragmentDeselected() {
        screensAdapter?.onFragmentDeselected(screenCls, fragmentId)
    }

    fun onFragmentSelected() {
        screensAdapter?.onFragmentSelected(screenCls, fragmentId)
    }
}