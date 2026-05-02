package com.auction.controller;

import com.auction.chart.BidHistoryChart;
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
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * BidController - Màn hình đấu giá trực tiếp (realtime bidding).
 *
 * Chức năng:
 *   - Hiển thị thông tin phiên đấu giá (item, giá hiện tại, thời gian còn lại)
 *   - Đặt giá (PLACE_BID)
 *   - Nhận cập nhật realtime từ server (broadcast NEW_BID, AUCTION_FINISHED,
 *     AUCTION_EXTENDED) qua ServerConnection.setBroadcastHandler()
 *   - Hiển thị lịch sử bid (GET_BID_HISTORY)
 *   - Đồng hồ đếm ngược thời gian kết thúc phiên
 *   - Nút "Quay lại" danh sách
 */
public class BidController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(BidController.class.getName());

    // ── FXML fields ────────────────────────────────────────────────────────
    // Thông tin phiên
    @FXML private Label auctionIdLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label itemTypeLabel;
    @FXML private Label itemDescLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label statusLabel;
    @FXML private Label countdownLabel;
    @FXML private Label currentLeaderLabel;
    @FXML private Label endTimeLabel;

    // Đặt giá
    @FXML private TextField  bidAmountField;
    @FXML private Button     placeBidButton;
    @FXML private Label      bidResultLabel;

    // Lịch sử bid
    @FXML private TableView<BidRow>         historyTable;
    @FXML private TableColumn<BidRow, String> colBidder;
    @FXML private TableColumn<BidRow, String> colAmount;
    @FXML private TableColumn<BidRow, String> colTime;

    // Navigation
    @FXML private Button backButton;

    // Biểu đồ giá
    @FXML private javafx.scene.layout.StackPane chartContainer;
    private BidHistoryChart bidHistoryChart;

    // ── State ──────────────────────────────────────────────────────────────
    private String auctionId;
    private String endTimeStr;
    private boolean auctionFinished = false;

    private final ObservableList<BidRow> bidHistory = FXCollections.observableArrayList();
    private javafx.animation.Timeline countdownTimeline;

    // ── Initializable ──────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Setup bảng lịch sử
        colBidder.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        historyTable.setItems(bidHistory);

        bidResultLabel.setVisible(false);

        // Chỉ Bidder mới được đặt giá
        boolean canBid = Session.getInstance().isBidder();
        placeBidButton.setDisable(!canBid);
        bidAmountField.setDisable(!canBid);
        if (!canBid) {
            bidResultLabel.setText("Chỉ tài khoản BIDDER mới có thể đặt giá.");
            bidResultLabel.setVisible(true);
        }

        // Khởi tạo biểu đồ giá và nhúng vào chartContainer
        bidHistoryChart = new BidHistoryChart();
        if (chartContainer != null) {
            bidHistoryChart.getChart().setMaxHeight(Double.MAX_VALUE);
            bidHistoryChart.getChart().setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.VBox.setVgrow(bidHistoryChart.getChart(), javafx.scene.layout.Priority.ALWAYS);
            chartContainer.getChildren().add(bidHistoryChart.getChart());
        }

        // Đăng ký nhận broadcast từ server
        ServerConnection.getInstance().setBroadcastHandler(this::handleBroadcast);
    }

    /**
     * Được gọi từ AuctionListController sau khi load FXML.
     * Truyền auctionId và load dữ liệu phiên.
     */
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
        auctionIdLabel.setText("Phiên: " + auctionId);
        joinAuctionRoom();
        loadAuctionDetail();
        loadBidHistory();
    }

    // ── Join auction room ──────────────────────────────────────────────────

    private void joinAuctionRoom() {
        new Thread(() -> {
            JSONObject data = new JSONObject();
            data.put("auctionId", auctionId);
            ServerConnection.getInstance().sendRequest("JOIN_AUCTION", data);
        }, "join-room").start();
    }

    // ── Load dữ liệu ──────────────────────────────────────────────────────

    private void loadAuctionDetail() {
        new Thread(() -> {
            JSONObject data = new JSONObject();
            data.put("auctionId", auctionId);
            JSONObject response = ServerConnection.getInstance().sendRequest("GET_AUCTION", data);

            Platform.runLater(() -> {
                if (ServerConnection.isOk(response)) {
                    JSONObject d = response.optJSONObject("data");
                    if (d != null) updateAuctionUI(d);
                } else {
                    showBidResult("Lỗi tải thông tin phiên đấu giá: " + response.optString("message"), false);
                }
            });
        }, "load-auction").start();
    }

    private void updateAuctionUI(JSONObject d) {
        itemNameLabel.setText(d.optString("itemName", "-"));
        startingPriceLabel.setText(String.format("%.0f đ", d.optDouble("startingPrice")));
        currentPriceLabel.setText(String.format("%.0f đ", d.optDouble("currentPrice")));
        statusLabel.setText(d.optString("status", "-"));

        String status = d.optString("status");
        endTimeStr = d.optString("endTime");
        endTimeLabel.setText(formatDateTime(endTimeStr));

        String winnerId = d.optString("winnerId", "");
        currentLeaderLabel.setText(winnerId.isEmpty() ? "Chưa có" : winnerId);

        // Màu trạng thái
        switch (status) {
            case "RUNNING"  -> statusLabel.setTextFill(Color.GREEN);
            case "FINISHED" -> { statusLabel.setTextFill(Color.RED); finishAuction(d); }
            case "OPEN"     -> statusLabel.setTextFill(Color.ORANGE);
            default         -> statusLabel.setTextFill(Color.GRAY);
        }

        // Bắt đầu đếm ngược nếu RUNNING
        if ("RUNNING".equals(status) || "OPEN".equals(status)) {
            startCountdown(endTimeStr);
        }

        // Gợi ý giá đặt tối thiểu
        if (Session.getInstance().isBidder() && !"FINISHED".equals(status)) {
            double minBid = d.optDouble("currentPrice") + 1;
            bidAmountField.setPromptText("Tối thiểu: " + String.format("%.0f", minBid));
        }
    }

    private void loadBidHistory() {
        new Thread(() -> {
            JSONObject data = new JSONObject();
            data.put("auctionId", auctionId);
            JSONObject response = ServerConnection.getInstance().sendRequest("GET_BID_HISTORY", data);

            Platform.runLater(() -> {
                if (ServerConnection.isOk(response)) {
                    JSONObject d = response.optJSONObject("data");
                    if (d != null) {
                        JSONArray history = d.optJSONArray("history");
                        parseHistory(history);
                    }
                }
            });
        }, "load-history").start();
    }

    private void parseHistory(JSONArray arr) {
        bidHistory.clear();
        if (arr == null) return;
        // Load vào biểu đồ
        java.util.List<BidHistoryChart.BidPoint> points = new java.util.ArrayList<>();
        // Hiển thị mới nhất lên đầu → duyệt ngược
        for (int i = arr.length() - 1; i >= 0; i--) {
            JSONObject tx = arr.optJSONObject(i);
            if (tx == null) continue;
            double amount = tx.optDouble("bidAmount");
            String tsStr  = tx.optString("timestamp");
            bidHistory.add(new BidRow(
                tx.optString("bidderId"),
                String.format("%.0f đ", amount),
                formatDateTime(tsStr)
            ));
            // Thêm vào danh sách cho biểu đồ (parse LocalDateTime)
            try {
                java.time.LocalDateTime ts =
                    java.time.LocalDateTime.parse(tsStr.replace(" ", "T"));
                points.add(new BidHistoryChart.BidPoint(amount, ts));
            } catch (Exception ignored) {}
        }
        // Vẽ toàn bộ lịch sử lên biểu đồ (tự sort theo thời gian)
        bidHistoryChart.loadHistory(points);
    }

    // ── Place Bid ──────────────────────────────────────────────────────────

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String amountStr = bidAmountField.getText().trim();
        if (amountStr.isEmpty()) {
            showBidResult("Vui lòng nhập số tiền muốn đặt.", false);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            showBidResult("Số tiền không hợp lệ, vui lòng chỉ nhập số.", false);
            return;
        }

        placeBidButton.setDisable(true);
        placeBidButton.setText("Đang xử lý...");
        bidResultLabel.setVisible(false);

        new Thread(() -> {
            JSONObject data = new JSONObject();
            data.put("auctionId", auctionId);
            data.put("bidAmount", amount);
            JSONObject response = ServerConnection.getInstance().sendRequest("PLACE_BID", data);

            Platform.runLater(() -> {
                placeBidButton.setDisable(false);
                placeBidButton.setText("Đặt giá");

                if (ServerConnection.isOk(response)) {
                    bidAmountField.clear();
                    showBidResult("✓ Đặt giá thành công: " + String.format("%.0f đ", amount), true);
                    loadBidHistory(); // Refresh lịch sử
                } else {
                    showBidResult("✗ " + response.optString("message", "Đặt giá thất bại."), false);
                }
            });
        }, "place-bid").start();
    }

    // ── Broadcast Handler (realtime) ───────────────────────────────────────

    /**
     * Nhận broadcast từ server (chạy trên JavaFX thread nhờ Platform.runLater
     * đã được xử lý trong ServerConnection).
     */
    private void handleBroadcast(JSONObject json) {
        String event = json.optString("event");
        String broadcastAuctionId = json.optString("auctionId");

        // Chỉ xử lý broadcast của phiên mình đang xem
        if (!auctionId.equals(broadcastAuctionId)) return;

        switch (event) {
            case "NEW_BID" -> {
                double newPrice    = json.optDouble("bidAmount");
                String bidderName  = json.optString("bidderName");
                String timestamp   = json.optString("timestamp");

                // Cập nhật giá realtime
                currentPriceLabel.setText(String.format("%.0f đ", newPrice));
                currentLeaderLabel.setText(bidderName);

                // Cập nhật biểu đồ realtime
                bidHistoryChart.addBidNow(newPrice);

                // Thêm vào đầu bảng lịch sử
                bidHistory.add(0, new BidRow(
                    bidderName,
                    String.format("%.0f đ", newPrice),
                    formatDateTime(timestamp)
                ));

                // Flash thông báo
                showBidResult("🔔 Bid mới: " + bidderName + " đặt "
                        + String.format("%.0f đ", newPrice), true);
            }
            case "AUCTION_FINISHED" -> {
                double finalPrice = json.optDouble("finalPrice");
                String winnerId   = json.optString("winnerId", "Không có");
                finishAuctionByBroadcast(finalPrice, winnerId);
            }
            case "AUCTION_EXTENDED" -> {
                String newEndTime   = json.optString("newEndTime");
                int    extraSeconds = json.optInt("extraSeconds");
                endTimeStr = newEndTime;
                endTimeLabel.setText(formatDateTime(newEndTime));
                startCountdown(newEndTime); // Reset đồng hồ
                showBidResult("⏱ Phiên được gia hạn thêm " + extraSeconds + " giây!", true);
            }
        }
    }

    // ── Countdown Timer ────────────────────────────────────────────────────

    private void startCountdown(String endTimeRaw) {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1),
                e -> updateCountdown(endTimeRaw)
            )
        );
        countdownTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdown(String endTimeRaw) {
        try {
            java.time.LocalDateTime endTime =
                java.time.LocalDateTime.parse(endTimeRaw.replace(" ", "T"));
            java.time.Duration remaining =
                java.time.Duration.between(java.time.LocalDateTime.now(), endTime);

            if (remaining.isNegative()) {
                countdownLabel.setText("Đã kết thúc");
                countdownLabel.setTextFill(Color.RED);
                if (countdownTimeline != null) countdownTimeline.stop();
            } else {
                long hours   = remaining.toHours();
                long minutes = remaining.toMinutesPart();
                long seconds = remaining.toSecondsPart();
                countdownLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                // Đổi màu khi < 60 giây
                countdownLabel.setTextFill(remaining.getSeconds() < 60 ? Color.RED : Color.BLACK);
            }
        } catch (Exception ex) {
            countdownLabel.setText("--:--:--");
        }
    }

    // ── Finish Auction ─────────────────────────────────────────────────────

    private void finishAuction(JSONObject d) {
        if (auctionFinished) return;
        auctionFinished = true;
        if (countdownTimeline != null) countdownTimeline.stop();
        placeBidButton.setDisable(true);
        bidAmountField.setDisable(true);
        countdownLabel.setText("Đã kết thúc");
        countdownLabel.setTextFill(Color.RED);
    }

    private void finishAuctionByBroadcast(double finalPrice, String winnerId) {
        if (auctionFinished) return;
        auctionFinished = true;
        if (countdownTimeline != null) countdownTimeline.stop();
        placeBidButton.setDisable(true);
        bidAmountField.setDisable(true);
        statusLabel.setText("FINISHED");
        statusLabel.setTextFill(Color.RED);
        currentPriceLabel.setText(String.format("%.0f đ", finalPrice));
        currentLeaderLabel.setText(winnerId);
        countdownLabel.setText("Đã kết thúc");
        countdownLabel.setTextFill(Color.RED);
        showBidResult("🏆 Phiên kết thúc! Người thắng: " + winnerId
                + " | Giá cuối: " + String.format("%.0f đ", finalPrice), true);
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    @FXML
    private void handleBack(ActionEvent event) {
        // Rời khỏi phòng đấu giá
        new Thread(() -> {
            ServerConnection.getInstance().sendRequest("LEAVE_AUCTION", null);
        }).start();

        // Xóa broadcast handler và reset chart
        ServerConnection.getInstance().setBroadcastHandler(null);
        bidHistoryChart.reset();
        if (countdownTimeline != null) countdownTimeline.stop();

        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/auction/view/auction_list.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setTitle("Auction System - Danh sách đấu giá");
            stage.setScene(new Scene(root, 900, 600));
        } catch (IOException e) {
            LOGGER.severe("Không thể tải giao diện danh sách đấu giá: " + e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void showBidResult(String message, boolean success) {
        bidResultLabel.setText(message);
        bidResultLabel.setTextFill(success ? Color.GREEN : Color.RED);
        bidResultLabel.setVisible(true);
    }

    private String formatDateTime(String raw) {
        if (raw == null || raw.isEmpty()) return "-";
        return raw.replace("T", " ").substring(0, Math.min(raw.length(), 16));
    }

    // ── Inner class: BidRow ────────────────────────────────────────────────

    public static class BidRow {
        private final SimpleStringProperty bidder;
        private final SimpleStringProperty amount;
        private final SimpleStringProperty time;

        public BidRow(String bidder, String amount, String time) {
            this.bidder = new SimpleStringProperty(bidder);
            this.amount = new SimpleStringProperty(amount);
            this.time   = new SimpleStringProperty(time);
        }

        public String getBidder() { return bidder.get(); }
        public String getAmount() { return amount.get(); }
        public String getTime()   { return time.get(); }
    }
}
