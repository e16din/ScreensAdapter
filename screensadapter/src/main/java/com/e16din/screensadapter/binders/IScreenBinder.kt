package com.e16din.screensadapter.binders


interface IScreenBinder {

    var counter: Int // increments on binder.onShow()

    fun isEnabled() = true //todo: handle it on lifecycle methods calls

    fun onPrepare() {}

    fun onBind() {}

    fun onShow() {}

    fun onFocus() {}

    fun onNextScreen() {}

    fun onLostFocus() {}

    fun onHide() {}

    fun initScreen(screen: Any)
}