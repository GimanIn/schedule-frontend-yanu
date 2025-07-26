plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize") // ✅ 꼭 있어야 합니다! Parcelize를 사용하려면 필수
}

android {
    namespace = "com.example.mycalendar" // 현재 프로젝트 패키지 이름
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mycalendar" // 현재 프로젝트 패키지 이름
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true // findViewById를 사용하기 위해 viewBinding을 켰습니다.
    }
}

dependencies {
    // ✅ Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ✅ OkHttp (로그 확인용. 선택이지만 추천)
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // ✅ RecyclerView (일정 리스트 표시)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ✅ Kotlin Extensions (Core KTX)
    implementation("androidx.core:core-ktx:1.12.0")

    // ✅ AppCompat (기본적인 UI 스타일을 위한 라이브러리)
    implementation("androidx.appcompat:appcompat:1.6.1")

    // ✅ Google Material Design (UI 요소들)
    implementation("com.google.android.material:material:1.4.0")


    // ✅ AppCompat (기본적인 UI 스타일을 위한 라이브러리)
    implementation("androidx.appcompat:appcompat:1.6.1") // 이 부분 추가

    // ✅ ConstraintLayout (레이아웃 구성)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ✅ Activity KTX (편리한 액티비티 기능)
    implementation("androidx.activity:activity-ktx:1.8.2")

    // ✅ PowerMenu (선택적인 메뉴 기능)
    implementation("com.github.skydoves:powermenu:2.2.4")
    //추가
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // ✅ Unit 테스트
    testImplementation("junit:junit:4.13.2")



    // ✅ Android 테스트
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
