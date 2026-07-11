package com.stevesad.client.ui;

import com.stevesad.client.ClientApplication;
import com.stevesad.client.connection.VpnConnectionFactory;
import com.stevesad.client.storage.LocalStorage;
import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.common.tun.TunDevice;
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
        LocalStorage localStorage = applicationContext.getBean(LocalStorage.class);
        VpnConnectionFactory vpnConnectionFactory = applicationContext.getBean(VpnConnectionFactory.class);

        TunPacketPublisher tunPacketPublisher = applicationContext.getBean(TunPacketPublisher.class);
        TunPacketConsumer tunPacketConsumer = applicationContext.getBean(TunPacketConsumer.class);
        TunDevice tunDevice = applicationContext.getBean(TunDevice.class);

        mainView = new ClientMainView(stage, localStorage, vpnConnectionFactory, tunPacketPublisher, tunPacketConsumer, tunDevice);
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
            mainView.closeCurrentConnection();
        }
        if (applicationContext != null) {
            applicationContext.close();
        }
    }
}
