import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenLocal()
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.androidGradle}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}")
    }
}

plugins {
    id("org.jetbrains.dokka") version Versions.dokka
    id("org.jetbrains.kotlinx.kover") version Versions.kover
}

allprojects {
    repositories {
        mavenLocal()
        google()
        jcenter()
    }

    tasks.withType(KotlinCompile::class.java).all {
        kotlinOptions {
            // Needed for @OptIn(ExperimentalStdlibApi::class) (see Profiler.kt)
            // This goes away in newer version of Kotlin
            // This Gradle syntax changes in newer version of Kotlin, see:
            // https://kotlinlang.org/docs/opt-in-requirements.html#module-wide-opt-in
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}

kover {
    isDisabled = false                                   // true to disable instrumentation of all test tasks in all projects
    coverageEngine.set(kotlinx.kover.api.CoverageEngine.INTELLIJ) // change instrumentation agent and reporter
    intellijEngineVersion.set(Versions.intellijCover)    // change version of IntelliJ agent and reporter
    generateReportOnCheck = true                         // false to do not execute `koverMergedReport` task before `check` task
    disabledProjects = setOf()                           // setOf("project-name") or setOf(":project-name") to disable coverage for project with path `:project-name` (`:` for the root project)
    instrumentAndroidPackage = false                     // true to instrument packages `android.*` and `com.android.*`
    runAllTestsForProjectTask = false                    // true to run all tests in all projects if `koverHtmlReport`, `koverXmlReport`, `koverReport`, `koverVerify` or `check` tasks executed on some project
}

tasks {
    dokkaGfmMultiModule.configure {
        outputDirectory.set(buildDir.resolve("dokka"))
    }

    register("clean").configure {
        delete(rootProject.buildDir)
        delete("buildSrc/build")
    }
}



