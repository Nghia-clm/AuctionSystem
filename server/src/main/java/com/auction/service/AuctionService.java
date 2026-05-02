package com.auction.service;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidTransactionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.UserNotFoundException;
import com.auction.manager.AuctionManager;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.observer.BidObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuctionService - Xử lý logic nghiệp vụ đấu giá.
 * Thread-safe: dùng ReentrantReadWriteLock per auction từ AuctionManager.
 */
public class AuctionService {

    private static final Logger LOGGER = Logger.getLogger(AuctionService.class.getName());

    // Cấu hình anti-sniping được xử lý bởi AntiSnipingTimer (Observer Pattern)
    // Xem: advanced/src/main/java/com/auction/AntiSnipingTimer.java

    private final AuctionDAO         auctionDAO         = new AuctionDAO();
    private final BidTransactionDAO  bidTransactionDAO  = new BidTransactionDAO();
    private final ItemDAO            itemDAO            = new ItemDAO();
    private final UserDAO            userDAO            = new UserDAO();
    private final AuctionManager     auctionManager     = AuctionManager.getInstance();

    // Danh sách observer (realtime update)
    private final List<BidObserver> observers = new ArrayList<>();

    // ── OBSERVER ──────────────────────────────────────────────────────────

    public void addObserver(BidObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(BidObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(Auction auction, BidTransaction tx) {
        for (BidObserver obs : observers) {
            try {
                obs.onNewBid(auction, tx);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Observer notification failed", e);
            }
        }
    }

    // ── CREATE AUCTION ────────────────────────────────────────────────────

    /**
     * Tạo phiên đấu giá mới.
     */
    public Auction createAuction(String itemId, String sellerId,
                                  double startingPrice,
                                  String startTimeStr, String endTimeStr) {
        Item item = itemDAO.findById(itemId);
        if (item == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm: " + itemId);

        User seller = userDAO.findById(sellerId);
        if (seller == null) throw new IllegalArgumentException("Không tìm thấy người bán: " + sellerId);

        if (startingPrice <= 0) throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");

        LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
        LocalDateTime endTime   = LocalDateTime.parse(endTimeStr);

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        String auctionId = UUID.randomUUID().toString();
        Auction auction = new Auction(auctionId, item, seller, startingPrice, startTime, endTime);

        auctionDAO.insert(auction);
        auctionManager.addAuction(auction);

        LOGGER.info("Auction created: " + auctionId + " | Item: " + item.getName());
        return auction;
    }

    // ── PLACE BID ─────────────────────────────────────────────────────────

    /**
     * Đặt giá - thread-safe với ReentrantWriteLock per auction.
     * Tránh: lost update, race condition, hai người cùng thắng.
     *
     * @throws AuctionClosedException nếu phiên đã đóng
     * @throws InvalidBidException    nếu giá không hợp lệ
     * @throws UserNotFoundException  nếu bidder không tồn tại
     */
    public BidTransaction placeBid(String auctionId, String bidderId, double bidAmount)
            throws AuctionClosedException, InvalidBidException, UserNotFoundException {

        // Lấy write lock của auction này (chỉ 1 thread xử lý bid tại một thời điểm)
        ReentrantReadWriteLock.WriteLock lock = auctionManager.getWriteLock(auctionId);
        lock.lock();

        try {
            // 1. Load auction mới nhất từ DB (tránh stale data)
            Auction auction = auctionDAO.findById(auctionId);
            if (auction == null) throw new IllegalArgumentException("Không tìm thấy phiên đấu giá: " + auctionId);

            // 2. Kiểm tra trạng thái phiên
            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.CANCELED
                    || auction.getStatus() == AuctionStatus.PAID) {
                throw new AuctionClosedException("Phiên đấu giá đã đóng: " + auctionId);
            }

            if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                // Tự động đóng phiên nếu hết giờ
                auction.setStatus(AuctionStatus.FINISHED);
                auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
                throw new AuctionClosedException("Phiên đấu giá đã hết thời gian");
            }

            // 3. Kiểm tra giá đấu hợp lệ
            if (bidAmount <= auction.getCurrentPrice()) {
                throw new InvalidBidException(
                    "Bid amount " + bidAmount + " must be greater than current price " + auction.getCurrentPrice()
                );
            }

            // 4. Kiểm tra bidder tồn tại và không bị ban
            User bidder = userDAO.findById(bidderId);
            if (bidder == null) throw new UserNotFoundException("Không tìm thấy người đấu giá: " + bidderId);
            if (bidder.isBanned()) throw new IllegalStateException("Tài khoản người đấu giá đã bị khóa");

            // 5. Không cho seller tự đấu giá sản phẩm của mình
            if (auction.getSeller().getId().equals(bidderId)) {
                throw new InvalidBidException("Người bán không thể tự đặt giá cho phiên của mình");
            }

            // 6. Cập nhật giá hiện tại
            auction.setCurrentPrice(bidAmount);
            auction.setWinner(bidder);
            if (auction.getStatus() == AuctionStatus.OPEN) {
                auction.setStatus(AuctionStatus.RUNNING);
                auctionDAO.updateStatus(auctionId, AuctionStatus.RUNNING);
            }
            auctionDAO.updateCurrentPrice(auctionId, bidAmount, bidderId);

            // 7. Lưu transaction
            String txId = UUID.randomUUID().toString();
            BidTransaction tx = new BidTransaction(txId, auctionId, bidderId, bidAmount, LocalDateTime.now());
            bidTransactionDAO.insert(tx);

            // 8. Cập nhật cache trong AuctionManager
            auctionManager.addAuction(auction);

            // 9. Notify observers (AntiSnipingTimer là BidObserver xử lý anti-snipe)
            // Anti-sniping được xử lý duy nhất bởi AntiSnipingTimer qua Observer Pattern
            // Tránh gia hạn kép nếu dùng cả 2 cơ chế cùng lúc
            notifyObservers(auction, tx);

            LOGGER.info("Bid placed: auction=" + auctionId
                    + " | bidder=" + bidderId + " | amount=" + bidAmount);
            return tx;

        } finally {
            lock.unlock();
        }
    }

    // ── GET AUCTIONS ──────────────────────────────────────────────────────

    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }

    public List<Auction> getRunningAuctions() {
        return auctionDAO.findByStatus(AuctionStatus.RUNNING);
    }

    public Auction getAuctionById(String auctionId) {
        Auction auction = auctionDAO.findById(auctionId);
        if (auction == null) throw new IllegalArgumentException("Không tìm thấy phiên đấu giá: " + auctionId);
        return auction;
    }

    public List<Auction> getAuctionsBySeller(String sellerId) {
        return auctionDAO.findBySeller(sellerId);
    }

    // ── BID HISTORY ───────────────────────────────────────────────────────

    public List<BidTransaction> getBidHistory(String auctionId) {
        return bidTransactionDAO.findByAuction(auctionId);
    }

    // ── CLOSE AUCTION ─────────────────────────────────────────────────────

    /**
     * Đóng phiên thủ công (Admin hoặc hết giờ).
     */
    public void closeAuction(String auctionId) {
        ReentrantReadWriteLock.WriteLock lock = auctionManager.getWriteLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionDAO.findById(auctionId);
            if (auction == null) throw new IllegalArgumentException("Không tìm thấy phiên đấu giá");
            if (auction.getStatus() == AuctionStatus.FINISHED) return;

            auction.setStatus(AuctionStatus.FINISHED);
            auctionDAO.updateStatus(auctionId, AuctionStatus.FINISHED);
            LOGGER.info("Auction manually closed: " + auctionId);
        } finally {
            lock.unlock();
        }
    }

    public void cancelAuction(String auctionId) {
        ReentrantReadWriteLock.WriteLock lock = auctionManager.getWriteLock(auctionId);
        lock.lock();
        try {
            auctionDAO.updateStatus(auctionId, AuctionStatus.CANCELED);
            LOGGER.info("Auction canceled: " + auctionId);
        } finally {
            lock.unlock();
        }
    }
}