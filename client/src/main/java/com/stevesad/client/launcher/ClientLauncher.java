package com.stevesad.client.launcher;

import com.stevesad.client.quic.QuicClientFactory;
import com.stevesad.client.route.RouteManager;
import com.stevesad.client.storage.VpnProfile;
import com.stevesad.common.launcher.TransactionalLauncher;
import com.stevesad.common.launcher.TunLauncher;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.netty.quic.QuicClient;
import reactor.netty.quic.QuicConnection;

import java.net.InetAddress;

@Component
@RequiredArgsConstructor
public class ClientLauncher extends TransactionalLauncher {

    private final TunLauncher tunLauncher;
    private final RouteManager routeManager;
    private final QuicClientFactory quicClientFactory;

    private volatile QuicConnection currentConnection;
    private volatile Disposable currentStreamSubscription;
    private volatile VpnProfile currentProfile;

    public synchronized void start(VpnProfile profile) throws Exception {
        if (currentProfile != null) {
            stop();
        }

        currentProfile = profile;

        String gateway = routeManager.getGateway(profile.getServerHost());
        InetAddress clientAddress = profile.getClientAddress();
        InetAddress serverAddress = InetAddress.getByName(profile.getServerHost());
        QuicClient client = quicClientFactory.createClient(profile);

        // Connect to remote server
        super.addAction(
                () -> {
                    currentConnection = client.connectNow();
                    currentStreamSubscription = currentConnection
                            .createStream(quicClientFactory.createHandler())
                            .subscribe();
                    currentConnection.onDispose().doFinally(_ -> stop());
                },
                () -> {
                    if (currentConnection != null) {
                        currentConnection.disposeNow();
                        currentConnection = null;
                    }

                    if (currentStreamSubscription != null) {
                        currentStreamSubscription.dispose();
                        currentStreamSubscription = null;
                    }
                });

        // Launch Tun device
        super.addAction(() -> tunLauncher.start(clientAddress.getHostAddress(), 32, 1400), tunLauncher::stop);

        // Set route for server connection
        super.addAction(
                () -> routeManager.addRoute(serverAddress.getHostAddress() + "/32", gateway),
                () -> routeManager.deleteRoute(serverAddress.getHostAddress() + "/32", gateway));

        // Add routes
        for (String route : profile.getRoutes()) {
            super.addAction(
                    () -> routeManager.addRoute(route, clientAddress.getHostAddress()),
                    () -> routeManager.deleteRoute(route, clientAddress.getHostAddress()));
        }

        // Execute transaction
        super.executeSequence();
    }

    @PreDestroy
    public synchronized void stop() {
        currentProfile = null;
        super.rollbackSequence();
    }
}
