package com.stevesad.client.route;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockRouteManager implements RouteManager {

    @Override
    public void addRoute(String route, String gateway) {
        log.info("route add {} via {}", route, gateway);
    }

    @Override
    public void deleteRoute(String route, String gateway) {
        log.info("route delete {} via {}", route, gateway);
    }

    @Override
    public String getGateway(String host) {
        return "192.168.0.1";
    }
}
