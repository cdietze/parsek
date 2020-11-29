plugins {
    kotlin("js")
}

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
}

dependencies {
    implementation(project(":"))
    implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")
    testImplementation(kotlin("test-js"))
}

kotlin {
    js(LEGACY) {
        browser {
            binaries.executable()
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
}