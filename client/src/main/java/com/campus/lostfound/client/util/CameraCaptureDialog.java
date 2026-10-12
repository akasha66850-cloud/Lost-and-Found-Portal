package com.campus.lostfound.client.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CameraCaptureDialog {

    public static class PhotoResult {
        private final byte[] data;
        private final String filename;
        private final String contentType;
        private final Image image;

        public PhotoResult(byte[] data, String filename, String contentType, Image image) {
            this.data = data;
            this.filename = filename;
            this.contentType = contentType;
            this.image = image;
        }

        public byte[] getData() { return data; }
        public String getFilename() { return filename; }
        public String getContentType() { return contentType; }
        public Image getImage() { return image; }
    }

    public static PhotoResult show(Stage ownerStage, String title) {
        Stage dialog = new Stage();
        dialog.initOwner(ownerStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(title != null ? title : "Camera Snapshot & Photo Upload");
        dialog.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #161824; -fx-font-family: 'Segoe UI', sans-serif;");

        Label heading = new Label("Item Camera Capture & Photo Selector");
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        heading.setTextFill(Color.WHITE);

        // Viewfinder pane
        StackPane viewfinder = new StackPane();
        viewfinder.setPrefSize(420, 260);
        viewfinder.setMaxSize(420, 260);
        viewfinder.setStyle("-fx-background-color: #0F1017; -fx-border-color: #383B52; -fx-border-radius: 8; -fx-background-radius: 8;");

        // Preview image view
        ImageView previewView = new ImageView();
        previewView.setFitWidth(400);
        previewView.setFitHeight(240);
        previewView.setPreserveRatio(true);

        // Crosshairs / camera HUD overlay
        VBox hudOverlay = new VBox(10);
        hudOverlay.setAlignment(Pos.CENTER);
        hudOverlay.setMouseTransparent(true);

        Circle lensCircle = new Circle(36);
        lensCircle.setFill(Color.TRANSPARENT);
        lensCircle.setStroke(Color.web("#CE8DF9"));
        lensCircle.setStrokeWidth(2);

        Label timestampLabel = new Label("CAM-01 • " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        timestampLabel.setTextFill(Color.web("#8E92BC"));
        timestampLabel.setFont(Font.font("Monospaced", 11));

        hudOverlay.getChildren().addAll(lensCircle, timestampLabel);
        viewfinder.getChildren().addAll(previewView, hudOverlay);

        final PhotoResult[] selectedResult = new PhotoResult[1];

        // Action buttons
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER);

        Button btnSnapshot = new Button("📸 Snap Camera Photo");
        btnSnapshot.setStyle("-fx-background-color: #CE8DF9; -fx-text-fill: #161824; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");

        Button btnChooseFile = new Button("📁 Choose File from Disk");
        btnChooseFile.setStyle("-fx-background-color: #26293D; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-border-color: #4A4E69; -fx-border-radius: 6;");

        Button btnConfirm = new Button("✓ Use This Photo");
        btnConfirm.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8 16;");
        btnConfirm.setDisable(true);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #8E92BC; -fx-padding: 8 16;");

        btnSnapshot.setOnAction(e -> {
            // Render simulated live camera capture snapshot with date, campus seal, grid
            WritableImage snap = viewfinder.snapshot(null, null);
            BufferedImage bImage = SwingFXUtils.fromFXImage(snap, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ImageIO.write(bImage, "png", baos);
                byte[] bytes = baos.toByteArray();
                Image fxImg = new Image(new ByteArrayInputStream(bytes));
                previewView.setImage(fxImg);
                hudOverlay.setVisible(false);
                selectedResult[0] = new PhotoResult(bytes, "cam_snap_" + System.currentTimeMillis() + ".png", "image/png", fxImg);
                btnConfirm.setDisable(false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        btnChooseFile.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Item Image");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png")
            );
            File file = chooser.showOpenDialog(dialog);
            if (file != null) {
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    Image img = new Image(new ByteArrayInputStream(bytes));
                    previewView.setImage(img);
                    hudOverlay.setVisible(false);
                    String ct = file.getName().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                    selectedResult[0] = new PhotoResult(bytes, file.getName(), ct, img);
                    btnConfirm.setDisable(false);
                } catch (IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to read image file: " + ex.getMessage());
                    alert.showAndWait();
                }
            }
        });

        btnConfirm.setOnAction(e -> dialog.close());
        btnCancel.setOnAction(e -> {
            selectedResult[0] = null;
            dialog.close();
        });

        buttonBar.getChildren().addAll(btnSnapshot, btnChooseFile, btnConfirm, btnCancel);
        root.getChildren().addAll(heading, viewfinder, buttonBar);

        Scene scene = new Scene(root, 480, 380);
        dialog.setScene(scene);
        dialog.showAndWait();

        return selectedResult[0];
    }
}
