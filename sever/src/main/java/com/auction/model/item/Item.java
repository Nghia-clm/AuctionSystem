package com.auction.model.item;

import com.auction.model.entity.Entity;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice;
    protected String sellerId;
    protected String category;

    public Item() { super(); }

    public Item(String name, String description, double startingPrice, String sellerId) {
        super();
        this.name          = name;
        this.description   = description;
        this.startingPrice = startingPrice;
        this.sellerId      = sellerId;
    }

    public String getName()          { return name; }
    public String getDescription()   { return description; }
    public double getStartingPrice() { return startingPrice; }
    public String getSellerId()      { return sellerId; }
    public String getCategory()      { return category; }
    public String getType()          { return category; }  // alias dùng trong DAO

    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(double price)     { this.startingPrice = price; }
    public void setSellerId(String sellerId)       { this.sellerId = sellerId; }
    public void setCategory(String category)       { this.category = category; }

    public abstract boolean validate();

    @Override
    public String toString() {
        return "Item{id='" + id + "', name='" + name
               + "', category='" + category
               + "', startingPrice=" + startingPrice + "}";
    }
}