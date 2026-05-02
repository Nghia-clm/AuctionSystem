package com.auction.controller;

import com.auction.controller.LoginController.Session;
import com.auction.network.ServerConnection;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
/* import java.util.ArrayList;
import java.util.List; */
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * AuctionListController - Hiển thị danh sách phiên đấu giá.
 *
 * Chức năng:
 *   - Load và hiển thị tất cả phiên đấu giá từ server (GET_AUCTIONS)
 *   - Lọc theo trạng thái (tất cả / RUNNING / OPEN / FINISHED)
 *   - Double-click hoặc nút "Tham gia" → mở BidController
 *   - Nút "Quản lý sản phẩm" (chỉ hiện với SELLER/ADMIN) → SellerController
 *   - Nút "Đăng xuất"
 *   - Tự động refresh mỗi 15 giây
 */
public class AuctionListController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(AuctionListController.class.getName());
    private static final int AUTO_REFRESH_SECONDS = 15;

    // ── FXML fields ────────────────────────────────────────────────────────
    @FXML private Label         welcomeLabel;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Button        refreshButton;
    @FXML private Button        sellerPanelButton;
    @FXML private Button        logoutButton;
    @FXML private Label         statusLabel;

    @FXML private TableView<AuctionRow>         auctionTable;
    @FXML private TableColumn<AuctionRow, String> colId;
    @FXML private TableColumn<AuctionRow, String> colItemName;
    @FXML private TableColumn<AuctionRow, String> colCurrentPrice;
    @FXML private TableColumn<AuctionRow, String> colStatus;
    @FXML private TableColumn<AuctionRow, String> colEndTime;
    @FXML private TableColumn<AuctionRow, String> colSeller;

    @FXML private Button joinButton;

    private final ObservableList<AuctionRow> auctionRows = FXCollections.observableArrayList();
    private javafx.animation.Timeline autoRefreshTimeline;

    // ── Initializable ──────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Session session = Session.getInstance();

        // Hiển thị tên user
        welcomeLabel.setText("Xin chào, " + session.getUsername()
                + " [" + session.getRole() + "]");

        // Ẩn nút Seller nếu không phải SELLER/ADMIN
        sellerPanelButton.setVisible(session.isSeller() || session.isAdmin());

        // Setup bộ lọc
        statusFilterCombo.getItems().addAll("Tất cả", "RUNNING", "OPEN", "FINISHED");
        statusFilterCombo.setValue("Tất cả");
        statusFilterCombo.setOnAction(e -> applyFilter());

        // Setup table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerId"));

        auctionTable.setItems(auctionRows);

        // Double click để tham gia
        auctionTable.setRowFactory(tv -> {
            TableRow<AuctionRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openBidScreen(row.getItem());
                }
            });
            return row;
        });

        // Nút tham gia chỉ active khi chọn 1 hàng
        joinButton.setDisable(true);
        auctionTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> joinButton.setDisable(selected == null)
        );

        // Lần đầu load
        loadAuctions();

        // Auto refresh
        startAutoRefresh();
    }

    // ── Load dữ liệu ──────────────────────────────────────────────────────

    private void loadAuctions() {
        setStatus("Đang tải danh sách...");
        refreshButton.setDisable(true);

        new Thread(() -> {
            JSONObject response = ServerConnection.getInstance().sendRequest("GET_AUCTIONS", null);

            Platform.runLater(() -> {
                refreshButton.setDisable(false);
                if (ServerConnection.isOk(response)) {
                    JSONObject data = response.optJSONObject("data");
                    if (data != null) {
                        JSONArray arr = data.optJSONArray("auctions");
                        parseAndDisplay(arr);
                        setStatus("Tổng " + auctionRows.size() + " phiên đấu giá.");
                    }
                } else {
                    setStatus("Lỗi: " + response.optString("message"));
                }
            });
        }, "load-auctions").start();
    }

    private void parseAndDisplay(JSONArray arr) {
        auctionRows.clear();
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject a = arr.optJSONObject(i);
            if (a == null) continue;
            auctionRows.add(new AuctionRow(
                a.optString("auctionId"),
                a.optString("itemName"),
                String.format("%.0f đ", a.optDouble("currentPrice")),
                a.optString("status"),
                formatEndTime(a.optString("endTime")),
                a.optString("sellerId")
            ));
        }
        applyFilter();
    }

    private void applyFilter() {
        String filter = statusFilterCombo.getValue();
        if ("Tất cả".equals(filter)) {
            auctionTable.setItems(auctionRows);
            return;
        }
        ObservableList<AuctionRow> filtered = FXCollections.observableArrayList();
        for (AuctionRow row : auctionRows) {
            if (filter.equals(row.getStatus())) filtered.add(row);
        }
        auctionTable.setItems(filtered);
    }

    // ── Auto refresh ───────────────────────────────────────────────────────

    private void startAutoRefresh() {
        autoRefreshTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(AUTO_REFRESH_SECONDS),
                e -> loadAuctions()
            )
        );
        autoRefreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    // ── Button handlers ────────────────────────────────────────────────────

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadAuctions();
    }

    @FXML
    private void handleJoin(ActionEvent event) {
        AuctionRow selected = auctionTable.getSelectionModel().getSelectedItem();
        if (selected != null) openBidScreen(selected);
    }

    @FXML
    private void handleSellerPanel(ActionEvent event) {
        try {
            if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/auction/view/seller_panel.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) sellerPanelButton.getScene().getWindow();
            stage.setTitle("Auction System - Quản lý sản phẩm");
            stage.setScene(new Scene(root, 900, 600));
        } catch (IOException e) {
            LOGGER.severe("Không thể tải giao diện quản lý sản phẩm: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        new Thread(() -> {
            ServerConnection.getInstance().sendRequest("LOGOUT", null);
        }).start();

        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        Session.getInstance().logout();

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/auction/view/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setTitle("Auction System - Đăng nhập");
            stage.setScene(new Scene(root, 500, 400));
        } catch (IOException e) {
            LOGGER.severe("Không thể tải giao diện đăng nhập: " + e.getMessage());
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private void openBidScreen(AuctionRow auctionRow) {
        try {
            if (autoRefreshTimeline != null) autoRefreshTimeline.stop();

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/auction/view/bid_screen.fxml"));
            Parent root = loader.load();

            // Truyền auctionId sang BidController
            BidController bidCtrl = loader.getController();
            bidCtrl.setAuctionId(auctionRow.getAuctionId());

            Stage stage = (Stage) auctionTable.getScene().getWindow();
            stage.setTitle("Đấu giá - " + auctionRow.getItemName());
            stage.setScene(new Scene(root, 900, 650));
        } catch (IOException e) {
            LOGGER.severe("Không thể tải giao diện đấu giá: " + e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private String formatEndTime(String raw) {
        if (raw == null || raw.isEmpty()) return "-";
        // raw là LocalDateTime.toString() dạng "2026-05-01T20:00:00"
        return raw.replace("T", " ").substring(0, Math.min(raw.length(), 16));
    }

    // ── Inner class: AuctionRow (model cho TableView) ─────────────────────

    public static class AuctionRow {
        private final SimpleStringProperty auctionId;
        private final SimpleStringProperty itemName;
        private final SimpleStringProperty currentPrice;
        private final SimpleStringProperty status;
        private final SimpleStringProperty endTime;
        private final SimpleStringProperty sellerId;

        public AuctionRow(String auctionId, String itemName, String currentPrice,
                          String status, String endTime, String sellerId) {
            this.auctionId    = new SimpleStringProperty(auctionId);
            this.itemName     = new SimpleStringProperty(itemName);
            this.currentPrice = new SimpleStringProperty(currentPrice);
            this.status       = new SimpleStringProperty(status);
            this.endTime      = new SimpleStringProperty(endTime);
            this.sellerId     = new SimpleStringProperty(sellerId);
        }

        public String getAuctionId()    { return auctionId.get(); }
        public String getItemName()     { return itemName.get(); }
        public String getCurrentPrice() { return currentPrice.get(); }
        public String getStatus()       { return status.get(); }
        public String getEndTime()      { return endTime.get(); }
        public String getSellerId()     { return sellerId.get(); }
    }
}
