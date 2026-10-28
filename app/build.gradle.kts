plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

android {
    namespace = "com.example"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    // Local secrets live in gitignored local.properties (see README "Signing a
    // release"). Only the release keystore uses it now — the app itself is
    // fully offline and takes no API keys.
    // NOTE: providers.gradleProperty does NOT see local.properties, so it is
    // read from disk explicitly.
    val localSecretsFile = rootProject.file("local.properties")
    val localSecrets = Properties().apply {
        if (localSecretsFile.exists()) {
            try { localSecretsFile.inputStream().use { load(it) } } catch (_: Exception) { }
        }
    }
    fun secret(name: String): String? =
        (localSecrets.getProperty(name) ?: System.getenv(name))?.takeIf { it.isNotBlank() }

    defaultConfig {
        applicationId = "com.aistudio.noorup.qzmxlw"
        minSdk = 26
        targetSdk = 37
        versionCode = 14
        versionName = "1.2.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Values come from local.properties / env — never committed.
            // If NOORUP_KEYSTORE_FILE is absent the config stays empty and the
            // release block below skips signing (unsigned = local testing only).
            val ks = secret("NOORUP_KEYSTORE_FILE")
            if (!ks.isNullOrBlank()) {
                storeFile = rootProject.file(ks)
                storePassword = secret("NOORUP_KEYSTORE_PASSWORD")
                keyAlias = secret("NOORUP_KEY_ALIAS")
                keyPassword = secret("NOORUP_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
        }
        release {
            // R8 enabled: Play requires DEX obfuscation (threshold deadline Feb 2027).
            // Gson models are protected in proguard-rules.pro — verify Hadith/Quran
            // screens after building before uploading.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signed ONLY when the keystore is configured (see README "Signing a
            // release"). Without it the build stays unsigned: fine for local
            // testing, NOT uploadable to Play. The old debug-key signing is gone
            // on purpose — debug-signed artifacts must never be uploaded.
            if (!secret("NOORUP_KEYSTORE_FILE").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    packaging {
        jniLibs {
            // androidx.graphics:graphics-path (pulled in by Compose ui-graphics)
            // ships a stripped prebuilt libandroidx.graphics.path.so with no
            // debug symbols. Compose only reaches it through Path.iterator(),
            // PathHitTester and Path.toSvg(); this app uses none of them and R8
            // removes the wrapper classes. Excluding the unused library keeps
            // native code out of the bundle, so Play stops asking for symbols.
            excludes += "**/libandroidx.graphics.path.so"
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
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.gson)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
