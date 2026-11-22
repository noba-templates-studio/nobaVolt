plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Android: el código común se empaqueta como librería que usa el módulo androidApp
    android {
        namespace = "com.noba.volt.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        // Necesario para que las imágenes, fuentes y el sonido (composeResources) lleguen al APK
        androidResources { enable = true }
    }

    // iOS: se compila como framework "Shared" que usa el proyecto de Xcode (iosApp)
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { destino ->
        destino.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
    }
}

// Clase de recursos generada: com.noba.volt.recursos.Res
compose.resources {
    packageOfResClass = "com.noba.volt.recursos"
    publicResClass = false
}
