plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree("$projectDir/lib") { include("*.jar") })
    implementation("io.swagger:swagger-annotations:1.6.8")
    implementation("io.swagger:swagger-core:1.6.11")
    implementation("io.swagger:swagger-models:1.6.8")
    implementation("org.webjars:swagger-ui:4.18.1")
    implementation("io.swagger:swagger-jaxrs:1.6.8")

    testImplementation(fileTree("$projectDir/testlib") { include("*.jar") })
}

sourceSets {
    main {
        java {
            srcDirs("src/java")
        }
    }
    test {
        java {
            srcDirs("test/java")
        }
    }
}

application {
    mainClass.set("nxt.Nxt")
}

tasks.startScripts { isEnabled = false }

distributions {
    main {
        contents {
            from("conf") {
                include(
                    "fimk-default.properties",
                    "embedded-template.properties",
                    "logging-default.properties"
                )
                into("conf")
            }
            from("html") { into("html") }
            from("logs") { include("placeholder.txt"); into("logs") }
            from("$projectDir/run.bat")
            from("$projectDir/run.sh")
            from("$projectDir/LICENSE.txt")
            from("$projectDir/README.txt")
        }
    }
}