package com.campus.lostfound.client.controller;

import com.campus.lostfound.client.service.ApiClient;
import com.campus.lostfound.client.service.ApiException;
import com.campus.lostfound.client.util.Navigator;
import com.campus.lostfound.client.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtIdentifier;
    @FXML private PasswordField txtPassword;
    @FXML private CheckBox chkRememberMe;
    @FXML private Button btnLogin;
    @FXML private Label lblError;

    @FXML
    public void initialize() {
        String remembered = SessionManager.getInstance().getRememberedIdentifier();
        if (remembered != null && !remembered.isBlank()) {
            txtIdentifier.setText(remembered);
            chkRememberMe.setSelected(true);
            Platform.runLater(() -> txtPassword.requestFocus());
        }
    }

    @FXML
    private void handleLogin() {
        String identifier = txtIdentifier.getText() != null ? txtIdentifier.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText() : "";

        if (identifier.isEmpty() || password.isEmpty()) {
            showError("Please enter both email/roll number and password");
            return;
        }

        btnLogin.setDisable(true);
        btnLogin.setText("LOGGING IN...");
        lblError.setVisible(false);
        lblError.setManaged(false);

        new Thread(() -> {
            try {
                ApiClient.getInstance().login(identifier, password);

                if (chkRememberMe.isSelected()) {
                    SessionManager.getInstance().saveRememberedIdentifier(identifier);
                } else {
                    SessionManager.getInstance().saveRememberedIdentifier("");
                }

                Platform.runLater(() -> {
                    Navigator.getInstance().showMainShell();
                });
            } catch (ApiException e) {
                Platform.runLater(() -> {
                    btnLogin.setDisable(false);
                    btnLogin.setText("LOGIN");
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnLogin.setDisable(false);
                    btnLogin.setText("LOGIN");
                    showError("Connection failed: " + e.getMessage() + "\n(Ensure backend server is running on port 8080)");
                });
            }
        }).start();
    }

    @FXML
    private void handleQuickAdmin() {
        txtIdentifier.setText("admin@campus.edu");
        txtPassword.setText("Admin@123");
        handleLogin();
    }

    @FXML
    private void handleQuickStaff() {
        txtIdentifier.setText("staff@campus.edu");
        txtPassword.setText("Staff@123");
        handleLogin();
    }

    @FXML
    private void handleQuickStudent() {
        txtIdentifier.setText("student@campus.edu");
        txtPassword.setText("Student@123");
        handleLogin();
    }

    @FXML
    private void handleForgotPassword() {
        Navigator.showInfo("Forgot Password", "For security and authentication integrity, password resets must be requested from the campus IT desk or system administrator (admin@campus.edu).");
    }

    @FXML
    private void handleOpenRegister() {
        Stage regDialog = new Stage();
        regDialog.initOwner(Navigator.getInstance().getPrimaryStage());
        regDialog.initModality(Modality.APPLICATION_MODAL);
        regDialog.setTitle("New Student Registration");
        regDialog.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #FFFFFF;");

        Label title = new Label("Student Registration");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00274F;");

        Label subtitle = new Label("Create an account to report and track lost items");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #666666;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);

        TextField txtName = new TextField();
        txtName.setPromptText("Full Name");

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("student@campus.edu");

        TextField txtRoll = new TextField();
        txtRoll.setPromptText("e.g. 21CS045 (optional)");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Min 8 characters");

        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("College Email:"), 0, 1);
        grid.add(txtEmail, 1, 1);
        grid.add(new Label("Roll Number:"), 0, 2);
        grid.add(txtRoll, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(txtPass, 1, 3);

        Label regError = new Label();
        regError.setStyle("-fx-text-fill: #D93025; -fx-font-size: 12px;");
        regError.setVisible(false);
        regError.setManaged(false);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(e -> regDialog.close());

        Button btnSubmit = new Button("Register");
        btnSubmit.setStyle("-fx-background-color: #00274F; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSubmit.setOnAction(e -> {
            String name = txtName.getText() != null ? txtName.getText().trim() : "";
            String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
            String roll = txtRoll.getText() != null ? txtRoll.getText().trim() : "";
            String pass = txtPass.getText() != null ? txtPass.getText() : "";

            if (name.isEmpty() || email.isEmpty() || pass.length() < 8) {
                regError.setText("Please fill all required fields (Password min 8 chars).");
                regError.setVisible(true);
                regError.setManaged(true);
                return;
            }

            btnSubmit.setDisable(true);
            new Thread(() -> {
                try {
                    ApiClient.getInstance().register(name, email, roll, pass);
                    Platform.runLater(() -> {
                        regDialog.close();
                        Navigator.showInfo("Registration Successful", "Account created successfully! You can now log in.");
                        txtIdentifier.setText(email);
                        txtPassword.setText(pass);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        regError.setText("Registration error: " + ex.getMessage());
                        regError.setVisible(true);
                        regError.setManaged(true);
                    });
                }
            }).start();
        });

        actions.getChildren().addAll(btnCancel, btnSubmit);
        root.getChildren().addAll(title, subtitle, grid, regError, actions);

        regDialog.setScene(new Scene(root, 420, 360));
        regDialog.showAndWait();
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}
