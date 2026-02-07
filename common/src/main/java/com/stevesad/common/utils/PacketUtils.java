package com.stevesad.common.utils;

import io.netty.buffer.ByteBuf;
import org.pcap4j.packet.IpV4Packet;

import java.net.InetAddress;

public class PacketUtils {

    public static InetAddress getDst(ByteBuf buf) throws Exception {
        return getPacket(buf).getHeader().getDstAddr();
    }

    public static InetAddress getSrc(ByteBuf buf) throws Exception {
        return getPacket(buf).getHeader().getSrcAddr();
    }

    public static IpV4Packet getPacket(ByteBuf buf) throws Exception {
        var packet = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), packet, 0, packet.length);

        return IpV4Packet.newPacket(
                packet,
                0,
                packet.length
        );
    }
}
