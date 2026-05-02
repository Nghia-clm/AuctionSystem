package com.auction.dao;

import com.auction.factory.ItemFactory;
import com.auction.model.item.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ItemDAO - Thao tác CRUD với bảng items trong database.
 */
public class ItemDAO {

    private static final Logger LOGGER = Logger.getLogger(ItemDAO.class.getName());

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    public boolean insert(Item item, String sellerId) {
        String sql = "INSERT INTO items (item_id, seller_id, type, name, description, starting_price) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, sellerId);
            ps.setString(3, item.getType());
            ps.setString(4, item.getName());
            ps.setString(5, item.getDescription());
            ps.setDouble(6, item.getStartingPrice());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Insert item failed: " + item.getName(), e);
            return false;
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────

    public Item findById(String itemId) {
        String sql = "SELECT * FROM items WHERE item_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findById item failed: " + itemId, e);
        }
        return null;
    }

    public List<Item> findAll() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) items.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findAll items failed", e);
        }
        return items;
    }

    public List<Item> findBySeller(String sellerId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) items.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findBySeller failed: " + sellerId, e);
        }
        return items;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    public boolean update(String itemId, String name, String description) {
        String sql = "UPDATE items SET name = ?, description = ? WHERE item_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Update item failed: " + itemId, e);
            return false;
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public boolean delete(String itemId) {
        String sql = "DELETE FROM items WHERE item_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Delete item failed: " + itemId, e);
            return false;
        }
    }

    // ── HELPER ────────────────────────────────────────────────────────────

    private Item mapRow(ResultSet rs) throws SQLException {
        String id           = rs.getString("item_id");
        String type         = rs.getString("type");
        String name         = rs.getString("name");
        String description  = rs.getString("description");
        double startingPrice = rs.getDouble("starting_price");
        return ItemFactory.createItem(id, type, name, description, startingPrice);
    }
}
