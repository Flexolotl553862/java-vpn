package com.stevesad.client.connection;

public interface VpnConnection {

    void blockUntilClose();

    void close() throws Exception;
}
