package io.github.gvergine.ifsx.gui;

import io.github.gvergine.ifsx.core.executor.SdpToolExecutor;
import io.github.gvergine.ifsx.core.hooks.HookRunner;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class IfsxApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("MainWindow.fxml"));
        Parent root = loader.load();

        stage.setTitle("IFSX -- IFS Extract/Repack Tool");
        var icon = getClass().getResourceAsStream("ifsx.png");
        if (icon != null) stage.getIcons().add(new Image(icon));
        stage.setScene(new Scene(root, 1000, 650));
        stage.show();

        HookRunner.ensureDirectories();
        SdpToolExecutor.checkSdpTools().ifPresent(msg -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("SDP Tools Not Found");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.show();
        }));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
