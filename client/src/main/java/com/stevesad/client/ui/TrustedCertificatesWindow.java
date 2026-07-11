package com.stevesad.client.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;

final class TrustedCertificatesWindow {

    private final Window owner;
    private final ObservableList<Path> certificates;
    private final Stage stage = new Stage();
    private final ListView<Path> certificateList;
    private final TextField certificatePathField = new TextField();
    private final Button addButton = new Button("Add certificate");
    private final Button deleteButton = new Button("Delete certificate");

    TrustedCertificatesWindow(Window owner, ObservableList<Path> certificates) {
        this.owner = owner;
        this.certificates = certificates;
        this.certificateList = new ListView<>(certificates);
        configureStage();
    }

    void show() {
        if (stage.isShowing()) {
            stage.requestFocus();
            return;
        }

        stage.show();
    }

    private void configureStage() {
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("trust store Settings");
        stage.setMinWidth(560);
        stage.setMinHeight(420);
        stage.setScene(new Scene(createContent(), 620, 460));
    }

    private VBox createContent() {
        Label title = new Label("Trusted Certificates");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");

        certificatePathField.setPromptText("Certificate file");
        certificatePathField.setEditable(false);

        certificateList.setPlaceholder(new Label("No trusted certificates"));
        certificateList.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        certificateList.getSelectionModel().selectedItemProperty().addListener((_, _, selected) -> {
            showCertificate(selected);
            updateButtonState();
        });
        VBox.setVgrow(certificateList, Priority.ALWAYS);

        Button chooseButton = new Button("Choose");
        chooseButton.setOnAction(_ -> chooseCertificate());

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        addFormRow(form, 0, "Certificate", certificatePathField, chooseButton);

        certificatePathField.textProperty().addListener((_, _, _) -> updateButtonState());

        addButton.setOnAction(_ -> addCertificate());
        deleteButton.setOnAction(_ -> deleteCertificate());

        HBox actions = new HBox(12, addButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(14, title, certificateList, form, actions);
        content.setPadding(new Insets(22));
        content.setAlignment(Pos.TOP_LEFT);
        updateButtonState();
        return content;
    }

    private void addFormRow(GridPane form, int row, String labelText, Region field, Button button) {
        Label label = new Label(labelText);
        label.setMinWidth(90);
        GridPane.setHgrow(field, Priority.ALWAYS);

        form.add(label, 0, row);
        form.add(field, 1, row);

        if (button != null) {
            form.add(button, 2, row);
        } else {
            Region spacer = new Region();
            spacer.setMinWidth(92);
            form.add(spacer, 2, row);
        }
    }

    private void chooseCertificate() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose trusted certificate");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PEM files", "*.pem"));

        File selected = fileChooser.showOpenDialog(stage);
        if (selected != null) {
            certificatePathField.setText(selected.toPath().toString());
        }
    }

    private void addCertificate() {
        Path certificate = Path.of(certificatePathField.getText());
        certificates.add(certificate);
        clearForm();
    }

    private void deleteCertificate() {
        Path selected = certificateList.getSelectionModel().getSelectedItem();
        if (selected == null || !confirmCertificateDeletion(selected)) {
            return;
        }

        certificates.remove(selected);
        clearForm();
    }

    private boolean confirmCertificateDeletion(Path certificate) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Delete trusted certificate");
        alert.setHeaderText("Delete trusted certificate?");
        alert.setContentText(certificate.toString());

        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void showCertificate(Path certificate) {
        if (certificate == null) {
            return;
        }

        certificatePathField.setText(certificate.toString());
    }

    private void clearForm() {
        certificatePathField.clear();
        certificateList.getSelectionModel().clearSelection();
        updateButtonState();
    }

    private void updateButtonState() {
        addButton.setDisable(certificatePathField.getText().isBlank());
        deleteButton.setDisable(certificateList.getSelectionModel().getSelectedItem() == null);
    }
}
