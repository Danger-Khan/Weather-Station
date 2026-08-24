plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.dangerkhan.weatherstation"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dangerkhan.weatherstation"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.material3)

    // MQTT Paho Client
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // Charting Engine
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}
