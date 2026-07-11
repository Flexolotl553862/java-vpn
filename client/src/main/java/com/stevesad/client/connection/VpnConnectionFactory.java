package com.stevesad.client.connection;

import com.stevesad.client.storage.VpnProfile;

public interface VpnConnectionFactory {

    VpnConnection openConnection(VpnProfile profile) throws Exception;
}
