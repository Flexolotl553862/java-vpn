package com.stevesad.client.route;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CommandLineExecutor {

    public static String execute(List<String> command) throws IOException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        String output;
        try (var input = process.getInputStream()) {
            output = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while executing route command", e);
        }

        if (exitCode != 0) {
            throw new IOException("Command failed with exit code " + exitCode + ": "
                    + String.join(" ", command) + (output.isEmpty() ? "" : System.lineSeparator() + output));
        }

        return output;
    }
}
