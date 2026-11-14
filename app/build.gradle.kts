plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.agendasacramental"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.martincornelli.agendasacramental"
        minSdk = 24
        targetSdk = 36
        versionCode = 49
        versionName = "1.7.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".design.qa"
            versionNameSuffix = "-design-qa"
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

// Android and web ship the exact same parser, workbook limits and duplicate rules.
abstract class SyncHistoryAssets : Sync() {
    @get:Internal abstract val outputDir: DirectoryProperty
}
val syncHistoryAssets = tasks.register<SyncHistoryAssets>("syncHistoryAssets") {
    from(rootProject.file("docs")) {
        include("participation-history.mjs", "history-workbook.mjs", "history-organizations.mjs", "history-workbook-worker.js", "vendor/xlsx*", "vendor/SHEETJS-LICENSE.txt")
    }
    outputDir.set(layout.buildDirectory.dir("generated/historyAssets"))
    into(outputDir)
    eachFile { path = "history/$path" }
}
// Register once for all variants and retain the task dependency. A generated
// variant source was absent from merge*Assets, leaving a blank history WebView.
android.sourceSets.getByName("main").assets.directories.add(layout.buildDirectory.dir("generated/historyAssets").get().asFile.path)
tasks.named("preBuild").configure { dependsOn(syncHistoryAssets) }

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Sign In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Biometría
    implementation("androidx.biometric:biometric:1.1.0")

    // In-App Updates
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
