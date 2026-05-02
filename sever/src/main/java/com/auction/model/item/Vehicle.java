package com.auction.model.item;

public class Vehicle extends Item {
    private String vehicleType; // CAR, MOTORBIKE, TRUCK
    private String brand;
    private int year;
    private int mileage;

    public Vehicle() {
        super();
        this.category = "VEHICLE";
    }

    public Vehicle(String name, String description,
                   double startingPrice, String sellerId,
                   String vehicleType, String brand, int year, int mileage) {
        super(name, description, startingPrice, sellerId);
        this.category    = "VEHICLE";
        this.vehicleType = vehicleType;
        this.brand       = brand;
        this.year        = year;
        this.mileage     = mileage;
    }

    public String getVehicleType()          { return vehicleType; }
    public String getBrand()                { return brand; }
    public int getYear()                    { return year; }
    public int getMileage()                 { return mileage; }
    public void setVehicleType(String type) { this.vehicleType = type; }
    public void setBrand(String brand)      { this.brand = brand; }
    public void setYear(int year)           { this.year = year; }
    public void setMileage(int mileage)     { this.mileage = mileage; }

    @Override
    public boolean validate() {
        if (name == null || name.isBlank())   return false;
        if (startingPrice <= 0)               return false;
        if (brand == null || brand.isBlank()) return false;
        if (year <= 1900)                     return false;
        if (mileage < 0)                      return false;
        return true;
    }

    @Override
    public void printInfo() {
        System.out.println("=== VEHICLE ===");
        System.out.println("ID             : " + id);
        System.out.println("Name           : " + name);
        System.out.println("Type           : " + vehicleType);
        System.out.println("Brand          : " + brand);
        System.out.println("Year           : " + year);
        System.out.println("Mileage        : " + mileage + " km");
        System.out.println("Starting Price : " + startingPrice);
        System.out.println("Description    : " + description);
    }
}
