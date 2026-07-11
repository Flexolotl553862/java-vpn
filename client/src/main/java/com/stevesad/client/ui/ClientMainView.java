package com.stevesad.client.ui;

import com.stevesad.client.connection.VpnConnection;
import com.stevesad.client.connection.VpnConnectionFactory;
import com.stevesad.client.storage.VpnProfile;
import com.stevesad.client.storage.VpnProfileService;
import com.stevesad.common.consumer.TunPacketConsumer;
import com.stevesad.common.publisher.TunPacketPublisher;
import com.stevesad.common.tun.TunDevice;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Slf4j
final class ClientMainView {

    private static final String DEFAULT_ROUTES = """
        127.0.0.0/1
        128.0.0.0/1
        """;

    private final Window owner;
    private final BorderPane root = new BorderPane();
    private final ObservableList<VpnProfile> profiles = FXCollections.observableArrayList();
    private final ListView<VpnProfile> profileList = new ListView<>(profiles);
    private final TextField profileNameField = new TextField();
    private final TextField serverHostField = new TextField();
    private final TextField serverPortField = new TextField();
    private final TextField certificatePathField = new TextField();
    private final TextField privateKeyPathField = new TextField();
    private final TextArea routesArea = new TextArea(DEFAULT_ROUTES);
    private final Label connectionStatusLabel = new Label("Disconnected");
    private final Button chooseCertificateButton = new Button("Choose");
    private final Button choosePrivateKeyButton = new Button("Choose");
    private final Button newButton = new Button("New");
    private final Button saveButton = new Button("Save configuration");
    private final Button deleteButton = new Button("Delete");
    private final Button connectButton = new Button("Connect");
    private final List<Control> profileControls = List.of(
            profileList,
            profileNameField,
            serverHostField,
            serverPortField,
            certificatePathField,
            privateKeyPathField,
            routesArea,
            chooseCertificateButton,
            choosePrivateKeyButton,
            newButton,
            saveButton,
            deleteButton);

    private final VpnProfileService vpnProfileService;
    private final VpnConnectionFactory vpnConnectionFactory;
    private final TunPacketPublisher tunPacketPublisher;
    private final TunPacketConsumer tunPacketConsumer;
    private final TunDevice tunDevice;

    private VpnConnection currentConnection;
    private VpnConnectionState connectionState = VpnConnectionState.DISCONNECTED;

    ClientMainView(
            Window owner,
            VpnProfileService vpnProfileService,
            VpnConnectionFactory vpnConnectionFactory,
            TunPacketPublisher tunPacketPublisher,
            TunPacketConsumer tunPacketConsumer,
            TunDevice tunDevice) {
        this.owner = owner;
        this.vpnProfileService = vpnProfileService;
        this.vpnConnectionFactory = vpnConnectionFactory;
        this.tunPacketPublisher = tunPacketPublisher;
        this.tunPacketConsumer = tunPacketConsumer;
        this.tunDevice = tunDevice;
        configureRoot();
    }

    Parent getRoot() {
        return root;
    }

    private void configureRoot() {
        root.setLeft(createSidebar());
        root.setCenter(createMainPanel());
        root.setStyle("-fx-font-size: 14px; -fx-background-color: #f7f8fa;");
        addProfileFormListeners();
        updateControlsForConnectionState(connectionState);
    }

    private Parent createSidebar() {
        Label title = new Label("Configurations");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");

        profileList.setPlaceholder(new Label("No configurations"));
        profileList.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(VpnProfile item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        profileList.getSelectionModel().selectedItemProperty().addListener((_, _, selected) -> {
            showProfile(selected);
            updateControlsForConnectionState(connectionState);
        });
        VBox.setVgrow(profileList, Priority.ALWAYS);

        VBox sidebar = new VBox(12, title, profileList);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(220);
        sidebar.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: transparent #dde1e7 transparent transparent;");
        return sidebar;
    }

    private Parent createMainPanel() {
        Label title = new Label("Connection Profile");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 700;");

        Label subtitle = new Label("Certificate and private key are stored as a named connection draft.");
        subtitle.setStyle("-fx-text-fill: #5d6673;");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(14);
        form.setMaxWidth(680);

        profileNameField.setPromptText("Profile name");
        serverHostField.setPromptText("Server host");
        serverPortField.setPromptText("Port");
        certificatePathField.setPromptText("Certificate file");
        certificatePathField.setEditable(false);
        privateKeyPathField.setPromptText("Private key file");
        privateKeyPathField.setEditable(false);
        routesArea.setPromptText("Routes, one CIDR per line");
        routesArea.setPrefRowCount(4);

        chooseCertificateButton.setOnAction(_ -> chooseCertificate());

        choosePrivateKeyButton.setOnAction(_ -> choosePrivateKey());

        addFormRow(form, 0, "Name", profileNameField, null);
        addFormRow(form, 1, "Server", createServerFields(), null);
        addFormRow(form, 2, "Certificate", certificatePathField, chooseCertificateButton);
        addFormRow(form, 3, "Private key", privateKeyPathField, choosePrivateKeyButton);
        addFormRow(form, 4, "Routes", routesArea, null);

        newButton.setOnAction(_ -> clearProfileForm());

        saveButton.setOnAction(_ -> saveProfile());

        deleteButton.setOnAction(_ -> deleteSelectedProfile());

        connectButton.setDisable(true);
        connectButton.setOnAction(_ -> toggleConnection());

        HBox actions = new HBox(12, newButton, saveButton, deleteButton, connectButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        HBox status = new HBox(8, new Label("Status:"), connectionStatusLabel);
        status.setAlignment(Pos.CENTER_LEFT);
        connectionStatusLabel.setStyle("-fx-font-weight: 700;");

        VBox panel = new VBox(16, title, subtitle, new Separator(), form, actions, status);
        panel.setPadding(new Insets(28));
        panel.setAlignment(Pos.TOP_LEFT);
        return panel;
    }

    private void addProfileFormListeners() {
        profileNameField.textProperty().addListener((_, _, _) -> updateControlsForConnectionState(connectionState));
        serverHostField.textProperty().addListener((_, _, _) -> updateControlsForConnectionState(connectionState));
        serverPortField.textProperty().addListener((_, _, _) -> updateControlsForConnectionState(connectionState));
        certificatePathField.textProperty().addListener((_, _, _) -> updateControlsForConnectionState(connectionState));
        privateKeyPathField.textProperty().addListener((_, _, _) -> updateControlsForConnectionState(connectionState));
        routesArea.textProperty().addListener((_, _, _) -> updateControlsForConnectionState(connectionState));
    }

    private HBox createServerFields() {
        serverHostField.setMinHeight(34);
        serverPortField.setMinHeight(34);
        serverPortField.setPrefWidth(110);
        serverPortField.setMaxWidth(130);
        HBox.setHgrow(serverHostField, Priority.ALWAYS);

        HBox row = new HBox(10, serverHostField, serverPortField);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void addFormRow(GridPane form, int row, String labelText, Region field, Button button) {
        Label label = new Label(labelText);
        label.setMinWidth(90);

        if (field instanceof TextField textField) {
            textField.setMinHeight(34);
        }
        GridPane.setHgrow(field, Priority.ALWAYS);

        form.add(label, 0, row);
        form.add(field, 1, row);

        if (button != null) {
            button.setMinHeight(34);
            form.add(button, 2, row);
        } else {
            Region spacer = new Region();
            spacer.setMinWidth(82);
            form.add(spacer, 2, row);
        }
    }

    private void chooseCertificate() {
        File selected = chooseFile("Choose certificate");
        if (selected != null) {
            certificatePathField.setText(selected.toPath().toString());
        }
    }

    private void choosePrivateKey() {
        File selected = chooseFile("Choose private key");
        if (selected != null) {
            privateKeyPathField.setText(selected.toPath().toString());
        }
    }

    private File chooseFile(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PEM files", "*.pem"));
        return fileChooser.showOpenDialog(owner);
    }

    private void saveProfile() {
        VpnProfile selectedProfile = profileList.getSelectionModel().getSelectedItem();
        VpnProfile profile = new VpnProfile(
                profileNameField.getText().trim(),
                serverHostField.getText().trim(),
                Integer.parseInt(serverPortField.getText().trim()),
                Path.of(certificatePathField.getText()),
                Path.of(privateKeyPathField.getText()),
                getRoutes());

        try {
            vpnProfileService.store(profile);
            if (selectedProfile != null && !selectedProfile.getName().equals(profile.getName())) {
                vpnProfileService.delete(selectedProfile);
            }
        } catch (Exception e) {
            showError("Failed to save configuration", e);
            return;
        }

        int selectedIndex = profileList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            profiles.set(selectedIndex, profile);
        } else {
            profiles.add(profile);
        }

        profileList.getSelectionModel().select(profile);
        updateControlsForConnectionState(VpnConnectionState.DISCONNECTED);
    }

    private void deleteSelectedProfile() {
        VpnProfile selected = profileList.getSelectionModel().getSelectedItem();
        if (selected == null || !confirmProfileDeletion(selected)) {
            return;
        }

        try {
            vpnProfileService.delete(selected);
            profiles.remove(selected);
            clearProfileForm();
        } catch (Exception e) {
            showError("Exception while delete profile", e);
        }
    }

    private boolean confirmProfileDeletion(VpnProfile profile) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("Delete configuration");
        alert.setHeaderText("Delete \"" + profile.getName() + "\"?");
        alert.setContentText("This configuration will be removed from the list.");

        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void showProfile(VpnProfile profile) {
        if (profile == null) {
            return;
        }

        profileNameField.setText(profile.getName());
        serverHostField.setText(profile.getServerHost());
        serverPortField.setText(String.valueOf(profile.getServerPort()));
        certificatePathField.setText(profile.getCertificatePath().toString());
        privateKeyPathField.setText(profile.getPrivateKeyPath().toString());
        routesArea.setText(String.join(System.lineSeparator(), profile.getRoutes()));
        connectButton.setDisable(false);
        updateControlsForConnectionState(VpnConnectionState.DISCONNECTED);
    }

    private void clearProfileForm() {
        profileNameField.clear();
        serverHostField.clear();
        serverPortField.clear();
        certificatePathField.clear();
        privateKeyPathField.clear();
        routesArea.setText(DEFAULT_ROUTES);
        profileList.getSelectionModel().clearSelection();
        setConnectionState(VpnConnectionState.DISCONNECTED);
    }

    private void toggleConnection() {
        if (currentConnection != null) {
            disconnectCurrentProfile();
            return;
        }

        VpnProfile selected = profileList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        connectSelectedProfile(selected);
    }

    private void connectSelectedProfile(VpnProfile selected) {
        setConnectionState(VpnConnectionState.CONNECTING);
        connectButton.setDisable(true);

        try {
            if (currentConnection != null) {
                closeCurrentConnection();
            }

            tunDevice.start();
            tunPacketPublisher.startPollingLoop();
            tunPacketConsumer.startPollingLoop();
            currentConnection = vpnConnectionFactory.openConnection(selected);

            connectButton.setDisable(false);
            setConnectionState(VpnConnectionState.CONNECTED);
        } catch (Exception e) {
            connectButton.setDisable(false);
            setConnectionState(VpnConnectionState.DISCONNECTED);
            showError("Failed to connect to server", e);
        }
    }

    private void disconnectCurrentProfile() {
        connectButton.setDisable(true);
        closeCurrentConnection();
        connectButton.setDisable(false);
        setConnectionState(VpnConnectionState.DISCONNECTED);
    }

    private void setConnectionState(VpnConnectionState state) {
        connectionState = state;

        switch (state) {
            case DISCONNECTED -> {
                connectionStatusLabel.setText("Disconnected");
                connectButton.setText("Connect");
            }
            case CONNECTING -> {
                connectionStatusLabel.setText("Connecting");
                connectButton.setText("Connecting");
            }
            case CONNECTED -> {
                connectionStatusLabel.setText("Connected");
                connectButton.setText("Disconnect");
            }
        }

        updateControlsForConnectionState(state);
    }

    private void updateControlsForConnectionState(VpnConnectionState state) {
        boolean connectedOrConnecting = state != VpnConnectionState.DISCONNECTED;
        profileControls.forEach(control -> control.setDisable(connectedOrConnecting));
        saveButton.setDisable(connectedOrConnecting || isProfileIncomplete());
        deleteButton.setDisable(
                connectedOrConnecting || profileList.getSelectionModel().getSelectedItem() == null);

        if (state == VpnConnectionState.CONNECTING) {
            connectButton.setDisable(true);
            return;
        }

        if (state == VpnConnectionState.CONNECTED) {
            connectButton.setDisable(false);
            return;
        }

        connectButton.setDisable(profileList.getSelectionModel().getSelectedItem() == null);
    }

    private boolean isProfileIncomplete() {
        return profileNameField.getText().isBlank()
                || serverHostField.getText().isBlank()
                || serverPortField.getText().isBlank()
                || !isServerPortValid()
                || certificatePathField.getText().isBlank()
                || privateKeyPathField.getText().isBlank();
    }

    private boolean isServerPortValid() {
        try {
            int port = Integer.parseInt(serverPortField.getText().trim());
            return port > 0 && port <= 65_535;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    void loadProfiles() {
        try {
            profiles.setAll(vpnProfileService.loadAll());
        } catch (Exception e) {
            showError("Exception while loading existing profiles", e);
        }
    }

    private List<String> getRoutes() {
        return routesArea
                .getText()
                .lines()
                .map(String::trim)
                .filter(route -> !route.isBlank())
                .toList();
    }

    private void showError(String title, Exception e) {
        log.error("Exception occurred", e);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (owner.getScene() != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(e.getClass() + ": " + e.getMessage());
        alert.showAndWait();
    }

    void closeCurrentConnection() {
        if (currentConnection == null) {
            return;
        }

        try {
            currentConnection.close();
            tunPacketPublisher.stopPollingLoop();
            tunPacketConsumer.stopPollingLoop();
            setConnectionState(VpnConnectionState.DISCONNECTED);
            currentConnection = null;
        } catch (Exception e) {
            showError("Failed to close connection", e);
        }
    }
}
