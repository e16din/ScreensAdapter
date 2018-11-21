package com.e16din.screensadapter.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class SimpleFragment : Fragment() {

    companion object {
        const val KEY_LAYOUT_ID = "KEY_LAYOUT_ID"
        const val KEY_HAS_OPTIONS_MENU = "KEY_HAS_OPTIONS_MENU"

        fun create(layoutId: Int, hasOptionsMenu: Boolean = false): SimpleFragment {
            val fragment = SimpleFragment()
            val bundle = Bundle()
            bundle.putInt(KEY_LAYOUT_ID, layoutId)
            bundle.putBoolean(KEY_HAS_OPTIONS_MENU, hasOptionsMenu)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, vContainer: ViewGroup?, savedInstanceState: Bundle?): View? {
        val hasOptionsMenu = arguments!!.getBoolean(KEY_HAS_OPTIONS_MENU)
        setHasOptionsMenu(hasOptionsMenu)
        
        val layoutId = arguments!!.getInt(KEY_LAYOUT_ID)
        return inflater.inflate(layoutId, vContainer, false)

    }
}