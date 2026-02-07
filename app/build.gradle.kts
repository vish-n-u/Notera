plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.jetbrains.kotlin.plugin.compose")

}

android {
    namespace = "com.myapp.ridescribe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.myapp.ridescribe"
        minSdk = 29
        targetSdk = 35
        versionCode = 100
        versionName = "1.9.9.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
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
    kotlinOptions {
        jvmTarget = "1.8"
    }


    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val room_version = "2.6.1"
    ksp("androidx.room:room-compiler:2.5.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.compose.material:material:1.8.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("androidx.webkit:webkit:1.8.0")
    implementation("com.moizhassan.ffmpeg:ffmpeg-kit-16kb:6.1.1")
    // ✅ Aztec Glide loader (provides GlideImageLoader & GlideVideoThumbnailLoader)
    // ✅ Glide image library (required for GlideImageLoader)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.36.0")
    implementation("com.google.accompanist:accompanist-drawablepainter:0.35.0-alpha")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("com.arthenica:smart-exception-java:0.2.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("org.wordpress:aztec:v2.1.4")
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.material3:material3:1.4.0-alpha12")
    implementation("androidx.compose.ui:ui")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")
    implementation("com.github.skydoves:landscapist-glide:2.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit-mock:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc10")
    implementation ("com.canopas.intro-showcase-view:introshowcaseview:2.0.2")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    implementation("com.github.donald-okara:TextieMDLibrary:1.0.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}