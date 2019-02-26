plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
    id("com.jfrog.bintray")
}

android {
    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            isMinifyEnabled = false
            isTestCoverageEnabled = true
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin/")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.21")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.1")
    androidTestImplementation("androidx.test:runner:1.1.1")
    androidTestImplementation("androidx.test:rules:1.1.1")

}

fun addDependencies(pom: org.gradle.api.publish.maven.MavenPom) = pom.withXml {
    asNode().appendNode("dependencies").let { depNode ->
        configurations.implementation.get().dependencies.forEach {
            depNode.appendNode("dependency").apply {
                appendNode("groupId", it.group)
                appendNode("artifactId", it.name)
                appendNode("version", it.version)
            }
        }
    }
}

val publicationName = "animatednavbarLib"
val description = "An animated bottom navigational bar that follows material design guidelines."
val bintrayRepo = "maven"
val bintrayName = "animatednavbar"

val libraryName = "AnimatedBottomNavigationBar"
val artifactName = "animatednavbar"
val hubUrl = "https://github.com/kadahlin/AnimatedNavBar"
version = "1.0.1"

publishing {
    publications.create(publicationName, MavenPublication::class) {
        artifactId = artifactName
        groupId = "com.kyledahlin"
        version = version
        artifact("$buildDir/outputs/aar/animatednavbar-release.aar")
        addDependencies(pom)
    }
}

bintray {
    user = project.findProperty("bintrayUser") as String?
    key = project.findProperty("bintrayApiKey") as String?
    publish = true
    setPublications(publicationName)

    pkg.apply {
        repo = bintrayRepo
        name = bintrayName
        desc = description
        vcsUrl = "$hubUrl.git"
        websiteUrl = hubUrl
        issueTrackerUrl = "$hubUrl/issues"
        setLicenses("Apache-2.0")
        setLabels("kotlin", "android")
        dryRun = false
        override = false
        publicDownloadNumbers = true
    }
}