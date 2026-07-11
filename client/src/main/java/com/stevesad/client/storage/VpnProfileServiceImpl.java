package com.stevesad.client.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VpnProfileServiceImpl implements VpnProfileService {

    private static final Path ROOT_PATH;
    private static final String PROFILE_FILE = "settings.json";

    private final ObjectMapper objectMapper;

    static {
        Path root = Path.of("/");

        if (System.getProperty("user.home") != null) {
            root = Paths.get(System.getProperty("user.home"));
        }

        ROOT_PATH = root.resolve(".java-vpn");
    }

    @Override
    public void store(VpnProfile profile) throws Exception {
        Path profileDir = ROOT_PATH.resolve(profile.getName());
        Files.createDirectories(profileDir);

        Path cert = Files.copy(
                profile.getCertificatePath(),
                profileDir.resolve(profile.getCertificatePath().getFileName()),
                StandardCopyOption.REPLACE_EXISTING);
        Path privateKey = Files.copy(
                profile.getPrivateKeyPath(),
                profileDir.resolve(profile.getPrivateKeyPath().getFileName()),
                StandardCopyOption.REPLACE_EXISTING);

        Path settings = profileDir.resolve(PROFILE_FILE);
        if (!Files.exists(settings)) {
            Files.createFile(settings);
        }

        profile.setCertificatePath(cert);
        profile.setPrivateKeyPath(privateKey);

        Files.writeString(settings, objectMapper.writeValueAsString(profile));
    }

    @Override
    public void delete(VpnProfile vpnProfile) throws Exception {
        FileSystemUtils.deleteRecursively(ROOT_PATH.resolve(vpnProfile.getName()));
    }

    @Override
    public VpnProfile loadProfileByName(String name) {
        Path profilePath = ROOT_PATH.resolve(name).resolve(PROFILE_FILE);
        return objectMapper.readValue(profilePath, VpnProfile.class);
    }

    @Override
    public List<VpnProfile> loadAll() {
        File[] profileDirs = ROOT_PATH.toFile().listFiles();
        if (profileDirs == null) {
            profileDirs = new File[0];
        }

        List<VpnProfile> profiles = new ArrayList<>();
        for (File profileDir : profileDirs) {
            Path profilePath = profileDir.toPath().resolve(PROFILE_FILE);
            if (profileDir.isDirectory() && Files.isRegularFile(profilePath)) {
                profiles.add(objectMapper.readValue(profilePath, VpnProfile.class));
            }
        }
        return profiles;
    }
}
