import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven{
        url=uri("https://repo1.maven.org/maven2/")
    }


}



dependencies {
    testImplementation(kotlin("test"))

    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.2")
    implementation("org.eclipse.platform:org.eclipse.core.runtime:3.25.0")
    implementation(files("swt-module.jar"))
}



tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}