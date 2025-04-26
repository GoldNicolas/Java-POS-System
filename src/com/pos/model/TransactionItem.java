package com.pos.model;

// Represents an item line in a transaction (captures quantity and price at time of sale)
public class TransactionItem {
    private final Item item; // Reference to the inventory item
    private final int quantity;
    private final double priceAtTransaction; // Price might change later in inventory

    public TransactionItem(Item item, int quantity) {
        if (item == null) throw new IllegalArgumentException("Item cannot be null");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");

        this.item = item;
        this.quantity = quantity;
        this.priceAtTransaction = item.getPrice(); // Capture current price
    }

    public Item getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPriceAtTransaction() {
        return priceAtTransaction;
    }

    public double getSubtotal() {
        return priceAtTransaction * quantity;
    }

    @Override
    public String toString() {
        return String.format("%d x %s @ $%.2f = $%.2f",
                quantity, item.getName(), priceAtTransaction, getSubtotal());
    }
}