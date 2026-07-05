package com.stevesad.server;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;

import java.security.Security;

@SpringBootApplication(exclude = ReactiveUserDetailsServiceAutoConfiguration.class)
@RequiredArgsConstructor
public class ServerApplication {

    static void main(String[] args) {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        SpringApplication.run(ServerApplication.class, args);
    }
}
