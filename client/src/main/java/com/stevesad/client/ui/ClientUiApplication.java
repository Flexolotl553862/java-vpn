package com.stevesad.client.ui;

import com.stevesad.client.ClientApplication;
import com.stevesad.client.connection.VpnConnectionFactory;
import com.stevesad.client.storage.VpnProfileService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class ClientUiApplication extends Application {

    private ConfigurableApplicationContext applicationContext;
    private ClientMainView mainView;

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(ClientApplication.class).run();
    }

    @Override
    public void start(Stage stage) {
        VpnProfileService vpnProfileService = applicationContext.getBean(VpnProfileService.class);
        VpnConnectionFactory vpnConnectionFactory = applicationContext.getBean(VpnConnectionFactory.class);

        mainView = new ClientMainView(stage, vpnProfileService, vpnConnectionFactory);
        Scene scene = new Scene(mainView.getRoot(), 900, 560);

        stage.setTitle("java-vpn Client");
        stage.setMinWidth(760);
        stage.setMinHeight(460);
        stage.setScene(scene);
        stage.show();

        Platform.runLater(mainView::loadProfiles);
    }

    @Override
    public void stop() {
        if (mainView != null) {
            mainView.close();
        }
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
