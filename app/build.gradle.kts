plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.stealthx.securechat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.stealthx.securechat"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    }
}

dependencies {
    implementation(project(":stealthx-crypto"))
    implementation(project(":stealthx-ifr"))
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

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)
}
