import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"

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

    implementation("org.eclipse.platform:org.eclipse.core.runtime:3.25.0")
    implementation(files("libs/swt.jar"))
    implementation(kotlin("reflect"))
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.24.8")
    implementation(files("libs/javardise-1.0.2.jar"))

    // https://mvnrepository.com/artifact/org.eclipse.platform/org.eclipse.jface
    implementation("org.eclipse.platform:org.eclipse.jface:3.13.2")

}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (this.requested.name.contains("\${osgi.platform}")) {
                this.useTarget(
                    this.requested.toString()
                        .replace("\${osgi.platform}", "win32.win32.x86_64")
                )
            }
        }
    }
}



tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}