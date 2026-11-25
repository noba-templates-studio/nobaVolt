plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.noba.volt"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Mismo identificador que la versión anterior: los móviles que ya la tienen se actualizan
        applicationId = "com.noba.volt"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.1.0"
    }

    // Firma de la versión release: se lee de variables de entorno (en GitHub, de los "Secrets"),
    // así la llave y sus contraseñas nunca están en el código.
    val llave = System.getenv("NOBA_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    if (llave != null) {
        signingConfigs {
            create("release") {
                storeFile = llave
                storePassword = System.getenv("NOBA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NOBA_KEY_ALIAS")
                keyPassword = System.getenv("NOBA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (llave != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
}
