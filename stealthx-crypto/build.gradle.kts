/*
 * :stealthx-crypto — THE ONLY CRYPTO MODULE
 * ==========================================
 * All cryptographic operations live here.
 * NO other module may use lazysodium directly.
 * NO other module may implement crypto logic.
 *
 * Dependencies: :shared ONLY
 */
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.stealthx.crypto"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))

    // THE ONLY CRYPTO LIBRARY — lazysodium wraps libsodium
    implementation(libs.lazysodium.android)
    implementation(libs.jna)

    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
