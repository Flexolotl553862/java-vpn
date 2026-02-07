package com.stevesad.server.udp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public record UdpClientDetails(InetSocketAddress inetAddress, InetAddress clientAddress) {}
