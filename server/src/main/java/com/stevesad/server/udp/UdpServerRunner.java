package com.stevesad.server.udp;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

@Component
@RequiredArgsConstructor
public class UdpServerRunner implements ApplicationRunner {

    private final UdpServer udpServer;

    private Connection serverConnection;

    @Override
    public void run(ApplicationArguments args) {
        serverConnection = udpServer.bindNow();
        serverConnection.onDispose().block();
    }

    @PreDestroy
    public void cleanup() {
        serverConnection.disposeNow();
    }
}
