package com.campus.lostfound.client.controller;

import com.campus.lostfound.client.model.Role;
import com.campus.lostfound.client.model.UserDto;
import com.campus.lostfound.client.service.ApiClient;
import com.campus.lostfound.client.util.Navigator;
import com.campus.lostfound.client.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainShellController {

    @FXML private BorderPane rootBorderPane;
    @FXML private VBox navContainer;
    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavSearch;
    @FXML private Button btnNavReportLost;
    @FXML private Button btnNavMyReports;
    @FXML private Button btnNavMyClaims;
    @FXML private Button btnNavMatches;
    @FXML private Button btnNavLogFound;
    @FXML private Button btnNavInventory;
    @FXML private Button btnNavClaimsReview;
    @FXML private Button btnNavUsers;
    @FXML private Button btnNavMerge;
    @FXML private Button btnNavAudit;
    @FXML private Button btnNavNotifications;
    @FXML private Button btnLogout;

    @FXML private Label lblUnreadBadge;
    @FXML private Label lblViewTitle;
    @FXML private Label lblViewSubtitle;
    @FXML private Label lblCurrentDate;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    @FXML private ScrollPane contentScroll;
    @FXML private StackPane contentPane;

    private Timeline notificationPoller;
    private ViewBuilder viewBuilder;

    @FXML
    public void initialize() {
        this.viewBuilder = new ViewBuilder(this);

        UserDto user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            lblUserName.setText(user.getName());
            lblUserRole.setText(user.getRole().name());
            applyRoleVisibility(user.getRole());
        }

        lblCurrentDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")));

        startNotificationPoller();
        showDashboard();
    }

    private void applyRoleVisibility(Role role) {
        boolean isStudent = (role == Role.STUDENT);
        boolean isStaff = (role == Role.STAFF);
        boolean isAdmin = (role == Role.ADMIN);

        // Student actions
        setVisible(btnNavReportLost, isStudent);
        setVisible(btnNavMyReports, isStudent);
        setVisible(btnNavMyClaims, isStudent);

        // Staff / Admin actions
        setVisible(btnNavLogFound, isStaff || isAdmin);
        setVisible(btnNavInventory, isStaff || isAdmin);
        setVisible(btnNavClaimsReview, isStaff || isAdmin);

        // Admin actions
        setVisible(btnNavUsers, isAdmin);
        setVisible(btnNavMerge, isAdmin);
        setVisible(btnNavAudit, isAdmin);
    }

    private void setVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void setActiveNav(Button activeBtn, String title, String subtitle) {
        List<Button> allBtns = List.of(
                btnNavDashboard, btnNavSearch, btnNavReportLost, btnNavMyReports, btnNavMyClaims,
                btnNavMatches, btnNavLogFound, btnNavInventory, btnNavClaimsReview,
                btnNavUsers, btnNavMerge, btnNavAudit, btnNavNotifications
        );

        for (Button btn : allBtns) {
            if (btn == activeBtn) {
                btn.getStyleClass().remove("nav-btn");
                if (!btn.getStyleClass().contains("nav-btn-active")) {
                    btn.getStyleClass().add("nav-btn-active");
                }
            } else {
                btn.getStyleClass().remove("nav-btn-active");
                if (!btn.getStyleClass().contains("nav-btn")) {
                    btn.getStyleClass().add("nav-btn");
                }
            }
        }

        lblViewTitle.setText(title);
        lblViewSubtitle.setText(subtitle);
    }

    public void setContent(Node node) {
        contentPane.getChildren().clear();
        contentPane.getChildren().add(node);
    }

    private void startNotificationPoller() {
        pollNotifications();
        notificationPoller = new Timeline(new KeyFrame(Duration.seconds(30), e -> pollNotifications()));
        notificationPoller.setCycleCount(Timeline.INDEFINITE);
        notificationPoller.play();
    }

    public void pollNotifications() {
        new Thread(() -> {
            long count = ApiClient.getInstance().getUnreadNotificationCount();
            Platform.runLater(() -> {
                if (count > 0) {
                    lblUnreadBadge.setText(String.valueOf(count));
                    lblUnreadBadge.setVisible(true);
                    lblUnreadBadge.setManaged(true);
                } else {
                    lblUnreadBadge.setVisible(false);
                    lblUnreadBadge.setManaged(false);
                }
            });
        }).start();
    }

    // ===============================================================
    // NAVIGATION HANDLERS
    // ===============================================================

    @FXML public void showDashboard() {
        setActiveNav(btnNavDashboard, "Dashboard", "Real-Time Campus Recovery & System Overview");
        viewBuilder.buildDashboard();
    }

    @FXML public void showSearch() {
        setActiveNav(btnNavSearch, "Search & Browse", "Explore Registered Lost & Found Items Across Campus");
        viewBuilder.buildSearch();
    }

    @FXML public void showReportLost() {
        setActiveNav(btnNavReportLost, "Report Lost Item", "File a Lost Item Notice with AI/Rule Matching");
        viewBuilder.buildReportLost();
    }

    @FXML public void showMyReports() {
        setActiveNav(btnNavMyReports, "My Reports", "Manage and Track Your Submitted Lost Reports");
        viewBuilder.buildMyReports();
    }

    @FXML public void showMyClaims() {
        setActiveNav(btnNavMyClaims, "My Claims", "Track Verification and Return Handover Status");
        viewBuilder.buildMyClaims();
    }

    @FXML public void showMatches() {
        setActiveNav(btnNavMatches, "Smart Matches", "High-Confidence Potential Matches Generated Automatically");
        viewBuilder.buildMatches();
    }

    @FXML public void showLogFound() {
        setActiveNav(btnNavLogFound, "Log Found Item", "Register Found Property Received by Campus Staff");
        viewBuilder.buildLogFound();
    }

    @FXML public void showInventory() {
        setActiveNav(btnNavInventory, "Warehouse Inventory", "Currently Held Items, Retention Tracking & Disposal");
        viewBuilder.buildInventory();
    }

    @FXML public void showClaimsReview() {
        setActiveNav(btnNavClaimsReview, "Claims Review Desk", "Verify Ownership Proofs and Authorize Returns");
        viewBuilder.buildClaimsReview();
    }

    @FXML public void showUsers() {
        setActiveNav(btnNavUsers, "User Accounts", "Manage College Staff, Students, and Roles");
        viewBuilder.buildUsers();
    }

    @FXML public void showMerge() {
        setActiveNav(btnNavMerge, "Manual Matching & Merge", "Consolidate Duplicates and Force Match Items");
        viewBuilder.buildMerge();
    }

    @FXML public void showAudit() {
        setActiveNav(btnNavAudit, "Audit Log", "Immutable Security & Operational Activity Trail");
        viewBuilder.buildAudit();
    }

    @FXML public void showNotifications() {
        setActiveNav(btnNavNotifications, "Notifications", "System Alerts, Match Discoveries & Handover Notices");
        viewBuilder.buildNotifications();
    }

    @FXML public void handleLogout() {
        if (Navigator.showConfirm("Sign Out", "Are you sure you want to sign out of the portal?")) {
            if (notificationPoller != null) {
                notificationPoller.stop();
            }
            SessionManager.getInstance().clearSession();
            Navigator.getInstance().showLogin();
        }
    }
}
