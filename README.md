# ScreensAdapter
An adapter for application screens that allows you to create screens programmatically and avoid using AndroidManifest.xml

## Init
```kotlin
class App : MultiDexApplication(),
     MvpScreensAdapterApplication,
     YourApp {

    override val screensAdapter: MvpScreensAdapter<*, *> by lazy {
        GeneratedScreensAdapter(this, this, RequestManager)
    }

    override fun onCreate() {
        super.onCreate()

        val screenSettings = SplashBinder.createScreenSettings()
        screensAdapter.items.add(screenSettings)
        screensAdapter.start()
    }
}
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
        screen.app = screensAdapter.getApp() as YourApp

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
        fun showFaqScreen()
        fun showScenarioScreen()
    }

    interface SystemAgent : BaseScreen.SystemAgent {
        fun initViews()
    }

    lateinit var userAgent: UserAgent
    lateinit var systemAgent: SystemAgent
    lateinit var app: YourApp
    lateinit var server: RequestManager


    fun onBind() {
        systemAgent.initViews()
    }

    fun onShow() {
        if (app.isFaqFinished()) {
            userAgent.showScenarioScreen()

        } else {
            userAgent.showFaqScreen()
        }
    }

    fun onHide() {
        // do nothing
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
