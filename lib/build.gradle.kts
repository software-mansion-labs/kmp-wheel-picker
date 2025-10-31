import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "com.swmansion.kmpwheelpicker"

version = "0.1.1"

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetBrains.compose)
    alias(libs.plugins.jetBrains.dokka)
    alias(libs.plugins.jetBrains.kotlin.multiplatform)
    alias(libs.plugins.jetBrains.kotlin.plugin.compose)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPWheelPicker"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies { implementation(compose.preview) }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.jetBrains.kotlinX.coroutines.swing)
        }
    }

    explicitApi()
}

android {
    namespace = "com.swmansion.kmpwheelpicker"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        lint.targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies { debugImplementation(compose.uiTooling) }

dokka {
    moduleName = "kmp-wheel-picker"
    pluginsConfiguration.html.footerMessage = "© 2025 Software Mansion and Patryk Goworowski"
}

mavenPublishing {
    coordinates(artifactId = "kmp-wheel-picker")
    publishToMavenCentral()
    signAllPublications()
    pom {
        name = "KMP Wheel Picker"
        description = "Provides a modular wheel picker for Compose Multiplatform."
        url = "https://github.com/software-mansion-labs/kmp-wheel-picker"
        licenses {
            license {
                name = "The MIT License"
                url = "http://www.opensource.org/licenses/mit-license.php"
            }
        }
        scm {
            connection = "scm:git:git://github.com/software-mansion-labs/kmp-wheel-picker.git"
            developerConnection =
                "scm:git:ssh://github.com/software-mansion-labs/kmp-wheel-picker.git"
            url = "https://github.com/software-mansion-labs/kmp-wheel-picker"
        }
        developers {
            developer {
                id = "patrykgoworowski"
                name = "Patryk Goworowski"
                email = "contact@patrykgoworowski.com"
            }
            developer {
                id = "patrickmichalik"
                name = "Patrick Michalik"
                email = "patrick.michalik@swmansion.com"
            }
        }
    }
}
