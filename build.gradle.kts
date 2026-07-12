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

    val sourceFiles = sourceRoots.map { root ->
        fileTree(root) {
            include("**/*.java", "**/*.json", "**/*.kt", "**/*.kts", "**/*.pro", "**/*.xml")
            exclude("**/build/**")
        }
    }

    inputs.files(sourceFiles)

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

        val hits = sourceFiles.flatMap { tree ->
            tree.files
                .filter { it.extension.lowercase() in sourceExtensions }
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

val verifyNoClientSideGooglePlayUnlock = tasks.register("verifyNoClientSideGooglePlayUnlock") {
    group = "verification"
    description = "Prevent Google Play callbacks from granting a local tier without server verification"

    val billingViewModel = file(
        "presentation/src/main/java/com/stealthx/presentation/screens/UpgradeViewModel.kt"
    )
    inputs.file(billingViewModel)

    doLast {
        val source = billingViewModel.readText()
        val forbidden = listOf("saveTierResult(", "saveCachedTier(", "google_play:")
        val hits = forbidden.filter(source::contains)
        if (hits.isNotEmpty()) {
            error(
                "Google Play must be verified server-side before tier issuance; " +
                    "forbidden client unlock markers: ${hits.joinToString()}"
            )
        }
    }
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(verifyNoAppIfrWalletCode)
        dependsOn(verifyNoClientSideGooglePlayUnlock)
    }
}
