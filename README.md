# ScreensAdapter
Adapter for application screens.

## Init
```kotlin
// todo: add an example
```

## Using

```kotlin
@BindScreen(screen = SplashScreen::class)
class SplashBinder(adapter: MvpScreensAdapter<*, *>) : ScreenBinder<SplashScreen>(adapter),
    SplashScreen.UserAgent,
    SplashScreen.SystemAgent {

    companion object {
        fun createScreenSettings() = MvpScreenSettings(
            presenterCls = SplashScreen::class,
            layoutId = R.layout.screen_splash,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            themeId = R.style.AppTheme_NoActionBar,
            finishOnNextScreen = true
        )
    }

    override fun onBind() {
        screen.userAgent = this
        screen.systemAgent = this
        screen.app = screensAdapter.getApp() as MelangeApp

        screen.onBind()
    }

    override fun onShow() {
        screen.onShow()
    }

    override fun onHide() {
        screen.onHide()
        super.onHide()
    }

    // ...
}
```

```kotlin
@Screen
class SplashScreen(var nothing: Any?, var parent: Any?) {

    interface UserAgent : BaseScreen.UserAgent {
        fun showClockwiseAnimation()
        fun hideAllAnimations()

        fun showFaqScreen()
        fun showScenarioScreen()
    }

    interface SystemAgent : BaseScreen.SystemAgent {
        fun initViews()
    }

    lateinit var userAgent: UserAgent
    lateinit var systemAgent: SystemAgent
    lateinit var app: MelangeApp
    lateinit var server: RequestManager

    private var animationJob: Job? = null

    fun onBind() {
        systemAgent.initViews()
        userAgent.showClockwiseAnimation()
    }

    fun onShow() {
        animationJob = systemAgent.runOnBackgroundThread {
            delay(1000) //NOTE: что бы показать анимацию

            systemAgent.runOnUiThread {
                if (app.isFaqFinished()) {
                    userAgent.showScenarioScreen()

                } else {
                    userAgent.showFaqScreen()
                }
            }
        }
    }

    fun onHide() {
        animationJob?.cancel()
        userAgent.hideAllAnimations()
    }
}
```

## Download (Gradle)

```groovy
repositories {
    maven { url "http://dl.bintray.com/e16din/maven" }
}

dependencies {

    kapt "com.github.e16din:screensadaptermvp-compiler:0.9.51"
    implementation "com.github.e16din:screensadaptermvp-annotation:0.9.51"
    implementation "com.github.e16din:screensadaptermvp:0.9.51"
}
```
