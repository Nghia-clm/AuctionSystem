package com.auction.model;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.InvalidBidException;
import com.auction.model.item.Item;
import com.auction.model.user.User;
import com.auction.observer.BidObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private String id;
    private Item item;
    private User seller;
    private User winner;
    private double startingPrice;
    private double currentPrice;
    private String currentWinnerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<BidObserver> observers = new ArrayList<>();

    public Auction() {}

    // Constructor đầy đủ dùng trong AuctionService
    public Auction(String id, Item item, User seller, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.id            = id;
        this.item          = item;
        this.seller        = seller;
        this.startingPrice = startingPrice;
        this.currentPrice  = startingPrice;
        this.startTime     = startTime;
        this.endTime       = endTime;
        this.status        = AuctionStatus.OPEN;
    }

    // Constructor không cần seller (dùng khi tạo đơn giản)
    public Auction(String id, Item item, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this(id, item, null, startingPrice, startTime, endTime);
    }

    // ── Observer ──────────────────────────────────────────────
    public void addObserver(BidObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(BidObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers() {
        for (BidObserver obs : observers) {
            obs.onBidUpdated(id, currentPrice, currentWinnerId);
        }
    }

    // ── Core logic ────────────────────────────────────────────
    public synchronized boolean placeBid(String bidderId, double amount) throws AuctionClosedException, InvalidBidException {
        if (status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá chưa mở hoặc đã đóng!");
        }
        if (amount <= currentPrice) {
            throw new InvalidBidException(
                "Giá đặt (" + amount + ") phải cao hơn giá hiện tại (" + currentPrice + ")");
        }
        if (bidderId.equals(currentWinnerId)) {
            throw new InvalidBidException("Bạn đang dẫn đầu, không cần đặt lại!");
        }
        this.currentPrice    = amount;
        this.currentWinnerId = bidderId;
        notifyObservers();
        return true;
    }

    public synchronized void start() {
        if (status != AuctionStatus.OPEN) {
            throw new IllegalStateException("Chỉ bắt đầu được khi trạng thái là OPEN");
        }
        this.status = AuctionStatus.RUNNING;
    }

    public synchronized void finish() {
        if (status != AuctionStatus.RUNNING) return;
        this.status = (currentWinnerId == null)
                      ? AuctionStatus.CANCELED
                      : AuctionStatus.FINISHED;
        notifyObservers();
    }

    public synchronized void cancel() {
        this.status = AuctionStatus.CANCELED;
        notifyObservers();
    }

    public synchronized void markPaid() {
        if (status == AuctionStatus.FINISHED) {
            this.status = AuctionStatus.PAID;
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    // ── Getters ───────────────────────────────────────────────
    public String getId()              { return id; }
    public Item getItem()              { return item; }
    public User getSeller()            { return seller; }
    public User getWinner()            { return winner; }
    public double getStartingPrice()   { return startingPrice; }
    public double getCurrentPrice()    { return currentPrice; }
    public String getCurrentWinnerId() { return currentWinnerId; }
    public LocalDateTime getStartTime(){ return startTime; }
    public LocalDateTime getEndTime()  { return endTime; }
    public AuctionStatus getStatus()   { return status; }

    // ── Setters ───────────────────────────────────────────────
    public void setId(String id)                 { this.id = id; }
    public void setItem(Item item)               { this.item = item; }
    public void setSeller(User seller)           { this.seller = seller; }
    public void setWinner(User winner)           { this.winner = winner; this.currentWinnerId = winner != null ? winner.getId() : null; }
    public void setStartingPrice(double p)       { this.startingPrice = p; }
    public void setCurrentPrice(double p)        { this.currentPrice = p; }
    public void setCurrentWinnerId(String id)    { this.currentWinnerId = id; }
    public void setStartTime(LocalDateTime t)    { this.startTime = t; }
    public void setEndTime(LocalDateTime t)      { this.endTime = t; }
    public void setStatus(AuctionStatus s)       { this.status = s; }

    @Override
    public String toString() {
        return "Auction{id='" + id
               + "', item='" + (item != null ? item.getName() : "null")
               + "', currentPrice=" + currentPrice
               + ", status=" + status + "}";
    }
}