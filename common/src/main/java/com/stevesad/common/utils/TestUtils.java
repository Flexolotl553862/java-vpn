package com.stevesad.common.utils;

import io.netty.buffer.ByteBuf;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;

import java.net.Inet4Address;

public class TestUtils {

    public static IpV4Packet createUdpPacket(byte[] data, String src, String dst) {

        var udpBuilder = new UdpPacket.Builder()
                .payloadBuilder(new UnknownPacket.Builder().rawData(data))
                .srcAddr(Inet4Address.ofLiteral(src))
                .dstAddr(Inet4Address.ofLiteral(dst))
                .srcPort(UdpPort.HELLO_PORT)
                .dstPort(UdpPort.HELLO_PORT)
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true);

        return new IpV4Packet.Builder()
                .version(IpVersion.IPV4)
                .tos(IpV4Rfc791Tos.newInstance((byte) 0))
                .ttl((byte) 100)
                .protocol(IpNumber.UDP)
                .srcAddr(Inet4Address.ofLiteral(src))
                .dstAddr(Inet4Address.ofLiteral(dst))
                .payloadBuilder(udpBuilder)
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .dontFragmentFlag(true)
                .build();

    }

    public static IpV4Packet createIcmpEchoPacket(String src, String dst, short seq, short id) {

        IcmpV4EchoPacket.Builder echo =
                new IcmpV4EchoPacket.Builder()
                        .identifier(id)
                        .sequenceNumber(seq);

        IcmpV4CommonPacket.Builder icmp =
                new IcmpV4CommonPacket.Builder()
                        .type(IcmpV4Type.ECHO)
                        .code(IcmpV4Code.NO_CODE)
                        .payloadBuilder(echo)
                        .correctChecksumAtBuild(true);

        return new IpV4Packet.Builder()
                .version(IpVersion.IPV4)
                .tos(IpV4Rfc791Tos.newInstance((byte) 0))
                .ttl((byte) 100)
                .protocol(IpNumber.ICMPV4)
                .srcAddr(Inet4Address.ofLiteral(src))
                .dstAddr(Inet4Address.ofLiteral(dst))
                .payloadBuilder(icmp)
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .dontFragmentFlag(true)
                .build();
    }

    public static UdpPacket readRawUdpPacket(byte[] data) throws IllegalRawDataException {
        var ipPacket = IpV4Packet.newPacket(data, 0, data.length).getPayload().getRawData();
        return UdpPacket.newPacket(ipPacket, 0, ipPacket.length);
    }

    public static IcmpV4EchoPacket readRawIcmpEchoPacket(byte[] data) throws IllegalRawDataException {
        var ip = IpV4Packet.newPacket(data, 0, data.length).getPayload().getRawData();
        var icmpCommon = IcmpV4CommonPacket.newPacket(ip, 0, ip.length).getPayload().getRawData();
        return IcmpV4EchoPacket.newPacket(icmpCommon, 0, icmpCommon.length);
    }

    public static IpV4Packet revertPacket(ByteBuf buf) throws Exception {

        var packet = PacketUtils.getPacket(buf);

        return packet.getBuilder()
                .srcAddr(packet.getHeader().getDstAddr())
                .dstAddr(packet.getHeader().getSrcAddr())
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .build();
    }
}
