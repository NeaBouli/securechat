plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
tasks.withType<Test> { useJUnitPlatform() }
