plugins {
    kotlin("jvm") version "1.3.72"
}

group = "com.github.shk0da.rsx"
version = "0.1"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.shk0da:yahoofinance:0.1.2")
    implementation("org.xerial:sqlite-jdbc:3.31.1")
    implementation("org.slf4j:jcl-over-slf4j:1.7.30")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}

val uberJar = tasks.register<Jar>("uberJar") {
    manifest {
        attributes(
            "Main-Class" to "com.github.shk0da.rsx.Application"
        )
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

tasks {
    "build" {
        dependsOn(uberJar)
    }
}