package com.stevesad.common.utils;

import io.netty.buffer.ByteBuf;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PacketUtils {

    private static final byte[] ipV4AddressBuffer = new byte[4];

    public static InetAddress extractIpV4SrcAddress(ByteBuf buf) throws UnknownHostException {
        buf.getBytes(12, ipV4AddressBuffer, 0, 4);
        return Inet4Address.getByAddress(ipV4AddressBuffer);
    }

    public static InetAddress extractIpV4DstAddress(ByteBuf buf) throws UnknownHostException {
        buf.getBytes(16, ipV4AddressBuffer, 0, 4);
        return Inet4Address.getByAddress(ipV4AddressBuffer);
    }
}
