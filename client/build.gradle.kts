plugins {
    java
    application
    id("io.spring.dependency-management") version "1.1.7"
    id("org.springframework.boot") version "4.1.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.stevesad"
description = "vpn-client"

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

springBoot {
    mainClass.set("com.stevesad.client.ClientApplication")
}

application {
    mainClass.set("com.stevesad.client.ClientApplication")
}

javafx {
    version = "23.0.2"
    modules = listOf("javafx.controls")
}

dependencyManagement {
    imports {
        mavenBom("io.projectreactor:reactor-bom:2025.0.3")
    }
}

dependencies {
    // Spring Boot starter
    implementation("org.springframework:spring-core")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-webclient")

    // Reactor Netty
    implementation("io.projectreactor.netty:reactor-netty-core")
    implementation("io.projectreactor.netty:reactor-netty-quic:1.3.6")

    implementation(project(":common"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("runClientUi") {
    group = "application"
    description = "Runs the JavaFX client UI prototype."
    dependsOn(tasks.named("run"))
}
