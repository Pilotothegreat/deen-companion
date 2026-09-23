import java.util.Properties

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
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

// Release signing comes from an untracked keystore.properties or from CI environment variables.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingValue(property: String, env: String): String? =
    keystoreProperties.getProperty(property) ?: System.getenv(env)

android {
    namespace = "com.pilotothegreat.deencompanion"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pilotothegreat.deencompanion"
        minSdk = 26
        targetSdk = 37
        versionCode = 199
        versionName = "2.1.0"
        base.archivesName = "bilal-$versionName"
        // The donation sheet opens banking apps; the Google Play build leaves it out (see the play build type).
        buildConfigField("boolean", "SUPPORT_SHEET", "true")

        // Where anonymous usage reports are sent, for builds that have somewhere to send them.
        // Empty — the default, and what an open-source build compiles with — means the app counts
        // locally and posts nothing, however the switch in Settings is set.
        buildConfigField(
            "String",
            "ANALYTICS_ENDPOINT",
            "\"${project.findProperty("bilal.analyticsEndpoint") ?: ""}\"",
        )
    }

    signingConfigs {
        create("release") {
            signingValue("storeFile", "RELEASE_KEYSTORE_FILE")?.let { path ->
                storeFile = rootProject.file(path)
                storePassword = signingValue("storePassword", "RELEASE_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val release = signingConfigs.getByName("release")
            signingConfig = if (release.storeFile != null) release else signingConfigs.getByName("debug")
        }
        // The Google Play build: the release build without the donation sheet, which Play's payments
        // policy doesn't allow. `./gradlew bundlePlay` makes the App Bundle for the Play Console.
        create("play") {
            initWith(getByName("release"))
            buildConfigField("boolean", "SUPPORT_SHEET", "false")
            matchingFallbacks += listOf("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    androidResources {
        generateLocaleConfig = true
    }
    // The language can be changed inside the app, so every install needs all translations.
    bundle {
        language {
            enableSplit = false
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
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    // Robolectric migration tests read exported Room schemas from the tested variant's assets.
    sourceSets {
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }
}

// Google Play has its own listing under a new package, because the original listing's upload key is lost.
// GitHub builds keep the original package, so sideloaded installs keep updating.
androidComponents {
    onVariants(selector().withBuildType("play")) { variant ->
        variant.applicationId.set("com.pilotothegreat.bilal")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.navigation.suite)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.graphics.shapes)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(libs.play.app.update.ktx)
    implementation(libs.adhan)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.glance.appwidget.testing)
    debugImplementation(libs.compose.ui.test.manifest)
}
