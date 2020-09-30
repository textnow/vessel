buildscript {
    repositories {
        mavenLocal()
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.androidGradle}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    }
}

plugins {
    id("com.vanniktech.android.junit.jacoco") version Versions.vannikJacoco
}

allprojects {
    repositories {
        mavenLocal()
        google()
        jcenter()
    }
}

tasks.register("clean").configure {
    delete(rootProject.buildDir)
    delete("buildSrc/build")
}

junitJacoco {
    jacocoVersion = Versions.jacoco
    excludes = listOf(
        // Auto-generated code
        "**/BuildConfig.*",
        "**/*_Impl.*",
        "**/*_Impl\$*.*",
        // Jacoco can't handle these
        "**/*\$Lambda\$*.*",
        "**/*\$inlined\$*.*",
        // Doesn't make sense to test these
        "**/NoOp*.*",
        "**/LoggingKt.*"
    )
    includeNoLocationClasses = true
    includeInstrumentationCoverageInMergedReport = true
}
