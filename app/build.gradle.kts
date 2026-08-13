import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

android {
    namespace = "com.stealthx.securechat"
    compileSdk = 36

    signingConfigs {
        create("release") {
            val ksPath = localProps["KEYSTORE_PATH"] as? String
            val ksPass = localProps["KEYSTORE_PASS"] as? String
            val ksAlias = localProps["KEY_ALIAS"] as? String ?: "securechat"
            if (ksPath != null && ksPass != null) {
                storeFile = rootProject.file(ksPath)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksPass
            }
        }
    }

    defaultConfig {
        applicationId = "securechat.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "0.1.9-alpha"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "FORCED_TIER", "\"\"")
        buildConfigField("Boolean", "ALLOW_SCREENSHOTS", "false")
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "FORCE_ELITE", "true")
            buildConfigField("String", "FORCED_TIER", "\"ELITE\"")
        }
        create("storeScreenshot") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".screenshots"
            versionNameSuffix = "-screenshots"
            buildConfigField("Boolean", "FORCE_ELITE", "false")
            buildConfigField("String", "FORCED_TIER", "\"FREE\"")
            buildConfigField("Boolean", "ALLOW_SCREENSHOTS", "true")
            matchingFallbacks += listOf("debug")
        }
        create("internalRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
            buildConfigField("Boolean", "FORCE_ELITE", "true")
            buildConfigField("String", "FORCED_TIER", "\"ELITE\"")
            matchingFallbacks += listOf("release")
        }
        create("freeTierRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
            buildConfigField("Boolean", "FORCE_ELITE", "false")
            buildConfigField("String", "FORCED_TIER", "\"FREE\"")
            matchingFallbacks += listOf("release")
        }
        create("proTierRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
            buildConfigField("Boolean", "FORCE_ELITE", "false")
            buildConfigField("String", "FORCED_TIER", "\"PRO\"")
            matchingFallbacks += listOf("release")
        }
        create("eliteTierRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".elite"
            versionNameSuffix = "-elite"
            buildConfigField("Boolean", "FORCE_ELITE", "true")
            buildConfigField("String", "FORCED_TIER", "\"ELITE\"")
            matchingFallbacks += listOf("release")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("Boolean", "FORCE_ELITE", "false")
            buildConfigField("String", "FORCED_TIER", "\"\"")
        }
    }

    sourceSets.getByName("storeScreenshot") {
        java.srcDir("src/debug/java")
        manifest.srcFile("src/debug/AndroidManifest.xml")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DISCLAIMER",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE.md",
                "META-INF/INDEX.LIST",
                "META-INF/versions/**"
            )
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.work)
    implementation(project(":stealthx-crypto"))
    implementation(project(":stealthx-access"))
    implementation(project(":security"))
    implementation(project(":shared"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":features:messenger"))
    implementation(project(":features:contacts"))
    implementation(project(":features:settings"))
    implementation(project(":presentation"))
    implementation(project(":transport"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.androidx.biometric)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.junit.ktx)
    androidTestImplementation(libs.androidx.test.runner)
}

tasks.withType<Test> { useJUnitPlatform() }
