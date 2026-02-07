package com.stevesad.client.udp;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.netty.Connection;
import reactor.netty.udp.UdpClient;

@Component
@RequiredArgsConstructor
public class UdpClientRunner implements ApplicationRunner {

    private final UdpClient udpClient;

    private Connection clientConnection;

    @Override
    public void run(ApplicationArguments args) {
        clientConnection = udpClient.connectNow();
        clientConnection.onDispose().block();
    }

    @PreDestroy
    public void shutDown() {
        clientConnection.disposeNow();
    }
}
