package com.stevesad.client.storage;

import java.nio.file.Path;
import java.util.List;

public interface LocalStorage {

    void storeProfile(VpnProfile vpnProfile) throws Exception;

    void deleteProfile(VpnProfile vpnProfile) throws Exception;

    List<VpnProfile> loadAllProfiles() throws Exception;

    void storeTrustCert(Path cert) throws Exception;

    void deleteTrustCert(Path cert) throws Exception;

    List<Path> loadAllTrustCerts() throws Exception;
}
