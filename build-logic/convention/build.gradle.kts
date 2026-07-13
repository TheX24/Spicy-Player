@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    `kotlin-dsl`
}


dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


gradlePlugin {
    plugins {
        register("AndroidLibrary") {
            id = "com.tx24.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("JvmLibrary") {
            id = "com.tx24.kotlin.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("Hilt") {
            id = "com.tx24.android.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("AndroidFeature") {
            id = "com.tx24.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("AndroidCompose") {
            id = "com.tx24.android.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("AndroidApplication") {
            id = "com.tx24.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("AndroidApplicationCompose") {
            id = "com.tx24.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
    }
}