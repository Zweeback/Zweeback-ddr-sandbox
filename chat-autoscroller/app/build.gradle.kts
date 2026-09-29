plugins {
    id("com.android.application")
}

android {
    namespace = "de.chat.autoscroller"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.chat.autoscroller"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}
