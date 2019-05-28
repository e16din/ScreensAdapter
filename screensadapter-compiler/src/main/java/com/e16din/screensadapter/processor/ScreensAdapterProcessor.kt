package com.e16din.screensadapter.processor

import com.e16din.screensadapter.annotation.BindScreen
import com.e16din.screensadapter.annotation.model.App
import com.e16din.screensadapter.annotation.model.Screen
import com.e16din.screensadapter.annotation.model.Server
import com.e16din.screensadapter.processor.ScreensAdapterProcessor.Companion.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.squareup.kotlinpoet.*
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.tools.Diagnostic


//@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ScreensAdapterProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        private const val SCREENS_ADAPTER_PACKAGE = "com.e16din.screensadapter"

        private const val PADDING_1 = "  "
        private const val PADDING_2 = "    "
        private const val PADDING_3 = "          "

        private val CLS_ANDROID_APPLICATION =
                ClassName("android.app", "Application")
        private val CLS_BASE_ANDROID_SCREEN_BINDER =
                ClassName("com.e16din.screensadapter.binders", "IScreenBinder")
    }

    private lateinit var filer: Filer
    private lateinit var messager: Messager

    private var generated = false

    private var appClassName: String? = null
    private var serverClassName: String? = null

    private var screens = arrayListOf<String>()
    // NOTE: Binder -> Screen
    private var screenByBinderMap = hashMapOf<String, String>()
    // NOTE: Screen -> Binder
    private var binderByScreenMap = hashMapOf<String, String>()
    // NOTE: Screen -> DataType
    private var screenDataTypeByScreenMap = hashMapOf<String, String>()
    // NOTE: Screen -> ParentType
    private var screenParentTypeByScreenMap = hashMapOf<String, String>()

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        val annotations = HashSet<String>()
        annotations.add(App::class.java.canonicalName)
        annotations.add(Server::class.java.canonicalName)
        annotations.add(Screen::class.java.canonicalName)
        annotations.add(BindScreen::class.java.canonicalName)

        return annotations
    }

    @Synchronized
    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        filer = processingEnvironment.filer
        messager = processingEnvironment.messager
    }

    override fun process(set: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (generated) {
            return false
        }

        "process()".print()

        processApp(roundEnv)
        processServer(roundEnv)
        processScreens(roundEnv)
        processBinders(roundEnv)

        if (appClassName != null
                && serverClassName != null) {

            makeScreenAdapter(SCREENS_ADAPTER_PACKAGE)
            generated = true
            return true
        }

        throw IllegalStateException("GeneratedScreensAdapter is empty")
    }

    private fun processServer(roundEnv: RoundEnvironment) {
        roundEnv.getElementsAnnotatedWith(Server::class.java)
                .firstOrNull()
                ?.run {
                    serverClassName = this.getFullName()
                    "serverClassName: $serverClassName".print()
                }
    }

    private fun processApp(roundEnv: RoundEnvironment) {
        roundEnv.getElementsAnnotatedWith(App::class.java)
                .firstOrNull()
                ?.run {
                    appClassName = this.getFullName()
                    "appClassName: $appClassName".print()
                }
    }

    private fun processScreens(roundEnv: RoundEnvironment) {
        ">> processScreens:".print()
        val binderElements =
                roundEnv.getElementsAnnotatedWith(Screen::class.java)

        binderElements.forEach { element ->
            val annotation = element.getAnnotation(Screen::class.java)
            var dataTypeName = try {
                annotation.data.qualifiedName
            } catch (e: MirroredTypeException) {
                e.typeMirror.toString()
            }

            val isDataNullable = annotation.isDataNullable
            if (isDataNullable) {
                dataTypeName = "$dataTypeName?"
            }

            val parentTypeName = try {
                annotation.parent.qualifiedName
            } catch (e: MirroredTypeException) {
                e.typeMirror.toString()
            }

            val screenName = element.getFullName()
            screenDataTypeByScreenMap[screenName] = dataTypeName!!
            screenParentTypeByScreenMap[screenName] = parentTypeName!!
        }
    }

    private fun processBinders(roundEnv: RoundEnvironment) {
        ">> processBinders:".print()
        val binderElements =
                roundEnv.getElementsAnnotatedWith(BindScreen::class.java)

        binderElements.forEach { element ->
            val screenName = try {
                element.getAnnotation(BindScreen::class.java).screen.qualifiedName
            } catch (e: MirroredTypeException) {
                e.typeMirror.toString()
            }!!

            val annotationMirror = element.annotationMirrors.first()
            val mirrorAsStr = annotationMirror.toString()
            mirrorAsStr.print()

            screens.add(screenName)

            if (!isObjectClass(screenName)) {
                val binderName = element.getFullName()
                screenByBinderMap[binderName] = screenName
                binderByScreenMap[screenName] = binderName
            }
        }
        screenByBinderMap.forEach { binderName, screensNames ->
            "$binderName: $screensNames".print()
        }
    }

    private fun isObjectClass(className: String?): Boolean {
        return className!!.endsWith(".Object.class")
                || className.endsWith(".Object::class")
                || className.endsWith(".Object::class.java")
                || className.endsWith(".Object")
    }

    private fun Element.getFullName() =
            "${processingEnv.elementUtils.getPackageOf(this)}.${this.simpleName}"

    private fun makeScreenAdapter(pkg: String) {
        val fileName = "GeneratedScreensAdapter"

        val generatedSourcesRoot = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
                .orEmpty()
        if (generatedSourcesRoot.isEmpty()) {
            "Can't find the target directory for generated Kotlin files."
                    .printError()
            return
        }

        val file = File(generatedSourcesRoot).apply { mkdir() }

        val screensAdapterTypeSpec = TypeSpec.classBuilder(fileName)
                .addConstructor()
                .addGeneratedScreensFunc()
                .addGeneratedBindersFunc()
                .build()

        FileSpec.builder(pkg, fileName)
                .addType(screensAdapterTypeSpec)
                .build()
                .writeTo(file)
    }

    private fun (TypeSpec.Builder).addGeneratedBindersFunc(): TypeSpec.Builder {
        "addGeneratedBindersFunc:".print()

        val screenClsParamName = "screenCls"

        var bindersObjectsCode = ""

        screens.forEach { screenName ->
            val binderName = binderByScreenMap[screenName]

            bindersObjectsCode += "$PADDING_2$screenName::class.java -> " +
                    "$PADDING_3$binderName(this)" +
                    "\n$PADDING_2\n"
        }

        val funcSpec = FunSpec.builder("makeBinder")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(screenClsParamName, ClassName.bestGuess("Class<*>"))
                .addStatement(
                        "${PADDING_1}return when ($screenClsParamName) {\n" +
                                bindersObjectsCode +
                                "${PADDING_2}else -> throw createNotFoundException($screenClsParamName.simpleName)\n" +
                                "$PADDING_1}")
                .returns(CLS_BASE_ANDROID_SCREEN_BINDER)
                .build()

        return this.addFunction(funcSpec)
    }

    private fun (TypeSpec.Builder).addGeneratedScreensFunc(): TypeSpec.Builder {
        val screenClsParamName = "screenCls"
        val dataParamName = "data"
        val parentParamName = "parent"


        var screensObjectsCode = ""
        screens.forEach { screenName ->

            var dataType = screenDataTypeByScreenMap[screenName]
            dataType = normalizeType(dataType)
            val data = if (dataType.equals("java.lang.Object"))
                dataParamName
            else
                "$dataParamName as $dataType"

            val parentType = screenParentTypeByScreenMap[screenName]
            val parent = if (parentType.equals("java.lang.Object"))
                parentParamName
            else
                "$parentParamName as $parentType"

            screensObjectsCode += "$PADDING_2$screenName::class.java -> \n" +
                    "restoreScreen(\"$screenName\") ?: $PADDING_3$screenName($data, $parent)" +
                    "$PADDING_2\n"
        }

        val funcSpec = FunSpec.builder("makeScreen")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(screenClsParamName, ClassName.bestGuess("Class<*>"))
                .addParameter(dataParamName, ClassName.bestGuess("Any?"))
                .addParameter(parentParamName, ClassName.bestGuess("Any?"))
                .addStatement(
                        "${PADDING_1}return when ($screenClsParamName) {\n" +
                                screensObjectsCode +
                                "${PADDING_2}else -> throw createNotFoundException($screenClsParamName.simpleName)\n" +
                                "$PADDING_1}")
                .returns(ClassName.bestGuess("Any"))
                .build()

        return this.addFunction(funcSpec)
    }

    private fun normalizeType(dataType: String?): String? {
        return when (dataType) {
            "int" -> "Int"
            "long" -> "Long"
            "double" -> "Double"
            "java.lang.String" -> "String"
            else -> dataType
        }
    }

    private fun (TypeSpec.Builder).addConstructor(): TypeSpec.Builder {
        val superClassName = ClassName(SCREENS_ADAPTER_PACKAGE,
                "ScreensAdapter<$appClassName, $serverClassName>")

        val androidAppParamName = "androidApp"
        val appParamName = "app"
        val serverParamName = "server"
        val delayForSplashMsParamName = "delayForSplashMs"

        return this.superclass(superClassName).addModifiers(KModifier.PUBLIC)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter(androidAppParamName, CLS_ANDROID_APPLICATION)
                        .addParameter(appParamName, ClassName.bestGuess(appClassName!!))
                        .addParameter(serverParamName, ClassName.bestGuess(serverClassName!!))
                        .addParameter(delayForSplashMsParamName, Long::class)
                        .build())
                .addSuperclassConstructorParameter(androidAppParamName)
                .addSuperclassConstructorParameter(appParamName)
                .addSuperclassConstructorParameter(serverParamName)
                .addSuperclassConstructorParameter(delayForSplashMsParamName)
    }

    private fun String.print() =
            messager.printMessage(Diagnostic.Kind.WARNING, this)

    private fun String.printError() =
            messager.printMessage(Diagnostic.Kind.ERROR, this)
}

private fun String.substringBetween(start: String, end: String): String? {
    val startIndex = this.indexOf(start) + start.length
    val endIndex = this.indexOf(end)

    return try {
        this.substring(startIndex, endIndex)
    } catch (e: StringIndexOutOfBoundsException) {
        null
    }
}


//class GeneratedScreensAdapter(
//        androidApp: Application,
//        app: PurchasesAppAgent,
//        server: PurchasesServerAgent,
//        delayForSplashMs: Long,
//        splashTheme: Int
//) : ScreensAdapter<com.e16din.purchases.app.PurchasesAppAgent, com.e16din.purchases.server.PurchasesServerAgent>(androidApp, app, server, delayForSplashMs, splashTheme) {
//    override fun makeScreen(mainScreenCls: Class<*>, data:Any?): Collection<Any> {
//        return when (mainScreenCls) {
//            com.e16din.purchases.screens.main.MainScreen::class.java -> arrayListOf(
//                    "MainScreen".getData<MainScreen>() ?: com.e16din.purchases.screens.main.MainScreen(data)
// )
//            com.e16din.purchases.screens.order.OrderScreen::class.java -> arrayListOf(
//                    com.e16din.purchases.screens.order.OrderScreen()    )
//            else -> throw createNotFoundException(mainScreenCls.simpleName)
//        }
//    }
//
//    override fun makeBinder(screenCls: Class<*>): Collection<BaseScreenBinder> {
//        return when (screenCls) {
//            com.e16din.purchases.screens.main.MainScreen::class.java -> arrayListOf(
//                    com.e16din.purchases.screens.main.MainBinder(this)
//            )
//            com.e16din.purchases.screens.order.OrderScreen::class.java -> arrayListOf(
//                    com.e16din.purchases.screens.order.OrderBinder(this)
//            )
//            else -> throw createNotFoundException(screenCls.simpleName)
//        }
//    }
//}