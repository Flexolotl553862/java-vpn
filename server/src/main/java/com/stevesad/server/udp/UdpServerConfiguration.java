package com.stevesad.server.udp;

import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.common.utils.PacketUtils;
import com.stevesad.server.properties.ServerProperties;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.netty.udp.UdpServer;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ServerProperties.class)
public class UdpServerConfiguration {

    private final ServerProperties properties;
    private final TunPacketPublisher tunPacketPublisher;
    private final TunPacketConsumer tunPacketConsumer;

    private final Map<InetAddress, UdpClientDetails> detailsByClientAddressMap = new ConcurrentHashMap<>();

    @Bean
    public UdpServer udpServer() {
        return UdpServer.create()
                .host(properties.getHost())
                .port(properties.getPort())
                .doOnBound(_ -> log.info("Started UDP server on {}:{}", properties.getHost(), properties.getPort()))
                .doOnUnbound(_ -> log.info("Shutdown UDP server"))
                .handle((in, out) -> {
                    var toTunFlux = in.receiveObject().doOnNext(o -> {
                        if (o instanceof DatagramPacket datagramPacket) {
                            log.debug(
                                    "Received {} bytes packet from client with address {}:{}",
                                    datagramPacket.content().readableBytes(),
                                    datagramPacket.sender().getHostName(),
                                    datagramPacket.sender().getPort());

                            // FIXME: only for testing, client have to confirm your internal address
                            try {
                                var packet = datagramPacket.content();
                                packet.retain();
                                var clientAddress = PacketUtils.getSrc(packet);
                                detailsByClientAddressMap.put(
                                        clientAddress, new UdpClientDetails(datagramPacket.sender(), clientAddress));
                                tunPacketConsumer.handleSingle(packet);
                            } catch (Exception e) {
                                log.warn(
                                        "Failed to parse incoming ip packet from {}:{}",
                                        datagramPacket.sender().getHostName(),
                                        datagramPacket.sender().getPort());
                            }
                        } else {
                            log.error("Unexpected type of incoming message, not Datagram");
                        }
                    });

                    var fromTunFlux =
                            out.sendObject(tunPacketPublisher.subscribeSingle().flatMap(buf -> {
                                try {
                                    var clientAddress = PacketUtils.getDst(buf);
                                    log.debug(
                                            "Send {} bytes packet to client {}",
                                            buf.readableBytes(),
                                            clientAddress.getHostAddress());

                                    var details = detailsByClientAddressMap.get(clientAddress);

                                    if (details == null) {
                                        log.warn("No details for client {} found", clientAddress.getHostAddress());
                                        return Mono.empty();
                                    }

                                    return Mono.just(new DatagramPacket(buf, details.inetAddress()));
                                } catch (Exception e) {
                                    log.warn("Failed to extract dst address", e);
                                    return Mono.empty();
                                }
                            }));

                    return Mono.when(fromTunFlux, toTunFlux);
                });
    }
}
