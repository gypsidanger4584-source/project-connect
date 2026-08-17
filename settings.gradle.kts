// -----------------------------------------------------------
// settings.gradle.kts: 플러그인 버전 단일화 및 관리 (8.13.0 통일)
// -----------------------------------------------------------
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    // ⭐️ [버전 충돌 해결] 모든 AGP 버전을 8.13.0으로 통일하여 오류를 해결합니다.
    plugins {
        val kotlinVersion = "1.9.23"
        val agpVersion = "8.13.2"      // 🚨 요청에 따라 8.13.0으로 통일
        val googleServicesVersion = "4.4.2"
        val kspVersion = "1.9.23-1.0.20" // ⭐️ KSP 버전 추가

        id("com.android.application") version agpVersion
        id("com.android.library") version agpVersion

        id("org.jetbrains.kotlin.android") version kotlinVersion
        id("org.jetbrains.kotlin.kapt") version kotlinVersion // KAPT 오류 해결

        // ⭐️ KSP 플러그인 추가 (Room 컴파일러 사용)
        id("com.google.devtools.ksp") version kspVersion

        id("com.google.gms.google-services") version googleServicesVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "login_new"
include(":app")
