package com.stevesad.client.cli;

import org.jline.utils.AttributedString;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.jline.PromptProvider;

@Configuration
public class CliConfiguration {

    @Bean
    PromptProvider promptProvider() {
        return () -> new AttributedString("java-vpn-client:> ");
    }
}
