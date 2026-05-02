package com.auction.dao;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.model.user.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AuctionDAO - Thao tác CRUD với bảng auctions trong database.
 */
public class AuctionDAO {

    private static final Logger LOGGER = Logger.getLogger(AuctionDAO.class.getName());
    private final ItemDAO itemDAO = new ItemDAO();
    private final UserDAO userDAO = new UserDAO();

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    public boolean insert(Auction auction) {
        String sql = """
            INSERT INTO auctions
                (auction_id, item_id, seller_id, starting_price, current_price,
                 status, start_time, end_time, winner_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, auction.getId());
            ps.setString(2, auction.getItem().getId());
            ps.setString(3, auction.getSeller().getId());
            ps.setDouble(4, auction.getStartingPrice());
            ps.setDouble(5, auction.getCurrentPrice());
            ps.setString(6, auction.getStatus().name());
            ps.setTimestamp(7, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(8, Timestamp.valueOf(auction.getEndTime()));
            ps.setString(9, auction.getWinner() != null ? auction.getWinner().getId() : null);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Insert auction failed: " + auction.getId(), e);
            return false;
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────

    public Auction findById(String auctionId) {
        String sql = "SELECT * FROM auctions WHERE auction_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findById auction failed: " + auctionId, e);
        }
        return null;
    }

    public List<Auction> findAll() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions ORDER BY start_time DESC";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) auctions.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findAll auctions failed", e);
        }
        return auctions;
    }

    public List<Auction> findByStatus(AuctionStatus status) {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) auctions.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findByStatus failed: " + status, e);
        }
        return auctions;
    }

    public List<Auction> findBySeller(String sellerId) {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE seller_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) auctions.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findBySeller failed: " + sellerId, e);
        }
        return auctions;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    public boolean updateCurrentPrice(String auctionId, double newPrice, String winnerId) {
        String sql = "UPDATE auctions SET current_price = ?, winner_id = ? WHERE auction_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setString(2, winnerId);
            ps.setString(3, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "updateCurrentPrice failed: " + auctionId, e);
            return false;
        }
    }

    public boolean updateStatus(String auctionId, AuctionStatus status) {
        String sql = "UPDATE auctions SET status = ? WHERE auction_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "updateStatus failed: " + auctionId, e);
            return false;
        }
    }

    public boolean updateEndTime(String auctionId, LocalDateTime newEndTime) {
        String sql = "UPDATE auctions SET end_time = ? WHERE auction_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(newEndTime));
            ps.setString(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "updateEndTime failed: " + auctionId, e);
            return false;
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public boolean delete(String auctionId) {
        String sql = "DELETE FROM auctions WHERE auction_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Delete auction failed: " + auctionId, e);
            return false;
        }
    }

    // ── HELPER ────────────────────────────────────────────────────────────

    private Auction mapRow(ResultSet rs) throws SQLException {
        String auctionId    = rs.getString("auction_id");
        double startPrice   = rs.getDouble("starting_price");
        double currentPrice = rs.getDouble("current_price");
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime   = rs.getTimestamp("end_time").toLocalDateTime();

        Item item     = itemDAO.findById(rs.getString("item_id"));
        User seller   = userDAO.findById(rs.getString("seller_id"));
        String winnerId = rs.getString("winner_id");
        User winner   = (winnerId != null) ? userDAO.findById(winnerId) : null;

        Auction auction = new Auction(auctionId, item, seller, startPrice, startTime, endTime);
        auction.setCurrentPrice(currentPrice);
        auction.setStatus(status);
        auction.setWinner(winner);
        return auction;
    }
}
