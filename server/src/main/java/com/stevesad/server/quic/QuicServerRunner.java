package com.stevesad.server.quic;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.netty.Connection;
import reactor.netty.quic.QuicServer;

@Component
@RequiredArgsConstructor
public class QuicServerRunner implements ApplicationRunner {

    private final QuicServer quicServer;

    private Connection serverConnection;

    @Override
    public void run(ApplicationArguments args) {
        serverConnection = quicServer.bindNow();
        serverConnection.onDispose().block();
    }

    @PreDestroy
    public void cleanup() {
        serverConnection.disposeNow();
    }
}
