plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

// The licensed Samsung SDK is supplied locally, never downloaded from an unofficial mirror.
val samsungSdk = file("libs/samsung-health-data-api-1.1.0.aar")

android {
    namespace = "com.jaydev.stepcounter"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.jaydev.stepcounter"
        minSdk = 29
        targetSdk = 37
        versionCode = 8
        versionName = "1.1.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "SAMSUNG_SDK_INCLUDED", "true")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("com.google.code.gson:gson:2.13.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(files(samsungSdk))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.register("requireSamsungSdk") {
    doLast { check(samsungSdk.isFile) { "Place the licensed Samsung SDK at app/libs/samsung-health-data-api-1.1.0.aar." } }
}
tasks.configureEach {
    if (name == "preBuild") dependsOn("requireSamsungSdk")
}
