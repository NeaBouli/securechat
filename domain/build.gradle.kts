plugins {
    alias(libs.plugins.android.library)
}
android {
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    namespace = "com.stealthx.domain"
    compileSdk = 37
    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}
dependencies {
    implementation(project(":stealthx-crypto"))
    implementation(project(":shared"))
    implementation(project(":transport"))
    // NO :data, NO :security
    implementation(libs.kotlinx.coroutines.android)
    // Hilt brings javax.inject annotations for DI-ready classes
    implementation(libs.hilt.android)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}
tasks.withType<Test> { useJUnitPlatform() }
