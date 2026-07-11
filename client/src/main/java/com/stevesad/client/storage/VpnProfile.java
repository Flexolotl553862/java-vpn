package com.stevesad.client.storage;

import lombok.*;

import java.nio.file.Path;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnProfile {

    private  String name;

    private String serverHost;

    private int serverPort;

    private Path certificatePath;

    private Path privateKeyPath;

    private List<String> routes;

    @Override
    public String toString() {
        return name;
    }
}
