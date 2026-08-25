import java.util.Properties
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

abstract class BuildTimestampValueSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String = "${System.currentTimeMillis()}L"
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Version code, bumped automatically by the pre-commit hook (.githooks/pre-commit).
// Keep the declaration on a single line: the hook matches it literally.
val appVersionCode = 6
val appVersionName = "1.0"

// Release signing material comes either from a local `keystore.properties`
// (not versioned) or from environment variables set by the CI.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingSetting(propertyKey: String, environmentKey: String): String? =
    keystoreProperties.getProperty(propertyKey)?.takeIf { it.isNotBlank() }
        ?: System.getenv(environmentKey)?.takeIf { it.isNotBlank() }

val releaseStorePath = signingSetting("storeFile", "KEYSTORE_FILE")
val releaseStorePassword = signingSetting("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = signingSetting("keyAlias", "KEY_ALIAS")
val releaseKeyPassword = signingSetting("keyPassword", "KEY_PASSWORD")
val releaseStore = releaseStorePath?.let { rootProject.file(it) }

val releaseSigningIssue: String? = when {
    releaseStorePath == null -> "storeFile / KEYSTORE_FILE is not set"
    releaseStore?.exists() != true -> "keystore file not found: ${releaseStore?.absolutePath}"
    releaseStorePassword == null -> "storePassword / KEYSTORE_PASSWORD is not set"
    releaseKeyAlias == null -> "keyAlias / KEY_ALIAS is not set"
    releaseKeyPassword == null -> "keyPassword / KEY_PASSWORD is not set"
    else -> null
}

android {
    namespace = "fr.triquet.manyinone"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "fr.triquet.manyinone"
        minSdk = 28
        targetSdk = 36
        versionCode = appVersionCode
        versionName = "$appVersionName.$appVersionCode"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningIssue == null) {
            create("release") {
                storeFile = releaseStore
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseSigningIssue == null) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "WARNING: release build falls back to the debug signing key " +
                        "($releaseSigningIssue). The produced APK must not be distributed."
                )
                signingConfigs.getByName("debug")
            }
        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    packaging {
        resources.excludes += setOf(
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
            "META-INF/NOTICE.md",
        )
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }


    defaultConfig {
        buildConfigField(
            "long",
            "BUILD_TIMESTAMP",
            providers.of(BuildTimestampValueSource::class) {}.get(),
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Navigation
    implementation(libs.navigation.compose)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit Barcode
    implementation(libs.mlkit.barcode)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ZXing
    implementation(libs.zxing.core)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
