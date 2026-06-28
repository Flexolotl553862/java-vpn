plugins {
    java
    id("io.spring.dependency-management") version "1.1.7"
    id("org.springframework.boot") version "4.1.0"
}

group = "com.stevesad"
description = "server"

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("io.projectreactor:reactor-bom:2025.0.3")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-logging")

    implementation("io.projectreactor.netty:reactor-netty:1.3.6")
    implementation("io.netty:netty-pkitesting")

    implementation("org.pcap4j:pcap4j-packetfactory-static:1.8.2")

    implementation(project(":common"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
