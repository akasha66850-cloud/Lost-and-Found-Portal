package com.campus.lostfound.client.controller;

import com.campus.lostfound.client.model.*;
import com.campus.lostfound.client.service.ApiClient;
import com.campus.lostfound.client.service.ApiException;
import com.campus.lostfound.client.util.CameraCaptureDialog;
import com.campus.lostfound.client.util.Navigator;
import com.campus.lostfound.client.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.*;

public class ViewBuilder {

    private final MainShellController shell;

    public ViewBuilder(MainShellController shell) {
        this.shell = shell;
    }

    // ===============================================================
    // 1. DASHBOARD VIEW (Replicating User's Reference Screenshot)
    // ===============================================================

    public void buildDashboard() {
        VBox root = new VBox(24);
        root.setFillWidth(true);

        Label loading = new Label("Loading dashboard metrics...");
        loading.setStyle("-fx-text-fill: #8E92BC; -fx-font-size: 14px;");
        shell.setContent(loading);

        new Thread(() -> {
            try {
                PageResponse<ItemDto> recentItemsPage = ApiClient.getInstance().searchItems(null, null, null, null, null, null, null, 0, 8);
                AdminStatsDto stats = null;
                if (SessionManager.getInstance().isStaffOrAdmin()) {
                    try { stats = ApiClient.getInstance().getAdminStats(); } catch (Exception ignored) {}
                }

                final AdminStatsDto finalStats = stats;
                final List<ItemDto> recentItems = recentItemsPage.getContent() != null ? recentItemsPage.getContent() : List.of();

                Platform.runLater(() -> {
                    // Metric Cards Row
                    HBox cardsRow = new HBox(20);
                    cardsRow.setFillHeight(true);

                    long totalLost = finalStats != null ? finalStats.getTotalLost() : recentItems.stream().filter(i -> i.getType() == ItemType.LOST).count();
                    long totalFound = finalStats != null ? finalStats.getTotalFound() : recentItems.stream().filter(i -> i.getType() == ItemType.FOUND).count();
                    long returned = finalStats != null ? finalStats.getTotalReturned() : recentItems.stream().filter(i -> i.getStatus() == ItemStatus.RETURNED).count();
                    double recoveryRate = finalStats != null ? finalStats.getRecoveryRate() : (totalFound > 0 ? (returned * 100.0 / totalFound) : 0.0);

                    // Card 1: Total Reports (Blue Gradient)
                    VBox card1 = createMetricCard("card-metric-blue", "Total Tracked Items", String.valueOf(totalLost + totalFound),
                            String.format("Lost: %d  •  Found: %d", totalLost, totalFound));
                    HBox.setHgrow(card1, Priority.ALWAYS);

                    // Card 2: Successful Handover (Green Gradient)
                    VBox card2 = createMetricCard("card-metric-green", "Recovered & Handed Over", String.valueOf(returned),
                            String.format("%.1f%% Recovery Success Rate", recoveryRate));
                    HBox.setHgrow(card2, Priority.ALWAYS);

                    // Card 3: Pending Claims (Red Gradient)
                    long pendingClaims = recentItems.stream().filter(i -> i.getStatus() == ItemStatus.CLAIM_PENDING).count();
                    VBox card3 = createMetricCard("card-metric-red", "Active Claims Pending", String.valueOf(pendingClaims),
                            "Items awaiting owner verification");
                    HBox.setHgrow(card3, Priority.ALWAYS);

                    cardsRow.getChildren().addAll(card1, card2, card3);

                    // Bottom Grid: Left Table & Right Progress Overview
                    HBox bottomRow = new HBox(20);
                    bottomRow.setFillHeight(true);

                    // Left Panel: Recent Transactions / Activity Table
                    VBox leftPanel = new VBox(16);
                    leftPanel.getStyleClass().add("dark-panel");
                    HBox.setHgrow(leftPanel, Priority.ALWAYS);

                    HBox leftHeader = new HBox();
                    Label leftTitle = new Label("Recent Campus Activity");
                    leftTitle.getStyleClass().add("panel-header-title");
                    leftHeader.getChildren().add(leftTitle);

                    TableView<ItemDto> table = new TableView<>();
                    table.setPrefHeight(320);

                    TableColumn<ItemDto, String> colType = new TableColumn<>("Type");
                    colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType() != null ? d.getValue().getType().name() : ""));
                    colType.setPrefWidth(80);

                    TableColumn<ItemDto, String> colTitle = new TableColumn<>("Item Description");
                    colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
                    colTitle.setPrefWidth(220);

                    TableColumn<ItemDto, String> colCat = new TableColumn<>("Category");
                    colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory() != null ? d.getValue().getCategory().name() : ""));
                    colCat.setPrefWidth(100);

                    TableColumn<ItemDto, String> colLoc = new TableColumn<>("Location");
                    colLoc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLocation()));
                    colLoc.setPrefWidth(110);

                    TableColumn<ItemDto, String> colStatus = new TableColumn<>("Status");
                    colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
                    colStatus.setPrefWidth(120);

                    table.getColumns().addAll(colType, colTitle, colCat, colLoc, colStatus);
                    table.getItems().addAll(recentItems);

                    leftPanel.getChildren().addAll(leftHeader, table);

                    // Right Panel: Location & Recovery Overview (Progress bars like reference image)
                    VBox rightPanel = new VBox(16);
                    rightPanel.getStyleClass().add("dark-panel");
                    rightPanel.setPrefWidth(320);
                    rightPanel.setMinWidth(300);

                    Label rightTitle = new Label("Location Density");
                    rightTitle.getStyleClass().add("panel-header-title");
                    rightPanel.getChildren().add(rightTitle);

                    rightPanel.getChildren().add(createProgressBarRow("Library Reading Halls", 0.82, "82%", "#F59E0B"));
                    rightPanel.getChildren().add(createProgressBarRow("Student Canteen", 0.55, "55%", "#10B981"));
                    rightPanel.getChildren().add(createProgressBarRow("Block A / Academic Complex", 0.70, "70%", "#6366F1"));
                    rightPanel.getChildren().add(createProgressBarRow("Sports Grounds & Pavilion", 0.40, "40%", "#EC4899"));
                    rightPanel.getChildren().add(createProgressBarRow("Hostel & Parking Area", 0.25, "25%", "#3B82F6"));

                    bottomRow.getChildren().addAll(leftPanel, rightPanel);

                    root.getChildren().addAll(cardsRow, bottomRow);
                    shell.setContent(root);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Label err = new Label("Unable to load dashboard data: " + ex.getMessage());
                    err.setTextFill(Color.web("#EF4444"));
                    shell.setContent(err);
                });
            }
        }).start();
    }

    private VBox createMetricCard(String styleClass, String title, String value, String subtitle) {
        VBox card = new VBox(10);
        card.getStyleClass().add(styleClass);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("metric-title");

        Label lblValue = new Label(value);
        lblValue.getStyleClass().add("metric-value");

        Label lblSub = new Label(subtitle);
        lblSub.getStyleClass().add("metric-subtitle");

        card.getChildren().addAll(lblTitle, lblValue, lblSub);
        return card;
    }

    private VBox createProgressBarRow(String label, double progress, String percentStr, String hexColor) {
        VBox box = new VBox(6);
        HBox top = new HBox();
        Label lblName = new Label(label);
        lblName.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 13px; -fx-font-weight: 600;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblVal = new Label(percentStr);
        lblVal.setStyle("-fx-text-fill: #8E92BC; -fx-font-size: 12px;");
        top.getChildren().addAll(lblName, spacer, lblVal);

        ProgressBar bar = new ProgressBar(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle(String.format("-fx-accent: %s; -fx-control-inner-background: #202235;", hexColor));

        box.getChildren().addAll(top, bar);
        return box;
    }

    // ===============================================================
    // 2. SEARCH & BROWSE VIEW
    // ===============================================================

    public void buildSearch() {
        VBox root = new VBox(18);
        root.getStyleClass().add("dark-panel");

        Label header = new Label("Browse Campus Items");
        header.getStyleClass().add("panel-header-title");

        // Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TextField txtKeyword = new TextField();
        txtKeyword.setPromptText("Keyword search...");
        txtKeyword.getStyleClass().add("dark-input");
        txtKeyword.setPrefWidth(220);

        ComboBox<Category> cbCategory = new ComboBox<>();
        cbCategory.getItems().add(null);
        cbCategory.getItems().addAll(Category.values());
        cbCategory.setPromptText("All Categories");
        cbCategory.getStyleClass().add("dark-input");

        ComboBox<String> cbLocation = new ComboBox<>();
        cbLocation.getItems().addAll("", "Library", "Canteen", "Block A", "Block B", "Block C", "Hostel", "Sports Ground", "Parking", "Other");
        cbLocation.setPromptText("All Locations");
        cbLocation.getStyleClass().add("dark-input");

        Button btnFilter = new Button("🔍 Search");
        btnFilter.getStyleClass().add("btn-primary");

        filterBar.getChildren().addAll(txtKeyword, cbCategory, cbLocation, btnFilter);

        // Results Table
        TableView<ItemDto> table = new TableView<>();
        table.setPrefHeight(500);

        TableColumn<ItemDto, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType() != null ? d.getValue().getType().name() : ""));
        colType.setPrefWidth(80);

        TableColumn<ItemDto, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colTitle.setPrefWidth(240);

        TableColumn<ItemDto, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory() != null ? d.getValue().getCategory().name() : ""));
        colCat.setPrefWidth(110);

        TableColumn<ItemDto, String> colLoc = new TableColumn<>("Location");
        colLoc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLocation()));
        colLoc.setPrefWidth(120);

        TableColumn<ItemDto, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventDate() != null ? d.getValue().getEventDate().toString() : ""));
        colDate.setPrefWidth(100);

        TableColumn<ItemDto, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
        colStatus.setPrefWidth(120);

        TableColumn<ItemDto, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(140);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnAction = new Button();

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                ItemDto current = getTableRow().getItem();
                btnAction.setText(current.getType() == ItemType.FOUND ? "Claim / Details" : "View Details");
                btnAction.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 4 8;");
                btnAction.setOnAction(e -> showItemDetailsModal(current));
                setGraphic(btnAction);
            }
        });

        table.getColumns().addAll(colType, colTitle, colCat, colLoc, colDate, colStatus, colAction);

        Runnable doSearch = () -> {
            String q = txtKeyword.getText() != null ? txtKeyword.getText().trim() : null;
            Category cat = cbCategory.getValue();
            String loc = cbLocation.getValue() != null && !cbLocation.getValue().isBlank() ? cbLocation.getValue() : null;

            new Thread(() -> {
                try {
                    PageResponse<ItemDto> resp = ApiClient.getInstance().searchItems(q, cat, loc, null, null, null, null, 0, 50);
                    Platform.runLater(() -> {
                        table.getItems().clear();
                        if (resp.getContent() != null) table.getItems().addAll(resp.getContent());
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> Navigator.showError("Search Error", ex.getMessage()));
                }
            }).start();
        };

        btnFilter.setOnAction(e -> doSearch.run());
        doSearch.run(); // initial query

        root.getChildren().addAll(header, filterBar, table);
        shell.setContent(root);
    }

    private void showItemDetailsModal(ItemDto item) {
        Stage dialog = new Stage();
        dialog.initOwner(Navigator.getInstance().getPrimaryStage());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Item Details: " + item.getTitle());

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #171826;");

        Label title = new Label(item.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);

        addDetailRow(grid, 0, "Type:", item.getType() != null ? item.getType().name() : "");
        addDetailRow(grid, 1, "Category:", item.getCategory() != null ? item.getCategory().name() : "");
        addDetailRow(grid, 2, "Location:", item.getLocation());
        addDetailRow(grid, 3, "Date:", item.getEventDate() != null ? item.getEventDate().toString() : "");
        addDetailRow(grid, 4, "Status:", item.getStatus() != null ? item.getStatus().name() : "");
        addDetailRow(grid, 5, "Reported By:", item.getCreatedByName() != null ? item.getCreatedByName() : "");
        if (item.getColor() != null) addDetailRow(grid, 6, "Color:", item.getColor());
        if (item.getBrand() != null) addDetailRow(grid, 7, "Brand:", item.getBrand());
        if (item.getStoredAt() != null) addDetailRow(grid, 8, "Stored At:", item.getStoredAt());

        Label desc = new Label("Description: " + item.getDescription());
        desc.setStyle("-fx-text-fill: #E2E8F0; -fx-wrap-text: true;");

        // Photo ImageView
        ImageView imgView = new ImageView();
        imgView.setFitWidth(280);
        imgView.setFitHeight(180);
        imgView.setPreserveRatio(true);

        new Thread(() -> {
            try {
                byte[] bytes = ApiClient.getInstance().getItemPhoto(item.getId());
                if (bytes != null && bytes.length > 0) {
                    Image fxImg = new Image(new ByteArrayInputStream(bytes));
                    Platform.runLater(() -> imgView.setImage(fxImg));
                }
            } catch (Exception ignored) {}
        }).start();

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnClose = new Button("Close");
        btnClose.getStyleClass().add("btn-secondary");
        btnClose.setOnAction(e -> dialog.close());
        actions.getChildren().add(btnClose);

        // If found item and student is viewing -> Allow Submit Claim
        if (item.getType() == ItemType.FOUND && SessionManager.getInstance().isStudent() && item.getStatus() == ItemStatus.LISTED) {
            Button btnClaim = new Button("Submit Ownership Claim");
            btnClaim.getStyleClass().add("btn-primary");
            btnClaim.setOnAction(e -> {
                dialog.close();
                showClaimSubmissionDialog(item);
            });
            actions.getChildren().add(btnClaim);
        }

        root.getChildren().addAll(title, grid, desc, imgView, actions);
        dialog.setScene(new Scene(root, 480, 520));
        dialog.showAndWait();
    }

    private void addDetailRow(GridPane grid, int row, String label, String val) {
        Label lblL = new Label(label);
        lblL.setStyle("-fx-text-fill: #8E92BC; -fx-font-weight: bold;");
        Label lblV = new Label(val != null ? val : "N/A");
        lblV.setStyle("-fx-text-fill: white;");
        grid.add(lblL, 0, row);
        grid.add(lblV, 1, row);
    }

    // ===============================================================
    // 3. REPORT LOST ITEM VIEW (With Photo & Camera Capture)
    // ===============================================================

    public void buildReportLost() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");
        root.setMaxWidth(720);

        Label header = new Label("Report a Lost Item");
        header.getStyleClass().add("panel-header-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Short title (e.g. Black iPhone 13)");
        txtTitle.getStyleClass().add("dark-input");

        ComboBox<Category> cbCategory = new ComboBox<>();
        cbCategory.getItems().addAll(Category.values());
        cbCategory.setValue(Category.PHONE);
        cbCategory.getStyleClass().add("dark-input");

        TextField txtColor = new TextField();
        txtColor.setPromptText("e.g. Midnight Black");
        txtColor.getStyleClass().add("dark-input");

        TextField txtBrand = new TextField();
        txtBrand.setPromptText("e.g. Apple, Dell, Nike");
        txtBrand.getStyleClass().add("dark-input");

        ComboBox<String> cbLocation = new ComboBox<>();
        cbLocation.getItems().addAll("Library", "Canteen", "Block A", "Block B", "Block C", "Hostel", "Sports Ground", "Parking", "Bus Stop", "Other");
        cbLocation.setValue("Library");
        cbLocation.getStyleClass().add("dark-input");

        DatePicker dpDate = new DatePicker(LocalDate.now());
        dpDate.getStyleClass().add("dark-input");

        TextArea txtDesc = new TextArea();
        txtDesc.setPromptText("Detailed description (identifying marks, stickers, damage)...");
        txtDesc.setPrefRowCount(3);
        txtDesc.getStyleClass().add("dark-input");

        grid.add(createLabel("Item Title:"), 0, 0); grid.add(txtTitle, 1, 0);
        grid.add(createLabel("Category:"), 0, 1); grid.add(cbCategory, 1, 1);
        grid.add(createLabel("Color:"), 0, 2); grid.add(txtColor, 1, 2);
        grid.add(createLabel("Brand:"), 0, 3); grid.add(txtBrand, 1, 3);
        grid.add(createLabel("Location Lost:"), 0, 4); grid.add(cbLocation, 1, 4);
        grid.add(createLabel("Date Lost:"), 0, 5); grid.add(dpDate, 1, 5);
        grid.add(createLabel("Description:"), 0, 6); grid.add(txtDesc, 1, 6);

        // Photo Upload & Camera Snapshot Control
        final CameraCaptureDialog.PhotoResult[] photoHolder = new CameraCaptureDialog.PhotoResult[1];
        HBox photoBox = new HBox(15);
        photoBox.setAlignment(Pos.CENTER_LEFT);

        ImageView previewImg = new ImageView();
        previewImg.setFitWidth(100);
        previewImg.setFitHeight(75);
        previewImg.setStyle("-fx-border-color: #383B52; -fx-background-color: #0F1017;");

        Button btnCapturePhoto = new Button("📸 Camera Snapshot / Select Photo");
        btnCapturePhoto.getStyleClass().add("btn-secondary");
        btnCapturePhoto.setOnAction(e -> {
            CameraCaptureDialog.PhotoResult res = CameraCaptureDialog.show(Navigator.getInstance().getPrimaryStage(), "Capture Lost Item Photo");
            if (res != null) {
                photoHolder[0] = res;
                previewImg.setImage(res.getImage());
            }
        });

        photoBox.getChildren().addAll(btnCapturePhoto, previewImg);
        grid.add(createLabel("Item Photo:"), 0, 7); grid.add(photoBox, 1, 7);

        Button btnSubmit = new Button("Submit Lost Report");
        btnSubmit.getStyleClass().add("btn-primary");
        btnSubmit.setOnAction(e -> {
            String title = txtTitle.getText() != null ? txtTitle.getText().trim() : "";
            String desc = txtDesc.getText() != null ? txtDesc.getText().trim() : "";
            LocalDate date = dpDate.getValue();

            if (title.isEmpty() || desc.length() < 5 || date == null) {
                Navigator.showError("Validation Error", "Please provide a title, date, and description (minimum 5 characters).");
                return;
            }

            btnSubmit.setDisable(true);
            new Thread(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", title);
                    data.put("category", cbCategory.getValue().name());
                    data.put("color", txtColor.getText());
                    data.put("brand", txtBrand.getText());
                    data.put("location", cbLocation.getValue());
                    data.put("eventDate", date.toString());
                    data.put("description", desc);

                    byte[] pBytes = photoHolder[0] != null ? photoHolder[0].getData() : null;
                    String pName = photoHolder[0] != null ? photoHolder[0].getFilename() : null;
                    String pCt = photoHolder[0] != null ? photoHolder[0].getContentType() : null;

                    ApiClient.getInstance().createLostItem(data, pBytes, pName, pCt);
                    Platform.runLater(() -> {
                        Navigator.showInfo("Report Filed", "Your lost item report was submitted! The automated matching engine is actively scanning found reports.");
                        shell.showMyReports();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        Navigator.showError("Submission Error", ex.getMessage());
                    });
                }
            }).start();
        });

        root.getChildren().addAll(header, grid, btnSubmit);
        shell.setContent(root);
    }

    // ===============================================================
    // 4. LOG FOUND ITEM VIEW (Staff & Admin with Camera Capture)
    // ===============================================================

    public void buildLogFound() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");
        root.setMaxWidth(720);

        Label header = new Label("Log a Found Item (Staff Desk)");
        header.getStyleClass().add("panel-header-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        TextField txtTitle = new TextField();
        txtTitle.setPromptText("Short title (e.g. Blue HydroFlask Bottle)");
        txtTitle.getStyleClass().add("dark-input");

        ComboBox<Category> cbCategory = new ComboBox<>();
        cbCategory.getItems().addAll(Category.values());
        cbCategory.setValue(Category.PHONE);
        cbCategory.getStyleClass().add("dark-input");

        TextField txtColor = new TextField();
        txtColor.setPromptText("e.g. Silver");
        txtColor.getStyleClass().add("dark-input");

        TextField txtBrand = new TextField();
        txtBrand.setPromptText("e.g. Apple, Dell, Hydro Flask");
        txtBrand.getStyleClass().add("dark-input");

        ComboBox<String> cbLocation = new ComboBox<>();
        cbLocation.getItems().addAll("Library", "Canteen", "Block A", "Block B", "Block C", "Hostel", "Sports Ground", "Parking", "Bus Stop", "Other");
        cbLocation.setValue("Library");
        cbLocation.getStyleClass().add("dark-input");

        DatePicker dpDate = new DatePicker(LocalDate.now());
        dpDate.getStyleClass().add("dark-input");

        TextField txtStoredAt = new TextField("Security Desk - Block A");
        txtStoredAt.setPromptText("Physical holding location (required)");
        txtStoredAt.getStyleClass().add("dark-input");

        TextField txtHiddenDetail = new TextField();
        txtHiddenDetail.setPromptText("Secret detail (required for Phone, Wallet, Laptop, Keys)");
        txtHiddenDetail.getStyleClass().add("dark-input");

        TextField txtRoll = new TextField();
        txtRoll.setPromptText("Owner Roll Number (only for student ID cards)");
        txtRoll.getStyleClass().add("dark-input");

        TextArea txtDesc = new TextArea();
        txtDesc.setPromptText("Public description...");
        txtDesc.setPrefRowCount(3);
        txtDesc.getStyleClass().add("dark-input");

        grid.add(createLabel("Item Title:"), 0, 0); grid.add(txtTitle, 1, 0);
        grid.add(createLabel("Category:"), 0, 1); grid.add(cbCategory, 1, 1);
        grid.add(createLabel("Color:"), 0, 2); grid.add(txtColor, 1, 2);
        grid.add(createLabel("Brand:"), 0, 3); grid.add(txtBrand, 1, 3);
        grid.add(createLabel("Location Found:"), 0, 4); grid.add(cbLocation, 1, 4);
        grid.add(createLabel("Date Found:"), 0, 5); grid.add(dpDate, 1, 5);
        grid.add(createLabel("Stored At:"), 0, 6); grid.add(txtStoredAt, 1, 6);
        grid.add(createLabel("Hidden Secret:"), 0, 7); grid.add(txtHiddenDetail, 1, 7);
        grid.add(createLabel("ID Roll No:"), 0, 8); grid.add(txtRoll, 1, 8);
        grid.add(createLabel("Description:"), 0, 9); grid.add(txtDesc, 1, 9);

        // Photo Upload & Camera Snapshot Control
        final CameraCaptureDialog.PhotoResult[] photoHolder = new CameraCaptureDialog.PhotoResult[1];
        HBox photoBox = new HBox(15);
        photoBox.setAlignment(Pos.CENTER_LEFT);

        ImageView previewImg = new ImageView();
        previewImg.setFitWidth(100);
        previewImg.setFitHeight(75);
        previewImg.setStyle("-fx-border-color: #383B52; -fx-background-color: #0F1017;");

        Button btnCapturePhoto = new Button("📸 Camera Snapshot / Select Photo");
        btnCapturePhoto.getStyleClass().add("btn-secondary");
        btnCapturePhoto.setOnAction(e -> {
            CameraCaptureDialog.PhotoResult res = CameraCaptureDialog.show(Navigator.getInstance().getPrimaryStage(), "Capture Found Item Photo");
            if (res != null) {
                photoHolder[0] = res;
                previewImg.setImage(res.getImage());
            }
        });

        photoBox.getChildren().addAll(btnCapturePhoto, previewImg);
        grid.add(createLabel("Item Photo:"), 0, 10); grid.add(photoBox, 1, 10);

        Button btnSubmit = new Button("Register Found Item");
        btnSubmit.getStyleClass().add("btn-primary");
        btnSubmit.setOnAction(e -> {
            String title = txtTitle.getText() != null ? txtTitle.getText().trim() : "";
            String desc = txtDesc.getText() != null ? txtDesc.getText().trim() : "";
            String stored = txtStoredAt.getText() != null ? txtStoredAt.getText().trim() : "";
            Category cat = cbCategory.getValue();

            if (title.isEmpty() || desc.length() < 5 || stored.isEmpty()) {
                Navigator.showError("Validation Error", "Please provide title, storedAt, and description (min 5 chars).");
                return;
            }

            Set<Category> highValue = Set.of(Category.PHONE, Category.WALLET, Category.LAPTOP, Category.KEYS);
            if (highValue.contains(cat) && (txtHiddenDetail.getText() == null || txtHiddenDetail.getText().isBlank())) {
                Navigator.showError("Validation Error", "Hidden secret detail is mandatory for high-value categories (" + cat + ").");
                return;
            }

            btnSubmit.setDisable(true);
            new Thread(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("title", title);
                    data.put("category", cat.name());
                    data.put("color", txtColor.getText());
                    data.put("brand", txtBrand.getText());
                    data.put("location", cbLocation.getValue());
                    data.put("eventDate", dpDate.getValue().toString());
                    data.put("description", desc);
                    data.put("storedAt", stored);
                    if (!txtHiddenDetail.getText().isBlank()) data.put("hiddenDetail", txtHiddenDetail.getText().trim());
                    if (!txtRoll.getText().isBlank()) data.put("ownerRollNumber", txtRoll.getText().trim());

                    byte[] pBytes = photoHolder[0] != null ? photoHolder[0].getData() : null;
                    String pName = photoHolder[0] != null ? photoHolder[0].getFilename() : null;
                    String pCt = photoHolder[0] != null ? photoHolder[0].getContentType() : null;

                    ApiClient.getInstance().createFoundItem(data, pBytes, pName, pCt);
                    Platform.runLater(() -> {
                        Navigator.showInfo("Found Item Logged", "Item registered into campus inventory and matching engine ran!");
                        shell.showInventory();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        Navigator.showError("Submission Error", ex.getMessage());
                    });
                }
            }).start();
        });

        root.getChildren().addAll(header, grid, btnSubmit);
        shell.setContent(root);
    }

    // ===============================================================
    // 5. MY REPORTS VIEW
    // ===============================================================

    public void buildMyReports() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");

        Label header = new Label("My Reported Lost Items");
        header.getStyleClass().add("panel-header-title");

        TableView<ItemDto> table = new TableView<>();
        table.setPrefHeight(450);

        TableColumn<ItemDto, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);

        TableColumn<ItemDto, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colTitle.setPrefWidth(240);

        TableColumn<ItemDto, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory() != null ? d.getValue().getCategory().name() : ""));
        colCat.setPrefWidth(100);

        TableColumn<ItemDto, String> colLoc = new TableColumn<>("Location");
        colLoc.setCellValueFactory(new PropertyValueFactory<>("location"));
        colLoc.setPrefWidth(120);

        TableColumn<ItemDto, String> colDate = new TableColumn<>("Event Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventDate() != null ? d.getValue().getEventDate().toString() : ""));
        colDate.setPrefWidth(100);

        TableColumn<ItemDto, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
        colStatus.setPrefWidth(120);

        TableColumn<ItemDto, Void> colAction = new TableColumn<>("Actions");
        colAction.setPrefWidth(180);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnCancel = new Button("Cancel");
            private final Button btnMatches = new Button("Matches");
            private final HBox box = new HBox(6, btnMatches, btnCancel);

            {
                btnCancel.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px;");
                btnMatches.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: white; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                ItemDto current = getTableRow().getItem();
                btnCancel.setDisable(current.getStatus() == ItemStatus.CLOSED || current.getStatus() == ItemStatus.CANCELLED);
                btnCancel.setOnAction(e -> {
                    if (Navigator.showConfirm("Cancel Report", "Are you sure you want to cancel report #" + current.getId() + "?")) {
                        new Thread(() -> {
                            try {
                                ApiClient.getInstance().cancelLostItem(current.getId());
                                Platform.runLater(() -> buildMyReports());
                            } catch (Exception ex) {
                                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                            }
                        }).start();
                    }
                });

                btnMatches.setOnAction(e -> shell.showMatches());
                setGraphic(box);
            }
        });

        table.getColumns().addAll(colId, colTitle, colCat, colLoc, colDate, colStatus, colAction);

        new Thread(() -> {
            try {
                List<ItemDto> items = ApiClient.getInstance().getMyReports();
                Platform.runLater(() -> table.getItems().addAll(items));
            } catch (Exception ex) {
                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
            }
        }).start();

        root.getChildren().addAll(header, table);
        shell.setContent(root);
    }

    // ===============================================================
    // 6. SMART MATCHES VIEW
    // ===============================================================

    public void buildMatches() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");

        Label header = new Label("Smart Automated Matches");
        header.getStyleClass().add("panel-header-title");

        TableView<MatchDto> table = new TableView<>();
        table.setPrefHeight(450);

        TableColumn<MatchDto, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<MatchDto, String> colScore = new TableColumn<>("Confidence");
        colScore.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getScore() + "%"));
        colScore.setPrefWidth(90);

        TableColumn<MatchDto, String> colLost = new TableColumn<>("Lost Item");
        colLost.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLostItem() != null ? d.getValue().getLostItem().getTitle() : ""));
        colLost.setPrefWidth(200);

        TableColumn<MatchDto, String> colFound = new TableColumn<>("Found Candidate");
        colFound.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFoundItem() != null ? d.getValue().getFoundItem().getTitle() : ""));
        colFound.setPrefWidth(200);

        TableColumn<MatchDto, String> colLoc = new TableColumn<>("Location");
        colLoc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFoundItem() != null ? d.getValue().getFoundItem().getLocation() : ""));
        colLoc.setPrefWidth(110);

        TableColumn<MatchDto, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
        colStatus.setPrefWidth(110);

        TableColumn<MatchDto, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(160);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnClaim = new Button("Claim");
            private final Button btnDismiss = new Button("Dismiss");
            private final HBox box = new HBox(6, btnClaim, btnDismiss);

            {
                btnClaim.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 11px;");
                btnDismiss.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                MatchDto current = getTableRow().getItem();
                btnDismiss.setOnAction(e -> {
                    new Thread(() -> {
                        try {
                            ApiClient.getInstance().dismissMatch(current.getId());
                            Platform.runLater(() -> buildMatches());
                        } catch (Exception ex) {
                            Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                        }
                    }).start();
                });

                btnClaim.setOnAction(e -> {
                    if (current.getFoundItem() != null) {
                        showClaimSubmissionDialog(current.getFoundItem());
                    }
                });

                setGraphic(box);
            }
        });

        table.getColumns().addAll(colId, colScore, colLost, colFound, colLoc, colStatus, colAction);

        new Thread(() -> {
            try {
                List<MatchDto> matches = ApiClient.getInstance().getMatches();
                Platform.runLater(() -> table.getItems().addAll(matches));
            } catch (Exception ex) {
                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
            }
        }).start();

        root.getChildren().addAll(header, table);
        shell.setContent(root);
    }

    // ===============================================================
    // 7. MY CLAIMS VIEW
    // ===============================================================

    public void buildMyClaims() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");

        Label header = new Label("My Ownership Claims");
        header.getStyleClass().add("panel-header-title");

        TableView<ClaimDto> table = new TableView<>();
        table.setPrefHeight(450);

        TableColumn<ClaimDto, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<ClaimDto, String> colItem = new TableColumn<>("Claimed Item");
        colItem.setCellValueFactory(new PropertyValueFactory<>("itemTitle"));
        colItem.setPrefWidth(220);

        TableColumn<ClaimDto, String> colAns = new TableColumn<>("My Verification Details");
        colAns.setCellValueFactory(new PropertyValueFactory<>("answer"));
        colAns.setPrefWidth(220);

        TableColumn<ClaimDto, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
        colStatus.setPrefWidth(110);

        TableColumn<ClaimDto, String> colNote = new TableColumn<>("Staff Note");
        colNote.setCellValueFactory(new PropertyValueFactory<>("reviewNote"));
        colNote.setPrefWidth(180);

        table.getColumns().addAll(colId, colItem, colAns, colStatus, colNote);

        new Thread(() -> {
            try {
                List<ClaimDto> claims = ApiClient.getInstance().getMyClaims();
                Platform.runLater(() -> table.getItems().addAll(claims));
            } catch (Exception ex) {
                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
            }
        }).start();

        root.getChildren().addAll(header, table);
        shell.setContent(root);
    }

    // ===============================================================
    // 8. CLAIMS REVIEW DESK (Staff & Admin)
    // ===============================================================

    public void buildClaimsReview() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");

        Label header = new Label("Claims Review & Verification Desk");
        header.getStyleClass().add("panel-header-title");

        TableView<ClaimDto> table = new TableView<>();
        table.setPrefHeight(450);

        TableColumn<ClaimDto, Long> colId = new TableColumn<>("Claim #");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);

        TableColumn<ClaimDto, String> colClaimant = new TableColumn<>("Claimant");
        colClaimant.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getClaimantName() + " (" + d.getValue().getClaimantEmail() + ")"));
        colClaimant.setPrefWidth(180);

        TableColumn<ClaimDto, String> colItem = new TableColumn<>("Item");
        colItem.setCellValueFactory(new PropertyValueFactory<>("itemTitle"));
        colItem.setPrefWidth(160);

        TableColumn<ClaimDto, String> colHidden = new TableColumn<>("Stored Secret");
        colHidden.setCellValueFactory(new PropertyValueFactory<>("hiddenDetail"));
        colHidden.setPrefWidth(140);

        TableColumn<ClaimDto, String> colAns = new TableColumn<>("Claimant Answer");
        colAns.setCellValueFactory(new PropertyValueFactory<>("answer"));
        colAns.setPrefWidth(180);

        TableColumn<ClaimDto, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
        colStatus.setPrefWidth(90);

        TableColumn<ClaimDto, Void> colActions = new TableColumn<>("Review Actions");
        colActions.setPrefWidth(210);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnProof = new Button("📄 Proof");
            private final Button btnApprove = new Button("✓ Approve");
            private final Button btnReject = new Button("✗ Reject");
            private final HBox box = new HBox(5, btnProof, btnApprove, btnReject);

            {
                btnProof.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 11px;");
                btnApprove.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 11px;");
                btnReject.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                ClaimDto current = getTableRow().getItem();
                btnProof.setDisable(!current.isHasProof());
                btnProof.setOnAction(e -> showClaimProofDialog(current.getId()));

                boolean isPending = (current.getStatus() == ClaimStatus.PENDING);
                btnApprove.setDisable(!isPending);
                btnReject.setDisable(!isPending);

                btnApprove.setOnAction(e -> promptClaimReview(current.getId(), "APPROVE"));
                btnReject.setOnAction(e -> promptClaimReview(current.getId(), "REJECT"));

                setGraphic(box);
            }
        });

        table.getColumns().addAll(colId, colClaimant, colItem, colHidden, colAns, colStatus, colActions);

        new Thread(() -> {
            try {
                PageResponse<ClaimDto> claims = ApiClient.getInstance().getClaims(null, 0, 50);
                Platform.runLater(() -> {
                    if (claims.getContent() != null) table.getItems().addAll(claims.getContent());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
            }
        }).start();

        root.getChildren().addAll(header, table);
        shell.setContent(root);
    }

    private void promptClaimReview(Long claimId, String decision) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Review Decision: " + decision);
        dialog.setHeaderText("Enter review note / instructions for the claimant:");
        dialog.setContentText("Note:");
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() || "APPROVE".equals(decision)) {
            String note = result.orElse("");
            if ("REJECT".equals(decision) && note.isBlank()) {
                Navigator.showError("Rejection Reason Required", "You must provide a reason when rejecting a claim.");
                return;
            }

            new Thread(() -> {
                try {
                    ApiClient.getInstance().reviewClaim(claimId, decision, note);
                    Platform.runLater(() -> {
                        Navigator.showInfo("Review Completed", "Claim #" + claimId + " has been marked as " + decision);
                        buildClaimsReview();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                }
            }).start();
        }
    }

    private void showClaimProofDialog(Long claimId) {
        new Thread(() -> {
            try {
                byte[] bytes = ApiClient.getInstance().getClaimProof(claimId);
                Platform.runLater(() -> {
                    if (bytes == null || bytes.length == 0) {
                        Navigator.showInfo("Proof Document", "No proof document file attached to this claim.");
                        return;
                    }
                    Stage dialog = new Stage();
                    dialog.setTitle("Claim Proof Document #" + claimId);
                    VBox root = new VBox(15);
                    root.setPadding(new Insets(15));
                    root.setStyle("-fx-background-color: #171826;");

                    ImageView img = new ImageView(new Image(new ByteArrayInputStream(bytes)));
                    img.setFitWidth(450);
                    img.setFitHeight(350);
                    img.setPreserveRatio(true);

                    Button btnClose = new Button("Close");
                    btnClose.setOnAction(e -> dialog.close());

                    root.getChildren().addAll(img, btnClose);
                    dialog.setScene(new Scene(root, 480, 420));
                    dialog.show();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Navigator.showError("Proof Error", ex.getMessage()));
            }
        }).start();
    }

    // ===============================================================
    // 9. INVENTORY VIEW (Staff & Admin)
    // ===============================================================

    public void buildInventory() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");

        Label header = new Label("Warehouse Held Items & Retention Inventory");
        header.getStyleClass().add("panel-header-title");

        TableView<ItemDto> table = new TableView<>();
        table.setPrefHeight(450);

        TableColumn<ItemDto, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<ItemDto, String> colTitle = new TableColumn<>("Item");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colTitle.setPrefWidth(200);

        TableColumn<ItemDto, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory() != null ? d.getValue().getCategory().name() : ""));
        colCat.setPrefWidth(100);

        TableColumn<ItemDto, String> colStored = new TableColumn<>("Stored Location");
        colStored.setCellValueFactory(new PropertyValueFactory<>("storedAt"));
        colStored.setPrefWidth(150);

        TableColumn<ItemDto, String> colSecret = new TableColumn<>("Hidden Detail");
        colSecret.setCellValueFactory(new PropertyValueFactory<>("hiddenDetail"));
        colSecret.setPrefWidth(160);

        TableColumn<ItemDto, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus() != null ? d.getValue().getStatus().name() : ""));
        colStatus.setPrefWidth(110);

        TableColumn<ItemDto, Void> colActions = new TableColumn<>("Disposal / Donation");
        colActions.setPrefWidth(170);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDonate = new Button("Donate");
            private final Button btnDispose = new Button("Dispose");
            private final HBox box = new HBox(6, btnDonate, btnDispose);

            {
                btnDonate.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-size: 11px;");
                btnDispose.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                ItemDto current = getTableRow().getItem();
                btnDonate.setOnAction(e -> handleDisposal(current.getId(), "DONATED"));
                btnDispose.setOnAction(e -> handleDisposal(current.getId(), "DISPOSED"));
                setGraphic(box);
            }
        });

        table.getColumns().addAll(colId, colTitle, colCat, colStored, colSecret, colStatus, colActions);

        new Thread(() -> {
            try {
                PageResponse<ItemDto> inv = ApiClient.getInstance().getInventory(0, 50);
                Platform.runLater(() -> {
                    if (inv.getContent() != null) table.getItems().addAll(inv.getContent());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
            }
        }).start();

        root.getChildren().addAll(header, table);
        shell.setContent(root);
    }

    private void handleDisposal(Long itemId, String action) {
        if (Navigator.showConfirm("Confirm Action", "Mark item #" + itemId + " as " + action + "?")) {
            new Thread(() -> {
                try {
                    ApiClient.getInstance().disposeItem(itemId, action);
                    Platform.runLater(() -> buildInventory());
                } catch (Exception ex) {
                    Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                }
            }).start();
        }
    }

    // ===============================================================
    // 10. NOTIFICATIONS VIEW
    // ===============================================================

    public void buildNotifications() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");

        HBox top = new HBox();
        Label header = new Label("System Notifications & Discovery Alerts");
        header.getStyleClass().add("panel-header-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnMarkAll = new Button("✓ Mark All as Read");
        btnMarkAll.getStyleClass().add("btn-secondary");
        btnMarkAll.setOnAction(e -> {
            new Thread(() -> {
                try {
                    ApiClient.getInstance().markAllNotificationsRead();
                    Platform.runLater(() -> {
                        buildNotifications();
                        shell.pollNotifications();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                }
            }).start();
        });

        top.getChildren().addAll(header, sp, btnMarkAll);

        TableView<NotificationDto> table = new TableView<>();
        table.setPrefHeight(450);

        TableColumn<NotificationDto, String> colType = new TableColumn<>("Alert Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setPrefWidth(140);

        TableColumn<NotificationDto, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(new PropertyValueFactory<>("message"));
        colMsg.setPrefWidth(420);

        TableColumn<NotificationDto, String> colDate = new TableColumn<>("Received");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString() : ""));
        colDate.setPrefWidth(140);

        TableColumn<NotificationDto, String> colRead = new TableColumn<>("Read");
        colRead.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isRead() ? "Yes" : "NEW"));
        colRead.setPrefWidth(80);

        TableColumn<NotificationDto, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(100);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnRead = new Button("Mark Read");
            {
                btnRead.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: white; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                NotificationDto current = getTableRow().getItem();
                btnRead.setDisable(current.isRead());
                btnRead.setOnAction(e -> {
                    new Thread(() -> {
                        try {
                            ApiClient.getInstance().markNotificationRead(current.getId());
                            Platform.runLater(() -> {
                                buildNotifications();
                                shell.pollNotifications();
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                        }
                    }).start();
                });
                setGraphic(btnRead);
            }
        });

        table.getColumns().addAll(colType, colMsg, colDate, colRead, colAction);

        new Thread(() -> {
            try {
                PageResponse<NotificationDto> notifs = ApiClient.getInstance().getNotifications(0, 50);
                Platform.runLater(() -> {
                    if (notifs.getContent() != null) table.getItems().addAll(notifs.getContent());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
            }
        }).start();

        root.getChildren().addAll(top, table);
        shell.setContent(root);
    }

    // ===============================================================
    // 11. USER MANAGEMENT VIEW (Admin)
    // ===============================================================

    public void buildUsers() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");

        HBox top = new HBox();
        Label header = new Label("Campus User Accounts & Access Control");
        header.getStyleClass().add("panel-header-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnNewUser = new Button("➕ Create Staff/Admin User");
        btnNewUser.getStyleClass().add("btn-primary");
        btnNewUser.setOnAction(e -> showCreateUserModal());

        top.getChildren().addAll(header, sp, btnNewUser);

        TableView<UserDto> table = new TableView<>();
        table.setPrefHeight(450);

        TableColumn<UserDto, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<UserDto, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setPrefWidth(180);

        TableColumn<UserDto, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(200);

        TableColumn<UserDto, String> colRoll = new TableColumn<>("Roll Number");
        colRoll.setCellValueFactory(new PropertyValueFactory<>("rollNumber"));
        colRoll.setPrefWidth(110);

        TableColumn<UserDto, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole() != null ? d.getValue().getRole().name() : ""));
        colRole.setPrefWidth(100);

        TableColumn<UserDto, String> colActive = new TableColumn<>("Active");
        colActive.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive() ? "Active" : "Disabled"));
        colActive.setPrefWidth(80);

        TableColumn<UserDto, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(180);
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnToggle = new Button("Toggle Active");
            private final Button btnRole = new Button("Set Role");
            private final HBox box = new HBox(6, btnToggle, btnRole);

            {
                btnToggle.setStyle("-fx-background-color: #232538; -fx-text-fill: white; -fx-font-size: 11px;");
                btnRole.setStyle("-fx-background-color: #6C5CE7; -fx-text-fill: white; -fx-font-size: 11px;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                UserDto current = getTableRow().getItem();
                btnToggle.setOnAction(e -> {
                    new Thread(() -> {
                        try {
                            ApiClient.getInstance().updateUserActive(current.getId(), !current.isActive());
                            Platform.runLater(() -> buildUsers());
                        } catch (Exception ex) {
                            Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                        }
                    }).start();
                });

                btnRole.setOnAction(e -> {
                    ChoiceDialog<Role> diag = new ChoiceDialog<>(current.getRole(), Role.values());
                    diag.setTitle("Change User Role");
                    diag.setHeaderText("Select new role for " + current.getName());
                    diag.showAndWait().ifPresent(r -> {
                        new Thread(() -> {
                            try {
                                ApiClient.getInstance().updateUserRole(current.getId(), r);
                                Platform.runLater(() -> buildUsers());
                            } catch (Exception ex) {
                                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                            }
                        }).start();
                    });
                });

                setGraphic(box);
            }
        });

        table.getColumns().addAll(colId, colName, colEmail, colRoll, colRole, colActive, colActions);

        new Thread(() -> {
            try {
                List<UserDto> users = ApiClient.getInstance().getAdminUsers();
                Platform.runLater(() -> table.getItems().addAll(users));
            } catch (Exception ex) {
                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
            }
        }).start();

        root.getChildren().addAll(top, table);
        shell.setContent(root);
    }

    private void showCreateUserModal() {
        Stage diag = new Stage();
        diag.setTitle("Create Privileged User");
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #171826;");

        TextField txtName = new TextField(); txtName.setPromptText("Name"); txtName.getStyleClass().add("dark-input");
        TextField txtEmail = new TextField(); txtEmail.setPromptText("Email"); txtEmail.getStyleClass().add("dark-input");
        TextField txtRoll = new TextField(); txtRoll.setPromptText("Roll Number (optional)"); txtRoll.getStyleClass().add("dark-input");
        PasswordField txtPass = new PasswordField(); txtPass.setPromptText("Password (min 8 chars)"); txtPass.getStyleClass().add("dark-input");
        ComboBox<Role> cbRole = new ComboBox<>(); cbRole.getItems().addAll(Role.STAFF, Role.ADMIN); cbRole.setValue(Role.STAFF); cbRole.getStyleClass().add("dark-input");

        Button btnCreate = new Button("Create User");
        btnCreate.getStyleClass().add("btn-primary");
        btnCreate.setOnAction(e -> {
            new Thread(() -> {
                try {
                    ApiClient.getInstance().createAdminUser(txtName.getText().trim(), txtEmail.getText().trim(),
                            txtRoll.getText().trim(), txtPass.getText(), cbRole.getValue());
                    Platform.runLater(() -> {
                        diag.close();
                        buildUsers();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                }
            }).start();
        });

        root.getChildren().addAll(new Label("Name:"), txtName, new Label("Email:"), txtEmail,
                new Label("Roll Number:"), txtRoll, new Label("Password:"), txtPass, new Label("Role:"), cbRole, btnCreate);
        diag.setScene(new Scene(root, 360, 480));
        diag.show();
    }

    // ===============================================================
    // 12. MANUAL MATCHING & MERGE VIEW (Admin)
    // ===============================================================

    public void buildMerge() {
        VBox root = new VBox(20);
        root.getStyleClass().add("dark-panel");

        Label header = new Label("Manual Matching & Item Merging Desk");
        header.getStyleClass().add("panel-header-title");

        // Section 1: Manual Match
        VBox boxMatch = new VBox(10);
        boxMatch.setStyle("-fx-border-color: #232538; -fx-padding: 15; -fx-background-radius: 8;");
        Label lblMTitle = new Label("Force Manual Match (LOST + FOUND)");
        lblMTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        HBox matchInputs = new HBox(12);
        matchInputs.setAlignment(Pos.CENTER_LEFT);
        TextField txtLostId = new TextField(); txtLostId.setPromptText("Lost Item ID"); txtLostId.getStyleClass().add("dark-input");
        TextField txtFoundId = new TextField(); txtFoundId.setPromptText("Found Item ID"); txtFoundId.getStyleClass().add("dark-input");
        Button btnDoMatch = new Button("⚡ Create Manual Match");
        btnDoMatch.getStyleClass().add("btn-primary");
        btnDoMatch.setOnAction(e -> {
            try {
                Long lId = Long.parseLong(txtLostId.getText().trim());
                Long fId = Long.parseLong(txtFoundId.getText().trim());
                new Thread(() -> {
                    try {
                        ApiClient.getInstance().createManualMatch(lId, fId);
                        Platform.runLater(() -> Navigator.showInfo("Success", "Manual match established!"));
                    } catch (Exception ex) {
                        Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                    }
                }).start();
            } catch (NumberFormatException nfe) {
                Navigator.showError("Input Error", "Please provide numeric item IDs.");
            }
        });
        matchInputs.getChildren().addAll(txtLostId, txtFoundId, btnDoMatch);
        boxMatch.getChildren().addAll(lblMTitle, matchInputs);

        // Section 2: Merge Items
        VBox boxMerge = new VBox(10);
        boxMerge.setStyle("-fx-border-color: #232538; -fx-padding: 15; -fx-background-radius: 8;");
        Label lblMergeTitle = new Label("Consolidate Duplicate Items (Keep ID + Remove ID)");
        lblMergeTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        HBox mergeInputs = new HBox(12);
        mergeInputs.setAlignment(Pos.CENTER_LEFT);
        TextField txtKeepId = new TextField(); txtKeepId.setPromptText("Keep Item ID"); txtKeepId.getStyleClass().add("dark-input");
        TextField txtRemoveId = new TextField(); txtRemoveId.setPromptText("Duplicate Remove ID"); txtRemoveId.getStyleClass().add("dark-input");
        Button btnDoMerge = new Button("🔀 Merge Duplicates");
        btnDoMerge.getStyleClass().add("btn-danger");
        btnDoMerge.setOnAction(e -> {
            try {
                Long kId = Long.parseLong(txtKeepId.getText().trim());
                Long rId = Long.parseLong(txtRemoveId.getText().trim());
                if (Navigator.showConfirm("Confirm Merge", "Merge item #" + rId + " into #" + kId + "? Item #" + rId + " will be cancelled.")) {
                    new Thread(() -> {
                        try {
                            ApiClient.getInstance().mergeItems(kId, rId);
                            Platform.runLater(() -> Navigator.showInfo("Success", "Items merged successfully!"));
                        } catch (Exception ex) {
                            Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
                        }
                    }).start();
                }
            } catch (NumberFormatException nfe) {
                Navigator.showError("Input Error", "Please provide numeric item IDs.");
            }
        });
        mergeInputs.getChildren().addAll(txtKeepId, txtRemoveId, btnDoMerge);
        boxMerge.getChildren().addAll(lblMergeTitle, mergeInputs);

        root.getChildren().addAll(header, boxMatch, boxMerge);
        shell.setContent(root);
    }

    // ===============================================================
    // 13. AUDIT LOG VIEW (Admin)
    // ===============================================================

    public void buildAudit() {
        VBox root = new VBox(16);
        root.getStyleClass().add("dark-panel");

        Label header = new Label("Immutable Security & System Audit Trail");
        header.getStyleClass().add("panel-header-title");

        TableView<Map<String, Object>> table = new TableView<>();
        table.setPrefHeight(480);

        TableColumn<Map<String, Object>, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().get("timestamp"))));
        colTime.setPrefWidth(160);

        TableColumn<Map<String, Object>, String> colEntity = new TableColumn<>("Entity");
        colEntity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get("entityType") + " #" + d.getValue().get("entityId")));
        colEntity.setPrefWidth(120);

        TableColumn<Map<String, Object>, String> colAct = new TableColumn<>("Action");
        colAct.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().get("action"))));
        colAct.setPrefWidth(140);

        TableColumn<Map<String, Object>, String> colActor = new TableColumn<>("Actor ID");
        colActor.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().get("actorId"))));
        colActor.setPrefWidth(90);

        TableColumn<Map<String, Object>, String> colDet = new TableColumn<>("Details");
        colDet.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().get("details"))));
        colDet.setPrefWidth(350);

        table.getColumns().addAll(colTime, colEntity, colAct, colActor, colDet);

        new Thread(() -> {
            try {
                PageResponse<Map<String, Object>> logs = ApiClient.getInstance().getAuditLogs(0, 50);
                Platform.runLater(() -> {
                    if (logs.getContent() != null) table.getItems().addAll(logs.getContent());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> Navigator.showError("Error", ex.getMessage()));
            }
        }).start();

        root.getChildren().addAll(header, table);
        shell.setContent(root);
    }

    // ===============================================================
    // CLAIM SUBMISSION DIALOG (With optional camera/proof capture)
    // ===============================================================

    private void showClaimSubmissionDialog(ItemDto foundItem) {
        Stage dialog = new Stage();
        dialog.setTitle("Submit Ownership Claim for: " + foundItem.getTitle());
        dialog.initOwner(Navigator.getInstance().getPrimaryStage());
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #171826;");

        Label heading = new Label("Ownership Verification Claim");
        heading.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label desc = new Label("Provide specific secret details only the true owner would know (e.g. wallpapers, passcode, engravings, contents):");
        desc.setStyle("-fx-text-fill: #8E92BC; -fx-wrap-text: true;");

        TextArea txtAnswer = new TextArea();
        txtAnswer.setPromptText("Enter your secret verification answer here...");
        txtAnswer.setPrefRowCount(4);
        txtAnswer.getStyleClass().add("dark-input");

        // Optional proof attachment
        final CameraCaptureDialog.PhotoResult[] proofHolder = new CameraCaptureDialog.PhotoResult[1];
        HBox proofBox = new HBox(12);
        proofBox.setAlignment(Pos.CENTER_LEFT);

        ImageView previewImg = new ImageView();
        previewImg.setFitWidth(80); previewImg.setFitHeight(60);

        Button btnAttachProof = new Button("📸 Attach Proof Photo / Receipt");
        btnAttachProof.getStyleClass().add("btn-secondary");
        btnAttachProof.setOnAction(e -> {
            CameraCaptureDialog.PhotoResult res = CameraCaptureDialog.show(dialog, "Attach Ownership Proof");
            if (res != null) {
                proofHolder[0] = res;
                previewImg.setImage(res.getImage());
            }
        });
        proofBox.getChildren().addAll(btnAttachProof, previewImg);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSubmit = new Button("Submit Claim");
        btnSubmit.getStyleClass().add("btn-primary");
        btnSubmit.setOnAction(e -> {
            String ans = txtAnswer.getText() != null ? txtAnswer.getText().trim() : "";
            if (ans.isBlank()) {
                Navigator.showError("Required", "Please provide verification details.");
                return;
            }

            btnSubmit.setDisable(true);
            new Thread(() -> {
                try {
                    byte[] pBytes = proofHolder[0] != null ? proofHolder[0].getData() : null;
                    String pName = proofHolder[0] != null ? proofHolder[0].getFilename() : null;
                    String pCt = proofHolder[0] != null ? proofHolder[0].getContentType() : null;

                    ApiClient.getInstance().submitClaim(foundItem.getId(), ans, null, pBytes, pName, pCt);
                    Platform.runLater(() -> {
                        dialog.close();
                        Navigator.showInfo("Claim Submitted", "Your claim was submitted to campus staff for verification!");
                        shell.showMyClaims();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        Navigator.showError("Claim Error", ex.getMessage());
                    });
                }
            }).start();
        });

        actions.getChildren().addAll(btnCancel, btnSubmit);
        root.getChildren().addAll(heading, desc, txtAnswer, proofBox, actions);

        dialog.setScene(new Scene(root, 480, 360));
        dialog.showAndWait();
    }

    private Label createLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #8E92BC; -fx-font-weight: bold; -fx-font-size: 13px;");
        return lbl;
    }
}
