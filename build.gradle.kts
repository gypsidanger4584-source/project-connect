// -----------------------------------------------------------
// build.gradle.kts (Project): 플러그인 ID 참조 (버전은 settings 파일에서 관리됨)
// -----------------------------------------------------------
plugins {
    // alias는 libs.versions.toml에 정의된 경우에만 사용 가능
    // 플러그인 ID를 직접 사용하여 settings.gradle.kts의 버전을 참조합니다.
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.kapt") apply false
    id("com.google.gms.google-services") apply false
}
