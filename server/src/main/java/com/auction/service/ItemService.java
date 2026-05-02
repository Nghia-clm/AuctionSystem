package com.auction.service;

import com.auction.dao.ItemDAO;
import com.auction.factory.ItemFactory;
import com.auction.model.item.Item;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ItemService - Xử lý logic nghiệp vụ liên quan đến sản phẩm đấu giá.
 */
public class ItemService {

    private static final Logger LOGGER = Logger.getLogger(ItemService.class.getName());
    private final ItemDAO itemDAO = new ItemDAO();

    // ── CREATE ────────────────────────────────────────────────────────────

    /**
     * Tạo sản phẩm mới bằng ItemFactory.
     * @param type         loại sản phẩm: ELECTRONICS, ART, VEHICLE
     * @param name         tên sản phẩm
     * @param description  mô tả
     * @param startingPrice giá khởi điểm
     * @param sellerId     ID người bán
     */
    public Item createItem(String type, String name, String description,
                            double startingPrice, String sellerId) {
        // Validate
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Item name cannot be empty");
        if (startingPrice <= 0)
            throw new IllegalArgumentException("Starting price must be positive");

        String id = UUID.randomUUID().toString();
        Item item = ItemFactory.createItem(id, type, name, description, startingPrice);

        boolean saved = itemDAO.insert(item, sellerId);
        if (!saved) throw new RuntimeException("Failed to save item to database");

        LOGGER.info("Item created: " + name + " [" + type + "] by seller=" + sellerId);
        return item;
    }

    // ── READ ──────────────────────────────────────────────────────────────

    public Item getItemById(String itemId) {
        Item item = itemDAO.findById(itemId);
        if (item == null) throw new IllegalArgumentException("Item not found: " + itemId);
        return item;
    }

    public List<Item> getAllItems() {
        return itemDAO.findAll();
    }

    public List<Item> getItemsBySeller(String sellerId) {
        return itemDAO.findBySeller(sellerId);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────

    /**
     * Cập nhật thông tin sản phẩm.
     * Chỉ seller sở hữu mới được sửa (kiểm tra ở tầng Controller/Handler).
     */
    public void updateItem(String itemId, String name, String description, String requesterId) {
        Item existing = itemDAO.findById(itemId);
        if (existing == null) throw new IllegalArgumentException("Item not found: " + itemId);

        String newName = (name != null && !name.isBlank()) ? name : existing.getName();
        String newDesc = (description != null) ? description : existing.getDescription();

        boolean updated = itemDAO.update(itemId, newName, newDesc);
        if (!updated) throw new RuntimeException("Failed to update item: " + itemId);

        LOGGER.info("Item updated: " + itemId + " by " + requesterId);
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    public void deleteItem(String itemId, String requesterId) {
        Item existing = itemDAO.findById(itemId);
        if (existing == null) throw new IllegalArgumentException("Item not found: " + itemId);

        boolean deleted = itemDAO.delete(itemId);
        if (!deleted) throw new RuntimeException("Failed to delete item: " + itemId);

        LOGGER.info("Item deleted: " + itemId + " by " + requesterId);
    }
}
