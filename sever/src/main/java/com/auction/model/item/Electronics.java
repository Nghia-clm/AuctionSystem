package com.auction.model.item;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;
    private String condition; // NEW, USED, REFURBISHED

    public Electronics() {
        super();
        this.category = "ELECTRONICS";
    }

    public Electronics(String name, String description,
                       double startingPrice, String sellerId,
                       String brand, int warrantyMonths, String condition) {
        super(name, description, startingPrice, sellerId);
        this.category       = "ELECTRONICS";
        this.brand          = brand;
        this.warrantyMonths = warrantyMonths;
        this.condition      = condition;
    }

    public String getBrand()             { return brand; }
    public int getWarrantyMonths()       { return warrantyMonths; }
    public String getCondition()         { return condition; }
    public void setBrand(String brand)   { this.brand = brand; }
    public void setWarrantyMonths(int w) { this.warrantyMonths = w; }
    public void setCondition(String c)   { this.condition = c; }

    @Override
    public boolean validate() {
        if (name == null || name.isBlank())   return false;
        if (startingPrice <= 0)               return false;
        if (brand == null || brand.isBlank()) return false;
        return true;
    }

    @Override
    public void printInfo() {
        System.out.println("=== ELECTRONICS ===");
        System.out.println("ID             : " + id);
        System.out.println("Name           : " + name);
        System.out.println("Brand          : " + brand);
        System.out.println("Condition      : " + condition);
        System.out.println("Warranty       : " + warrantyMonths + " months");
        System.out.println("Starting Price : " + startingPrice);
        System.out.println("Description    : " + description);
    }
}
