plugins {
    id("com.android.library")
    id("kotlin-android")
    kotlin("kapt")
    id("org.jetbrains.dokka")
    id("maven-publish")
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.2"

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode = AppVersions.code
        versionName = AppVersions.name
        setProperty("archivesBaseName", "${project.name}-$versionName")

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
        getByName("debug") {
            versionNameSuffix = ".${AppVersions.pr}-SNAPSHOT"
        }

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
                    variant.buildType.versionNameSuffix?.let {
                        version = "${variant.mergedFlavor.versionName}$it"
                    } ?: let {
                        version = variant.mergedFlavor.versionName
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

                                configurations["${variant.name}Implementation"].writeScope("compile")
                                configurations["${variant.name}Api"].writeScope("runtime")
                                configurations["implementation"].writeScope("compile")
                                configurations["api"].writeScope("runtime")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Workaround until [VSL-5] is done.
 * https://issuetracker.google.com/issues/78547461
 */
fun com.android.build.gradle.internal.dsl.TestOptions.UnitTestOptions.all(block: Test.() -> Unit) =
    all(KotlinClosure1<Any, Test>({ (this as Test).apply(block) }, owner = this))
