package com.stevesad.client.storage;

import java.nio.file.Path;
import java.util.List;

public interface LocalStorage {

    void storeProfile(VpnProfile vpnProfile) throws Exception;

    void deleteProfile(VpnProfile vpnProfile) throws Exception;

    List<VpnProfile> loadAllProfiles() throws Exception;

    Path storeTrustedCert(Path cert) throws Exception;

    void deleteTrustedCert(Path cert) throws Exception;

    List<Path> loadAllTrustedCerts() throws Exception;
}
