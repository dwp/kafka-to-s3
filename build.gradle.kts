import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.40"
    application
}

group = "uk.gov.dwp.dataworks"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.amazonaws:aws-java-sdk-s3:1.11.706")
    implementation("com.amazonaws:aws-java-sdk-core:1.11.706")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.2.2")
    implementation("org.apache.kafka", "kafka-clients", "2.3.0")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("org.apache.commons", "commons-text", "1.8")
    testImplementation("junit:junit:4.12")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("io.kotlintest", "kotlintest-runner-junit5", "3.3.2")

}

application {
    mainClassName = "Kafka2S3Kt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

sourceSets {
    create("integration") {
        java.srcDir(file("src/integration/kotlin"))
        compileClasspath += sourceSets.getByName("main").output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }

    create("unit") {
        java.srcDir(file("src/test/kotlin"))
        compileClasspath += sourceSets.getByName("main").output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
}

tasks.register<Test>("integration") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integration"].output.classesDirs
    classpath = sourceSets["integration"].runtimeClasspath
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
    }
}

tasks.register<Test>("unit") {
    description = "Runs the unit tests"
    group = "verification"
    testClassesDirs = sourceSets["unit"].output.classesDirs
    classpath = sourceSets["unit"].runtimeClasspath
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED)
    }
}
