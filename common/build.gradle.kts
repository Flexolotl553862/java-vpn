plugins {
    java
    id("io.spring.dependency-management") version "1.1.7"
    id("java-library")
}

group = "com.stevesad"
description = "common"

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.2")
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-logging")

    // Bouncy Castle
    api("org.bouncycastle:bcprov-jdk18on:1.84")
    api("org.bouncycastle:bcpkix-jdk18on:1.84")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("io.projectreactor.netty:reactor-netty-quic")

    // Pcap4j
    implementation("org.pcap4j:pcap4j-core:1.8.2")
    implementation("org.pcap4j:pcap4j-packetfactory-static:1.8.2")

    // Native lib loader
    implementation("org.scijava:native-lib-loader:2.5.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    useJUnitPlatform()
}
