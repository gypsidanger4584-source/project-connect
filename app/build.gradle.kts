import java.util.Properties

// -----------------------------------------------------------
// app/build.gradle.kts (최종 수정 + Retrofit + OkHttp 로깅)
// -----------------------------------------------------------
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

// OpenAI 키는 코드에 넣지 않고 local.properties(OPENAI_API_KEY=...) 또는 환경변수에서 읽는다.
// local.properties는 .gitignore 대상이다. 키가 없으면 빈 문자열이 들어가 AI 채팅은 동작하지 않는다.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val openAiApiKey: String = localProperties.getProperty("OPENAI_API_KEY")
    ?: System.getenv("OPENAI_API_KEY")
    ?: ""

android {
    namespace = "com.example.login_new"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.login_new"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val escapedKey = openAiApiKey.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "OPENAI_API_KEY", "\"$escapedKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            // 🔐 예시) 여기서 빌드타입별로 API 키 넣을 수 있음 (원하면)
            // buildConfigField("String", "OPENAI_API_KEY", "\"실제_디버그키\"")
        }
    }

    // ✅ Java 17로 업그레이드
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ✅ Kotlin JVM 타깃도 17로 맞춤
    kotlinOptions {
        jvmTarget = "17"
    }

    // ✅ 빌드 경고 줄이기
    packaging {
        resources.excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*")
    }
}

dependencies {
    // ✅ AndroidX & Material
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ✅ Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.biometric)
    implementation(libs.androidx.security.crypto)

    // ✅ Guava & WorkManager
    implementation(libs.guava)
    implementation(libs.androidx.work.runtime)

    // ✅ Room (libs 방식)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ✅ Glide (이미지 로더)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // ✅ Retrofit2 (ChatActivity에서 Call/Callback/Response 쓰는 용도)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // ✅ OkHttp Logging Interceptor (RetrofitInstance에서 HttpLoggingInterceptor 사용)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ✅ 테스트
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
