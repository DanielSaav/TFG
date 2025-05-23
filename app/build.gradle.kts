plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") version "4.4.0"
}

android {
    namespace = "com.example.tfg"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tfg"
        minSdk = 31
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

    dependencies {
        implementation(platform("com.google.firebase:firebase-bom:33.9.0"))

        // Firebase (con KTX opcionalmente)
        implementation("com.google.firebase:firebase-auth-ktx")        // auth KTX
        implementation("com.google.firebase:firebase-firestore-ktx")   // firestore KTX
        implementation("com.google.firebase:firebase-analytics-ktx")   // analytics KTX
        implementation("com.google.firebase:firebase-storage-ktx")     // storage KTX

        // Glide para cargar y redondear imágenes
        implementation("com.github.bumptech.glide:glide:4.15.1")
        annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

        // Para manejar la selección de imágenes (Activity KTX)
        implementation("androidx.activity:activity-ktx:1.8.0")

        // AndroidX y Material
        implementation(libs.appcompat)
        implementation(libs.material)
        implementation(libs.activity)
        implementation(libs.constraintlayout)

        // Testing
        testImplementation(libs.junit)
        androidTestImplementation(libs.ext.junit)
        androidTestImplementation(libs.espresso.core)
    }


