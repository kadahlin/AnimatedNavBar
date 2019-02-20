plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}

android {
    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            isMinifyEnabled = false
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin/")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.21")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
}

publishing {
    publications {
        create<MavenPublication>("libPublish") {
            groupId = "com.kyledahlin"
            artifactId = "animatednavbar"
            version = "1.0.0"
            artifact("$buildDir/outputs/aar/animatednavbar-release.aar")

            pom {
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                withXml {
                    val dependenciesNode = this.asNode().appendNode("dependencies")
                    try {
                        val deps = project.configurations.getByName("implementation").allDependencies
                        deps.forEach { dep ->
                            if (dep.group.isNullOrEmpty() || dep.name.isEmpty())
                                return@forEach

                            val dependencyNode = dependenciesNode.appendNode("dependency").apply {
                                appendNode("groupId", dep.group)
                                appendNode("artifactId", dep.name)
                                appendNode("version", dep.version)
                                appendNode("scope", "compile")
                            }

                            if (dep is ModuleDependency) {
                                if (!dep.isTransitive) {
                                    val exclusionNode = dependencyNode.appendNode("exclusions").appendNode("exclusion")
                                    exclusionNode.appendNode("groupId", "*")
                                    exclusionNode.appendNode("artifactId", "*")
                                } else if (dep.excludeRules.isNotEmpty()) {
                                    val exclusionNode = dependencyNode.appendNode("exclusions").appendNode("exclusion")
                                    dep.excludeRules.forEach { rule ->
                                        exclusionNode.appendNode("groupId", rule.group)
                                        exclusionNode.appendNode("artifactId", rule.module)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }
}