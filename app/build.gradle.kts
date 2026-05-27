import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Release 签名：优先读项目根目录下的 `keystore.properties`（已加入 .gitignore），
 * 没有的话退回到 debug 的自动签名 keystore。这样：
 *   - 平时本地 `./gradlew assembleRelease` 也能产出"可装的" APK，方便试 Release 包。
 *   - 真要发布上线，把以下 4 个键放进 keystore.properties（参考 keystore.properties.example）
 *     并把 keystore 文件放到该路径下，AGP 会自动接管签名。
 *
 * keystore.properties 示例：
 *   storeFile=/Users/you/.android/workscribe-release.jks
 *   storePassword=...
 *   keyAlias=...
 *   keyPassword=...
 */
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps: Properties? = if (keystorePropsFile.exists()) {
    Properties().apply { keystorePropsFile.inputStream().use { load(it) } }
} else null

/** 版本号单一来源：根目录 [version.properties]（可用 scripts/bump-version.sh 更新）。 */
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    check(versionPropsFile.exists()) {
        "缺少 ${versionPropsFile.path}，请参考 version.properties 或运行 scripts/bump-version.sh"
    }
    versionPropsFile.inputStream().use { load(it) }
}
val appVersionCode = versionProps.getProperty("VERSION_CODE")?.toIntOrNull()
    ?: error("version.properties 缺少有效的 VERSION_CODE")
val appVersionName = versionProps.getProperty("VERSION_NAME")?.trim().orEmpty()
    .ifEmpty { error("version.properties 缺少 VERSION_NAME") }

android {
    namespace = "club.cyxc.workscribe"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "club.cyxc.workscribe"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 有 keystore.properties 用正式 release keystore；没有就退回 debug keystore，
            // 让本地 `assembleRelease` 也能装上设备测试 Release 包。
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
}

// Release APK 命名：WorkScribe-{versionName}.apk（AGP 9 无 applicationVariants，在打包后重命名）
afterEvaluate {
    tasks.named("assembleRelease") {
        doLast {
            val releaseDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
            val target = File(releaseDir, "WorkScribe-$appVersionName.apk")
            releaseDir.listFiles()?.filter { it.extension == "apk" && it.name != target.name }?.forEach { apk ->
                apk.copyTo(target, overwrite = true)
                apk.delete()
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
