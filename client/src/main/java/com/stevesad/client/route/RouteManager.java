package com.stevesad.client.route;

public interface RouteManager {

    void addRoute(String route, String gateway) throws Exception;

    void deleteRoute(String route, String gateway) throws Exception;

    String getGateway(String host) throws Exception;
}
