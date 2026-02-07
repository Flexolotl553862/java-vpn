package com.stevesad.client.udp;

import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import io.netty.channel.socket.DatagramPacket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.netty.udp.UdpClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RemoteServerProperties.class)
public class UdpClientConfiguration {

    private final RemoteServerProperties remoteServerProperties;
    private final TunPacketPublisher tunPacketPublisher;
    private final TunPacketConsumer tunPacketConsumer;

    @Bean
    public UdpClient udpClient() {
        return reactor.netty.udp.UdpClient.create()
                .host(remoteServerProperties.getRemoteAddress().getHostAddress())
                .port(remoteServerProperties.getRemotePort())
                .doOnConnected(_ -> log.info(
                        "Connected to UDP server {}:{}",
                        remoteServerProperties.getRemoteAddress().getHostAddress(),
                        remoteServerProperties.getRemotePort()))
                .doOnDisconnected(_ -> log.info("UDP server disconnected"))
                .handle((in, out) -> {
                    var fromTunFlux = out.send(tunPacketPublisher
                            .subscribeSingle()
                            .doOnNext(packet -> log.debug(
                                    "Sent {} bytes packet to server {}:{}",
                                    packet.readableBytes(),
                                    remoteServerProperties.getRemoteAddress().getHostAddress(),
                                    remoteServerProperties.getRemotePort())));

                    var toTunFlux = in.receiveObject().doOnNext(o -> {
                        if (o instanceof DatagramPacket datagramPacket) {
                            var packet = datagramPacket.content();
                            packet.retain();
                            tunPacketConsumer.handleSingle(packet);
                        } else {
                            log.error("Unexpected type of incoming message, not Datagram");
                        }
                    });

                    return Mono.when(fromTunFlux, toTunFlux);
                });
    }
}
