// Root build.gradle.kts — SecureChat / StealthX Platform
// ============================================================
// NO logic here. Plugin declarations only.
// All versions are in gradle/libs.versions.toml

plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.android.library)      apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.kotlin.jvm)           apply false
    alias(libs.plugins.kotlin.kapt)          apply false
    alias(libs.plugins.kotlin.compose)       apply false
    alias(libs.plugins.hilt)                 apply false
    alias(libs.plugins.detekt)               apply true
}

// -- Detekt (static analysis) -----------------------------------------
detekt {
    config.setFrom(files("$rootDir/config/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.versions.detekt.get()}")
}

// -- Global task: run all module tests --------------------------------
tasks.register("testAll") {
    group = "verification"
    description = "Run all module unit tests"
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("test") })
}

val verifyNoAppIfrWalletCode = tasks.register("verifyNoAppIfrWalletCode") {
    group = "verification"
    description = "Fail if Android app source contains IFR or wallet code paths"

    val sourceRoots = listOf(
        "app",
        "data",
        "domain",
        "features",
        "presentation",
        "shared",
        "stealthx-access",
        "stealthx-crypto",
        "transport",
    ).map { file(it) }.filter { it.exists() }

    inputs.files(sourceRoots)

    doLast {
        val sourceExtensions = setOf("java", "json", "kt", "kts", "pro", "xml")
        val forbiddenTerms = listOf(
            "IFR",
            "Ifr",
            "ifr",
            "WalletConnect",
            "walletconnect",
            "MetaMask",
            "metamask",
            "Uniswap",
            "uniswap",
            "web3",
            "Web3",
            "Ethereum",
            "ethereum",
            "walletAddress",
            "walletSignature",
            "ifrDiscount",
            "stealthx-ifr",
        )

        val hits = sourceRoots.flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile }
                .filter { it.extension.lowercase() in sourceExtensions }
                .filterNot { it.invariantSeparatorsPath.contains("/build/") }
                .flatMap { sourceFile ->
                    val text = sourceFile.readText()
                    forbiddenTerms
                        .filter { term -> text.contains(term) }
                        .map { term -> "${sourceFile.relativeTo(rootDir).invariantSeparatorsPath}: $term" }
                }
        }

        if (hits.isNotEmpty()) {
            error("Android app code must remain IFR/wallet-free:\n${hits.joinToString("\n")}")
        }
    }
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(verifyNoAppIfrWalletCode)
    }
}
