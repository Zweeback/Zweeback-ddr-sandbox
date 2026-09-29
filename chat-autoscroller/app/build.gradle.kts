plugins {
    id("com.android.application")
}

android {
    namespace = "de.chat.autoscroller"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.chat.autoscroller"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
}
