plugins {
    id("com.android.library")
    id("kotlin-android")
    kotlin("kapt")
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.2"

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments = mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
                )
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    testOptions {
        unitTests.apply {
            // https://github.com/robolectric/robolectric/issues/5456
            isIncludeAndroidResources = false
            isReturnDefaultValues = true
        }
    }
}

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutine}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutine}")

    kapt("androidx.room:room-compiler:${Versions.room}")

    implementation("androidx.room:room-runtime:${Versions.room}")
    implementation("androidx.room:room-ktx:${Versions.room}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}")
    implementation("com.google.code.gson:gson:${Versions.gson}")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutine}")

    testImplementation("androidx.room:room-testing:${Versions.room}")
    testImplementation("androidx.test:core:${Versions.androidXTest}")
    testImplementation("androidx.test:runner:${Versions.androidXTest}")
    testImplementation("androidx.test:rules:${Versions.androidXTest}")
    testImplementation("androidx.test.ext:junit:${Versions.androidXJunit}")
    testImplementation("androidx.arch.core:core-testing:${Versions.androidXCoreTest}")

    testImplementation("org.koin:koin-test:${Versions.koin}")
    testImplementation("org.koin:koin-android:${Versions.koin}")

    testImplementation("junit:junit:${Versions.junit}")
    testImplementation("org.robolectric:robolectric:${Versions.robolectric}")

    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:${Versions.assertk}")
}

/**
 * Workaround until [VSL-5] is done.
 * https://issuetracker.google.com/issues/78547461
 */
fun com.android.build.gradle.internal.dsl.TestOptions.UnitTestOptions.all(block: Test.() -> Unit) =
    all(KotlinClosure1<Any, Test>({ (this as Test).apply(block) }, owner = this))
