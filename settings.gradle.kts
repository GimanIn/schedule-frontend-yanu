// settings.gradle.kts 파일 내용 - 이대로 복사해서 붙여넣으세요!

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // 구글이 제공하는 라이브러리 저장소
        mavenCentral() // 전 세계 개발자들이 사용하는 가장 큰 라이브러리 저장소
        // 혹시라도 필요할 수 있는 JCenter는 더 이상 사용되지 않으므로 추가하지 않습니다.
        // 추가적인 Maven 저장소가 필요하다면 여기에 'maven { url "..." }' 형식으로 추가합니다.
    }
}

// 이 부분은 본인의 프로젝트 이름에 따라 다를 수 있습니다.
// 만약 프로젝트 이름이 'MyCalendar'가 아니라면, 그 이름으로 바꿔주세요.
rootProject.name = "MyCalendar"
// 이 부분은 건드리지 마세요.
include(":app")