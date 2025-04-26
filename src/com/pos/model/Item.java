package com.pos.model;

import java.util.Objects;

public class Item {
    private final String barcode; // Unique identifier
    private String name;
    private double price;
    private int quantityInStock;

    public Item(String barcode, String name, double price, int initialQuantity) {
         if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Barcode cannot be empty.");
        }
         if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty.");
        }
         if (price < 0) {
             throw new IllegalArgumentException("Price cannot be negative.");
         }
         if (initialQuantity < 0) {
             throw new IllegalArgumentException("Initial quantity cannot be negative.");
         }
        this.barcode = barcode;
        this.name = name;
        this.price = price;
        this.quantityInStock = initialQuantity;
    }

    // Getters
    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantityInStock() { return quantityInStock; }

    // Setters (only for mutable fields)
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) {
         if (price >= 0) {
            this.price = price;
         }
    }

    // Stock Management
    public boolean decreaseStock(int quantity) {
        if (quantity > 0 && this.quantityInStock >= quantity) {
            this.quantityInStock -= quantity;
            return true;
        }
        return false; // Not enough stock or invalid quantity
    }

    public void increaseStock(int quantity) {
        if (quantity > 0) {
            this.quantityInStock += quantity;
        }
    }

    @Override
    public String toString() {
        return String.format("'%s' (ID: %s) - $%.2f [%d in stock]", name, barcode, price, quantityInStock);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(barcode, item.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode);
    }
}