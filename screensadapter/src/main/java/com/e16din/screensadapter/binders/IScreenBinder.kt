package com.e16din.screensadapter.binders


interface IScreenBinder {

    var counter: Int // increments on binder.onShow()

    fun isEnabled() = true //todo: handle it on lifecycle methods calls

    fun onPrepare() {}

    fun onBind() {}

    fun onShow(counter: Int) {}

    fun onFocus(counter: Int) {}

    fun onNextScreen() {}

    fun onLostFocus(counter: Int) {}

    fun onHide(counter: Int) {}

    fun initScreen(screen: Any)
}