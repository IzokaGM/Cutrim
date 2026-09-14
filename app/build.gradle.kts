plugins {
    id("com.android.application")
}

android {
    namespace = "com.cutrim.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.cutrim.app"

        minSdk = 26
        targetSdk = 37

        versionCode = 8
        versionName = "0.8.0"
    }
}

dependencies {
    implementation("androidx.media3:media3-common:1.11.0")
    implementation("androidx.media3:media3-effect:1.11.0")
    implementation("androidx.media3:media3-transformer:1.11.0")
}
