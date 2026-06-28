plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.kuvera.bpscnotes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kuvera.bpscnotes"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://api-stg.bpscnotes.in/api/v1/\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.bpscnotes.in/api/v1/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")

    // Activity
    implementation("androidx.activity:activity-compose:1.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    implementation(libs.androidx.material3)
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.socket:socket.io-client:2.1.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Image
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // WorkManager — for durable background retry (e.g., CA MCQ submit)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // JSoup — reliable HTML-to-plain-text conversion for share intents
    implementation("org.jsoup:jsoup:1.17.2")

    // Chrome Custom Tab — in-app browser for privacy policy / terms
    implementation("androidx.browser:browser:1.8.0")

    // Payments — Cashfree (web purchases / debug) + Google Play Billing (production)
    // Razorpay removed: confirmed unused in Architecture Audit (CRIT-06)
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.4.0")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    implementation("com.google.android.gms:play-services-ads:23.1.0")

    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
//    implementation("com.posthog.android:posthog:3.4.0")
    implementation("com.posthog:posthog-android:3.+")
//    implementation("com.posthog.android:posthog-android:3.4.0")

    implementation("com.cashfree.pg:api:2.+")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("com.github.yalantis:ucrop:2.2.8")
}
