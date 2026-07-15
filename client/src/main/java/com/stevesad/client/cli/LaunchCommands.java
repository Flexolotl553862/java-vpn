package com.stevesad.client.cli;

import com.stevesad.client.launcher.ClientLauncher;
import com.stevesad.client.storage.LocalStorage;
import com.stevesad.client.storage.VpnProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class LaunchCommands {

    private final LocalStorage storage;
    private final ClientLauncher launcher;
    private VpnProfile connected;

    @Command(name = "connect", description = "Connect using a saved profile")
    public String connect(@Argument(index = 0, description = "Profile name") String name) throws Exception {
        VpnProfile profile = storage.loadAllProfiles().stream()
                .filter(it -> it.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + name));
        launcher.start(profile);
        connected = profile;
        return "Connected to " + profile.getName();
    }

    @Command(name = "disconnect", description = "Disconnect VPN")
    public String disconnect() {
        launcher.stop();
        connected = null;
        return "Disconnected";
    }

    @Command(name = "status", description = "Show connection status")
    public String status() {
        return connected == null ? "Disconnected" : "Connected to " + connected.getName();
    }
}
