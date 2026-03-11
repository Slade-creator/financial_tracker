plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.studentassoc.financialtracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.studentassoc.financialtracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val driveId = localProperties.getProperty("GOOGLE_DRIVE_CLIENT_ID") ?: ""

        buildConfigField("String", "DRIVE_CLIENT_ID", "\"$driveId\"")
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

    packaging {
        resources {
            excludes.add("/META-INF/DEPENDENCIES")
            excludes.add("/META-INF/LICENSE")
            excludes.add("/META-INF/LICENSE.txt")
            excludes.add("/META-INF/license.txt")
            excludes.add("/META-INF/NOTICE")
            excludes.add("/META-INF/NOTICE.txt")
            excludes.add("/META-INF/notice.txt")
            excludes.add("/META-INF/ASL2.0")
            excludes.add("/META-INF/*.kotlin_module")
            excludes.add("/META-INF/INDEX.LIST")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.common.java8)

    implementation(libs.androidx.navigation.fragment.v276)
    implementation (libs.androidx.navigation.ui.v276)
    implementation(libs.androidx.viewpager2)

    implementation(libs.androidx.core)

    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.androidx.work.runtime)
    implementation(libs.gson)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.google.http.client.android)

    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android.v1352)
    implementation(libs.google.api.services.drive.vv3rev20231115200)

    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.androidx.biometric)
}