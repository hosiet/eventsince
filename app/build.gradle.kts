plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "me.byang.eventsince"
    compileSdk = 37

    defaultConfig {
        applicationId = "me.byang.eventsince"
        minSdk = 33
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing is optional so that CI and F-Droid can build without the key.
    // Provide EVENTSINCE_STORE_FILE, EVENTSINCE_STORE_PASSWORD, EVENTSINCE_KEY_ALIAS and
    // EVENTSINCE_KEY_PASSWORD as Gradle properties (for example in ~/.gradle/gradle.properties)
    // or as environment variables.
    fun releaseSigningProperty(name: String): String? =
        providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orNull?.ifBlank { null }

    val releaseStoreFile = releaseSigningProperty("EVENTSINCE_STORE_FILE")
    if (releaseStoreFile != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseSigningProperty("EVENTSINCE_STORE_PASSWORD")
                keyAlias = releaseSigningProperty("EVENTSINCE_KEY_ALIAS")
                keyPassword = releaseSigningProperty("EVENTSINCE_KEY_PASSWORD")
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    // Per-ABI APKs are only produced when the release script asks for them (-PabiSplits),
    // so day-to-day debug builds and CI keep a single universal APK.
    if (providers.gradleProperty("abiSplits").isPresent) {
        splits {
            abi {
                isEnable = true
                reset()
                include("arm64-v8a", "x86_64")
                isUniversalApk = true
            }
        }
    }

    bundle {
        // The app has its own language picker (LocaleManager), so both languages must be
        // installed up front instead of being delivered by Play as on-demand splits.
        language {
            enableSplit = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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

    lint {
        abortOnError = false
        warningsAsErrors = false
        checkReleaseBuilds = true
        // Cleartext (http://) WebDAV servers on a home LAN are a deliberate choice: the server
        // address is entered by the user, and certificate validation stays on for https://.
        disable += "InsecureBaseConfiguration"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.reorderable)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
