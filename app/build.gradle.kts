plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.busly.trackingbus"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.busly.trackingbus"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // For Kotlin code
//        buildConfigField(
//            "String",
//            "MAPS_API_KEY",
//            "\"$mapsApiKey\""
//        )

        // For AndroidManifest.xml
        manifestPlaceholders["MAPS_API_KEY"] = project.property("MAPS_API_KEY") as String
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }



    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}
dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    // Compose UI + Material3
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Necessary for setContent()
    implementation(libs.androidx.activity.compose)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-database")

    // GPS
    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    // AppCompat
    implementation("androidx.appcompat:appcompat:1.7.0")

    // ✅ REQUIRED for Theme.MaterialComponents.*
    implementation("com.google.android.material:material:1.12.0")
}

