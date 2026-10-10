plugins {
    alias(libs.plugins.android.library)
}
android {
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    namespace = "com.stealthx.transport"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":shared"))
    implementation(project(":stealthx-crypto"))
    // Hilt brings javax.inject annotations for DI-ready classes
    implementation(libs.hilt.android)
}
