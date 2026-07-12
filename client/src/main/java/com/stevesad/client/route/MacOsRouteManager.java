package com.stevesad.client.route;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class MacOsRouteManager implements RouteManager {

    @Override
    public void addRoute(String route, String gateway) throws Exception {
        CommandLineExecutor.execute(List.of("/sbin/route", "-n", "add", "-net", route, gateway));
    }

    @Override
    public void deleteRoute(String route, String gateway) throws Exception {
        CommandLineExecutor.execute(List.of("/sbin/route", "-n", "delete", "-net", route, gateway));
    }

    @Override
    public String getGateway(String host) throws Exception {
        String address = InetAddress.getByName(host).getHostAddress();

        String output = CommandLineExecutor.execute(List.of("/sbin/route", "-n", "get", address));

        return output.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("gateway:"))
                .map(line -> line.substring("gateway:".length()).trim())
                .findFirst()
                .orElseThrow(() -> new IOException("Gateway not found for host: " + host));
    }
}
