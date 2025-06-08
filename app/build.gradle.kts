plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.moodtunes_v1"
    compileSdk = 35

    viewBinding{
        enable = true
    }

    defaultConfig {
        applicationId = "com.example.moodtunes_v1"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += setOf("META-INF/LICENSE.txt", "META-INF/DEPENDENCIES")
        }
    }

}

dependencies {

    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android)

    // Retrofit for HTTP requests
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    // Retrofit converter for JSON (Gson)
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Kotlin Coroutines for asynchronous code
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // (Optional) OkHttp Logging interceptor for debugging network calls
    implementation ("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.11")

    // Room DB
    implementation ("androidx.room:room-runtime:2.6.0")
    implementation ("androidx.room:room-ktx:2.6.0")
    ksp ("androidx.room:room-compiler:2.6.0") // Replace kapt with ksp


    //Glide for loading images
    implementation ("com.github.bumptech.glide:glide:4.15.1")

    // Tensorflow-lite
    implementation ("org.tensorflow:tensorflow-lite:2.14.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.3")



    //Firebase authentication

    implementation ("com.google.firebase:firebase-auth:21.1.0")
    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    //implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-firestore-ktx:24.9.0")

    implementation("com.google.android.gms:play-services-auth:21.2.0")


    // Circle Image View
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    // Chip for genres
    implementation ("com.google.android.material:material:1.9.0")

    // Bottom nav bar
    implementation ("com.google.android.material:material:1.11.0")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.7")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}