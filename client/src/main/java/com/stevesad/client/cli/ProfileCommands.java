package com.stevesad.client.cli;

import com.stevesad.client.storage.LocalStorage;
import com.stevesad.client.storage.VpnProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ProfileCommands {

    private final LocalStorage storage;

    @Command(
            name = {"profile", "list"},
            description = "List saved profiles")
    public String profiles() throws Exception {
        String result = String.join(
                System.lineSeparator(),
                storage.loadAllProfiles().stream().map(VpnProfile::getName).toList());
        return result.isEmpty() ? "No profiles" : result;
    }

    @Command(
            name = {"profile", "show"},
            description = "Show a profile")
    public String show(@Argument(index = 0, description = "Profile name") String name) throws Exception {
        VpnProfile profile = profile(name);
        return """
            Name:               %s
            Address:            %s:%d
            certificate chain:  %s
            private key:        %s
            routes:             %s\
            """.formatted(
                        profile.getName(),
                        profile.getServerHost(),
                        profile.getServerPort(),
                        profile.getCertificateChainPath(),
                        profile.getPrivateKeyPath(),
                        String.join(", ", profile.getRoutes()));
    }

    @Command(
            name = {"profile", "save"},
            description = "Create a new profile")
    public String save(
            @Option(longName = "name", shortName = 'n', description = "Profile name", required = true) String name,
            @Option(longName = "host", shortName = 'h', description = "Server host", required = true) String host,
            @Option(longName = "port", shortName = 'p', description = "Server port", required = true) int port,
            @Option(longName = "chain", shortName = 'c', description = "Certificate chain path", required = true)
                    String certificate,
            @Option(longName = "key", shortName = 'k', description = "Private key path", required = true)
                    String privateKey,
            @Option(
                            longName = "routes",
                            shortName = 'r',
                            description = "Comma-separated routes",
                            defaultValue = "127.0.0.0/1,128.0.0.0/1")
                    String routes)
            throws Exception {
        storage.storeProfile(new VpnProfile(
                name,
                host,
                port,
                Path.of(certificate),
                Path.of(privateKey),
                Arrays.stream(routes.split(","))
                        .map(String::trim)
                        .filter(it -> !it.isEmpty())
                        .toList()));
        return "Saved: " + name;
    }

    @Command(
            name = {"profile", "delete"},
            description = "Delete a profile")
    public String delete(@Argument(index = 0, description = "Profile name") String name) throws Exception {
        storage.deleteProfile(profile(name));
        return "Deleted: " + name;
    }

    @Command(
            name = {"trust", "list"},
            description = "List trusted certificates")
    public String certificates() throws Exception {
        String result = String.join(
                System.lineSeparator(),
                storage.loadAllTrustedCerts().stream().map(Path::toString).toList());
        return result.isEmpty() ? "No trusted certificates" : result;
    }

    @Command(
            name = {"trust", "add"},
            description = "Add a trusted certificate")
    public String addCertificate(@Argument(index = 0, description = "Certificate path") String cert) throws Exception {
        return "Added: " + storage.storeTrustedCert(Path.of(cert));
    }

    @Command(
            name = {"trust", "delete"},
            description = "Delete a trusted certificate")
    public String deleteCertificate(@Argument(index = 0, description = "Stored certificate path") String cert)
            throws Exception {
        storage.deleteTrustedCert(Path.of(cert));
        return "Deleted: " + cert;
    }

    private VpnProfile profile(String name) throws Exception {
        return storage.loadAllProfiles().stream()
                .filter(it -> it.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + name));
    }
}
