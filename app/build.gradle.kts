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
    // release"). Only the release keystore uses it: the app holds no API keys.
    // The AI assistant reaches Gemini through the ai-proxy Worker (see ai-proxy/README.md).
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
        versionCode = 16
        versionName = "1.4.0"

        // Public URL of the deployed ai-proxy Worker; empty keeps the assistant offline-only.
        val aiProxyUrl = (project.findProperty("aiProxyUrl") as String?)?.trim().orEmpty()
        buildConfigField("String", "AI_PROXY_URL", "\"$aiProxyUrl\"")

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
            // Opt-in (-PdebugIdSuffix=.dbg): installs next to a Play/release build whose signature differs,
            // without touching its data. Off by default.
            (project.findProperty("debugIdSuffix") as String?)?.takeIf { it.isNotBlank() }?.let {
                applicationIdSuffix = it
            }
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
            // Robolectric + the bundled Quran/Hadith assets exhaust the default test heap in a full run.
            all { it.maxHeapSize = "1536m" }
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
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)
    implementation(libs.play.services.auth)
    implementation(libs.zxing.core)
    implementation(libs.play.services.code.scanner)

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
