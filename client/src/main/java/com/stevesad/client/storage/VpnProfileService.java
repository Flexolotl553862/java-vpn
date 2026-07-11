package com.stevesad.client.storage;

import java.util.List;

public interface VpnProfileService {

    void store(VpnProfile vpnProfile) throws Exception;

    void delete(VpnProfile vpnProfile) throws Exception;

    VpnProfile loadProfileByName(String name) throws Exception;

    List<VpnProfile> loadAll() throws Exception;
}
