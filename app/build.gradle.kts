import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val formattedDate = SimpleDateFormat("ddMMyyyy", Locale.US).format(Date())
android {
    namespace = "com.iconchanger.wallpaper.rolling.icons"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.iconchanger.wallpaper.rolling.icons"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xuse-k2=false",
            "-Xskip-metadata-version-check"
        )
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        dataBinding = true
    }

    applicationVariants.all {
        val variant = this
        val type = variant.buildType.name

        // APK
        variant.outputs.all {
            val output = this as? com.android.build.gradle.api.ApkVariantOutput
            output?.outputFileName =
                "D72_RollingIcons_v${variant.versionName}_c${variant.versionCode}_${formattedDate}-${type}.apk"
        }

        // AAB
        applicationVariants.all {
            val variant = this
            val type = variant.buildType.name

            // APK
            variant.outputs.all {
                val output = this as? com.android.build.gradle.api.ApkVariantOutput
                output?.outputFileName =
                    "D72_RollingIcons_v${variant.versionName}_c${variant.versionCode}_${formattedDate}-${type}.apk"
            }
        }
    }
    tasks.whenTaskAdded {
        if (name == "bundleRelease") {
            doLast {
                val bundleDir = File(project.buildDir, "outputs/bundle/release")
                bundleDir.listFiles { f -> f.extension == "aab" }?.forEach { aab ->
                    val newName =
                        "D72_RollingIcons_v${android.defaultConfig.versionName}_c${android.defaultConfig.versionCode}_${formattedDate}-release.aab"
                    aab.renameTo(File(aab.parent, newName))
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    implementation("com.intuit.ssp:ssp-android:1.1.0")


    // libGDX
    val gdxVersion = "1.14.2"
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")

    // Image loading (Coil)
    implementation("io.coil-kt:coil:2.5.0")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lottie animations
    implementation("com.airbnb.android:lottie:6.4.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.android.gms:play-services-ads")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-database")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")


    //lib ads
    implementation("com.cscapp:library-test:0.0.7")

    // sdk mediation
    implementation("com.facebook.android:facebook-android-sdk:18.3.0")
    implementation("com.google.ads.mediation:facebook:6.21.0.4")
    implementation("com.google.ads.mediation:applovin:13.6.3.0")
    implementation("com.google.ads.mediation:inmobi:11.4.0.0")
    implementation("com.google.ads.mediation:pangle:8.1.0.5.0")
    implementation("com.google.ads.mediation:mintegral:17.1.61.1")
    implementation("com.unity3d.ads:unity-ads:4.19.0")
    implementation("com.google.ads.mediation:unity:4.19.0.0")
}

