package com.e16din.screensadapter.activities

import android.os.Bundle
import com.e16din.screensadapter.ScreensAdapter

class FirstActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ScreensAdapter.get.run {
            // NOTE: Create once fore all project
            // NOTE: First activity may be created before Application object
            baseActivitySymbiont = createActivitySymbiont()

            prepareScreen(items.first())
        }
        super.onCreate(savedInstanceState)
    }
}