import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "io.github.darkxace01.unroll"
    compileSdk = 37

    defaultConfig {
        // The public ID on Google Play (permanent once uploaded). Kept separate from the code's namespace
        // so the store listing doesn't point to the GitHub account. Same pattern as Pixnix.
        applicationId = "studio.greasepencil.unroll"
        // Android 10: scoped storage and MediaStore RELATIVE_PATH for saving slides to the gallery.
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    // Release signing reads keystore.properties at the project root (never committed). Without it,
    // release builds are simply unsigned, so anyone can still build from source.
    val keystoreFile = rootProject.file("keystore.properties")
    val releaseSigning = if (keystoreFile.exists()) {
        val keys = Properties().apply { keystoreFile.inputStream().use(::load) }
        signingConfigs.create("release") {
            storeFile = file(keys.getProperty("storeFile"))
            storePassword = keys.getProperty("storePassword")
            keyAlias = keys.getProperty("keyAlias")
            keyPassword = keys.getProperty("keyPassword")
        }
    } else {
        null
    }

    // "play" = Google Play build: tip jar (Play billing), the opt-in "support with an ad" and ML Kit
    // face finding for seam safety. "foss" = F-Droid build: none of those, and no INTERNET permission.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "SHOW_SOURCE_LINK", "false")
            // Google's test ad unit until the real one exists. Swap in the AdMob console's IDs before release.
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "REWARDED_AD_UNIT", "\"ca-app-pub-3940256099942544/5224354917\"")
        }
        create("foss") {
            dimension = "distribution"
            buildConfigField("boolean", "SHOW_SOURCE_LINK", "true")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = releaseSigning
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.maxHeapSize = "1536m"
                it.jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
                // Renders every template to build/outputs/templates: ./gradlew testPlayDebugUnitTest -PtemplateSheets
                it.systemProperty("unroll.templateSheets", project.hasProperty("templateSheets").toString())
            }
        }
    }
}

// F-Droid isn't set up yet, so the foss build is off unless asked for (./gradlew -Pfoss ...). Otherwise
// Android Studio picks it first (alphabetical) and you'd run the build without tips or ads.
androidComponents {
    beforeVariants { variant ->
        if (variant.flavorName == "foss" && !project.hasProperty("foss")) variant.enable = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.exifinterface)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)

    // Google Play only: tip jar, opt-in rewarded ad (+ consent form), on-device face finding.
    "playImplementation"(libs.billing)
    "playImplementation"(libs.play.ads)
    "playImplementation"(libs.ump)
    "playImplementation"(libs.mlkit.face)

    // Canvas, seams and layout are plain Kotlin, tested on the computer.
    testImplementation(libs.junit)
    // Screens rendered on the computer (Robolectric + Roborazzi): ./gradlew recordRoborazziPlayDebug
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
