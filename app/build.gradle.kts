// FIXED: Add ndk block to release build type and bump version to 1.3.1
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

android {
    namespace = "com.pilotothegreat.deencompanion"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pilotothegreat.deencompanion"
        minSdk = 26
        targetSdk = 37
        versionCode = 133
        versionName = "1.3.3"
        base.archivesName = "deen-$versionName"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            // ADD this to keep Koin working:
            ndk {
                // Exclude filters if needed
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    lint {
        abortOnError = false
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core:1.7.6")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.7.6")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")

    implementation(libs.kotlinx.serialization.json)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.compose.navigation3)
    implementation(libs.androidx.compose.adaptive)

    implementation(libs.androidx.lifecycle.service)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.timber)

    implementation(libs.coil.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.haze)
    implementation(libs.haze.materials)

    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation("androidx.compose.ui:ui-test-junit4:1.7.6")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
    testImplementation("androidx.test.ext:junit:1.1.5")
}

