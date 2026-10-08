plugins {
    id("com.android.application")
}

android {
    namespace = "com.fl8y.lightcloner.bridge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fl8y.lightcloner.bridge"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
