package com.pos.model;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap; // Thread-safe for potential future enhancements
import java.util.stream.Collectors;

public class Inventory {
    private final Map<String, Item> items; // Barcode -> Item mapping
    private static final int LOW_STOCK_THRESHOLD = 10; // Example threshold

    public Inventory() {
        this.items = new ConcurrentHashMap<>();
    }

    // --- Item Management ---

    public void addItem(Item item) {
        if (item != null) {
            // If item exists, maybe update? For now, let's assume adding new only or use restock
             if(items.containsKey(item.getBarcode())){
                System.out.println("Warning: Item with barcode " + item.getBarcode() + " already exists. Use restockItem to add quantity.");
                // Optional: Update details like name/price if needed
                // Item existingItem = items.get(item.getBarcode());
                // existingItem.setName(item.getName());
                // existingItem.setPrice(item.getPrice());
             } else {
                items.put(item.getBarcode(), item);
             }
        }
    }

    public Optional<Item> findItemByBarcode(String barcode) {
        return Optional.ofNullable(items.get(barcode)); // Return an Optional to avoid null checks
    }

    public List<Item> getAllItems() {
        return new ArrayList<>(items.values()); // Return a copy of the item list
    }

    // --- Stock Operations ---
    // Sell
    // Restock

    public boolean sellItem(String barcode, int quantity) {
        Optional<Item> itemOpt = findItemByBarcode(barcode); // Find item by barcode
        if (itemOpt.isPresent()) {
            return itemOpt.get().decreaseStock(quantity); // If item exists, try to decrease stock
        }
        return false; // Item not found
    }

    public boolean restockItem(String barcode, int quantity) { // Restock item by barcode
        Optional<Item> itemOpt = findItemByBarcode(barcode); // Find item by barcode
        if (itemOpt.isPresent()) { // If item exists, increase stock
            itemOpt.get().increaseStock(quantity);
            System.out.println("Restocked " + quantity + " of item " + barcode + ". New stock: " + itemOpt.get().getQuantityInStock());
            checkLowStockWarning(itemOpt.get()); // Check stock after restocking
            return true;
        } else {
            System.err.println("Restock failed: Item with barcode " + barcode + " not found in inventory.");
            return false; // Item not found
        }
    }

    // --- Stock Checking ---
    // Check stock levels
    // Check low stock items

    public List<Item> getLowStockItems() {
        return items.values().stream()
                .filter(item -> item.getQuantityInStock() <= LOW_STOCK_THRESHOLD && item.getQuantityInStock() > 0)
                .collect(Collectors.toList());
    }

    public List<Item> getOutOfStockItems() {
        return items.values().stream()
                .filter(item -> item.getQuantityInStock() == 0)
                .collect(Collectors.toList());
    }


    public String checkStockLevelStatus(String barcode) {
        Optional<Item> itemOpt = findItemByBarcode(barcode);
        if (itemOpt.isPresent()) {
            Item item = itemOpt.get();
            if (item.getQuantityInStock() <= 0) {
                 return "OUT OF STOCK";
            } else if (item.getQuantityInStock() <= LOW_STOCK_THRESHOLD) {
                 return "LOW STOCK (" + item.getQuantityInStock() + ")";
            } else {
                return "In Stock (" + item.getQuantityInStock() + ")";
            }
        } else {
            return "ITEM NOT FOUND";
        }
    }

    public void checkLowStockWarning(Item item) {
        if (item.getQuantityInStock() <= LOW_STOCK_THRESHOLD && item.getQuantityInStock() > 0) {
            System.out.println("LOW STOCK WARNING: Item '" + item.getName() + "' (ID: " + item.getBarcode() + ") has only " + item.getQuantityInStock() + " units left!");
            // In a real GUI app, you'd show this in the UI, not System.out
        } else if (item.getQuantityInStock() == 0) {
             System.out.println("OUT OF STOCK ALERT: Item '" + item.getName() + "' (ID: " + item.getBarcode() + ") is now out of stock!");
        }
    }
     // Overload to check by barcode
    public void checkLowStockWarning(String barcode) {
        findItemByBarcode(barcode).ifPresent(this::checkLowStockWarning);
    }
}