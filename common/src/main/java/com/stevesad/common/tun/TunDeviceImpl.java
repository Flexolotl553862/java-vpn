package com.stevesad.common.tun;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.scijava.nativelib.NativeLibraryUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Slf4j
@Component
@ConditionalOnProperty(name = "tun.mock", havingValue = "none")
public class TunDeviceImpl implements TunDevice {

    private final TunDeviceProperties properties;

    public void loadNativeLib() {
        Path resource = Paths.get(
                "natives",
                NativeLibraryUtil.getArchitecture().toString().toLowerCase(Locale.ENGLISH),
                System.mapLibraryName(properties.getLibName()));

        try (InputStream lib = TunDeviceImpl.class.getClassLoader().getResourceAsStream(resource.toString())) {
            if (lib == null) {
                throw new IOException("Failed to extract native libs as resource");
            }

            Path tmpFile = Files.createTempDirectory("tmp")
                    .toAbsolutePath()
                    .resolve(resource.getFileName())
                    .toAbsolutePath();

            Files.createFile(tmpFile);
            lib.transferTo(Files.newOutputStream(tmpFile));

            System.load(tmpFile.toString());
        } catch (IOException e) {
            log.error("Failed to load native lib", e);
        }
    }

    public TunDeviceImpl(TunDeviceProperties properties) throws IOException {
        this.properties = properties;
        loadNativeLib();

        String[] octets = properties.getAddress().getHostAddress().split("\\.");
        int convertedIp = 0;

        for (String octet : octets) {
            convertedIp = (convertedIp << 8) | Integer.parseInt(octet);
        }

        open(convertedIp, properties.getMaskLength(), properties.getMtu());
        log.info("Opened Tunnel on {}/{}", properties.getAddress().getHostAddress(), properties.getMaskLength());
    }

    public native int open(int address, int maskLength, int mtu) throws IOException;

    @Override
    @PreDestroy
    public native void close();

    @Override
    public native int receive(ByteBuffer readBuffer, int writerIndex);

    @Override
    public native int send(ByteBuffer packetBuffer, int readerIndex, int readableBytes);
}
