package com.stevesad.client.route;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class LinuxRouteManager implements RouteManager {

    @Override
    public void addRoute(String route, String gateway) throws Exception {
        CommandLineExecutor.execute(List.of("ip", "route", "add", route, "via", gateway));
    }

    @Override
    public void deleteRoute(String route, String gateway) throws Exception {
        CommandLineExecutor.execute(List.of("ip", "route", "delete", route, "via", gateway));
    }

    @Override
    public String getGateway(String host) throws Exception {
        String address = InetAddress.getByName(host).getHostAddress();

        String output = CommandLineExecutor.execute(List.of("ip", "route", "get", address));

        String firstLine = output.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElseThrow(() -> new IOException("Route not found for host: " + host));

        String[] tokens = firstLine.split("\\s+");

        for (int i = 0; i < tokens.length - 1; i++) {
            if ("via".equals(tokens[i])) {
                return tokens[i + 1];
            }
        }

        throw new IOException("Gateway not found; destination may be directly connected: " + firstLine);
    }
}
