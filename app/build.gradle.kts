plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dashblock.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dashblock.game"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    sourceSets.getByName("main").assets.srcDir("../assets/music")

    kotlinOptions {
        jvmTarget = "17"
    }
}
