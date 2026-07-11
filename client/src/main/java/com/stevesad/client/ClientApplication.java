package com.stevesad.client;

import com.stevesad.client.ui.ClientUiApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientApplication {

    static void main(String[] args) {
        Application.launch(ClientUiApplication.class, args);
    }
}