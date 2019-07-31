package com.e16din.screensadapter.activities

import android.os.Bundle
import com.e16din.screensadapter.helpers.ActivityUtils

class TranslucentActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ActivityUtils.convertActivityToTranslucent(this)
//        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        super.onCreate(savedInstanceState)
    }
}