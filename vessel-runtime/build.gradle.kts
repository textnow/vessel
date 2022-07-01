plugins {
    id("com.android.library")
    id("kotlin-android")
    kotlin("kapt")
    id("org.jetbrains.dokka")
    id("maven-publish")
}

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"

    defaultConfig {
        minSdk = 21
        targetSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    afterEvaluate {
        libraryVariants.forEach { variant ->
            tasks {
                register("${variant.name}Sources", Jar::class) {
                    variant.sourceSets.forEach {
                        from(it.javaDirectories)
                    }
                    archiveClassifier.set("sources")
                }
            }
        }
    }

    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.systemProperty("robolectric.logging.enabled", "true")
                it.jvmArgs("-noverify")
            }
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

    testImplementation("io.insert-koin:koin-test:${Versions.koin}")
    testImplementation("io.insert-koin:koin-core:${Versions.koin}")
    testImplementation("io.insert-koin:koin-android:${Versions.koin}")
    testImplementation("io.insert-koin:koin-test-junit4:${Versions.koin}")

    testImplementation("junit:junit:${Versions.junit}")
    testImplementation("org.robolectric:robolectric:${Versions.robolectric}")

    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:${Versions.assertk}")
}

afterEvaluate {
    tasks.dokkaGfm.configure {
        moduleName.set(AppVersions.name)
        outputDirectory.set(buildDir.resolve("dokka"))
        dokkaSourceSets {
            named("release") {
                noAndroidSdkLink.set(false)
                noStdlibLink.set(false)
                noJdkLink.set(false)
                jdkVersion.set(8)
            }
        }
    }

    val dokkaJar by tasks.creating(Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Assembles ${project.name} documentation with Dokka"
        archiveClassifier.set("javadoc")
        from(tasks.dokkaGfm)
        dependsOn(tasks.dokkaGfm)
    }

    publishing {
        repositories {
            maven {
                name = GithubPackageRepository.repoName
                url = uri(GithubPackageRepository.url)
                credentials {
                    username = GithubPackageRepository.username
                    password = GithubPackageRepository.password
                }
            }
        }

        publications {
            android.libraryVariants.forEach { variant ->

                create<MavenPublication>(variant.name) {
                    groupId = VesselGroupId
                    artifactId = project.name
                    version = if (variant.buildType.isDebuggable) {
                        "${AppVersions.name}.${AppVersions.pr}-SNAPSHOT"
                    } else {
                        AppVersions.name
                    }

                    artifact(tasks["bundle${variant.name.capitalize()}Aar"])
                    artifact(tasks.named("${variant.name}Sources").get())
                    artifact(dokkaJar)

                    pom {
                        withXml {
                            asNode().appendNode("dependencies").apply {
                                fun Dependency.write(scope: String) = appendNode("dependency").apply {
                                    appendNode("groupId", group)
                                    appendNode("artifactId", name)
                                    appendNode("version", version)
                                    appendNode("scope", scope)
                                }

                                fun Configuration.writeScope(scope: String) =
                                    this.dependencies
                                        .filterNot { it.group == null }
                                        .filterNot { it.version == null }
                                        .filterNot { it.name == "unspecified" }
                                        .forEach { it.write(scope) }

                                configurations["${variant.name}Implementation"].writeScope("runtime")
                                configurations["${variant.name}Api"].writeScope("compile")
                                configurations["implementation"].writeScope("runtime")
                                configurations["api"].writeScope("compile")
                            }
                        }
                    }
                }
            }
        }
    }
}
