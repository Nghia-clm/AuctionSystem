package com.auction.dao;

import com.auction.model.BidTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BidTransactionDAO - Thao tác CRUD với bảng bid_transactions.
 */
public class BidTransactionDAO {

    private static final Logger LOGGER = Logger.getLogger(BidTransactionDAO.class.getName());

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    public boolean insert(BidTransaction tx) {
        String sql = "INSERT INTO bid_transactions (transaction_id, auction_id, bidder_id, bid_amount, bid_time) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tx.getId());
            ps.setString(2, tx.getAuctionId());
            ps.setString(3, tx.getBidderId());
            ps.setDouble(4, tx.getBidAmount());
            ps.setTimestamp(5, Timestamp.valueOf(tx.getTimestamp()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Insert BidTransaction failed", e);
            return false;
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────

    public List<BidTransaction> findByAuction(String auctionId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findByAuction failed: " + auctionId, e);
        }
        return list;
    }

    public List<BidTransaction> findByBidder(String bidderId) {
        List<BidTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE bidder_id = ? ORDER BY bid_time DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, bidderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findByBidder failed: " + bidderId, e);
        }
        return list;
    }

    public BidTransaction findHighestBid(String auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findHighestBid failed: " + auctionId, e);
        }
        return null;
    }

    // ── HELPER ────────────────────────────────────────────────────────────

    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        String id        = rs.getString("transaction_id");
        String auctionId = rs.getString("auction_id");
        String bidderId  = rs.getString("bidder_id");
        double amount    = rs.getDouble("bid_amount");
        LocalDateTime ts = rs.getTimestamp("bid_time").toLocalDateTime();
        return new BidTransaction(id, auctionId, bidderId, amount, ts);
    }
}
