package com.pos.service;

import com.pos.model.Inventory;
import com.pos.model.Item;
import com.pos.model.Employee;
import com.pos.model.Manager;
import com.pos.model.Cashier;

import java.util.List;
import java.util.Optional;

// This service acts as a facade and adds business logic around Inventory
public class InventoryService {
    private final Inventory inventory;
    // Define the threshold here, ensuring consistency with Inventory's internal logic if applicable.
    // Making it accessible via a getter allows other parts of the app (like UI) to know the value.
    private static final int LOW_STOCK_THRESHOLD = 10;

    /**
     * Constructor for InventoryService.
     * @param inventory The Inventory instance to manage. Must not be null.
     * @throws IllegalArgumentException if inventory is null.
     */
    public InventoryService(Inventory inventory) {
        if (inventory == null) {
             throw new IllegalArgumentException("Inventory cannot be null.");
        }
        this.inventory = inventory;
    }

    /**
     * Initializes the inventory with some sample items and logs initial stock status.
     */
    public void initializeInventory() {
        // Add some sample items
        inventory.addItem(new Item("BC001", "Apple", 0.50, 50));
        inventory.addItem(new Item("BC002", "Banana", 0.30, 100));
        inventory.addItem(new Item("BC003", "Orange Juice", 2.50, 30));
        inventory.addItem(new Item("BC004", "Bread Loaf", 3.00, 8)); // Low stock item
        inventory.addItem(new Item("BC005", "Milk Carton", 1.80, 15));
        inventory.addItem(new Item("BC006", "Coffee Beans", 8.99, 0)); // Out of stock item

        // Optional: Log initial stock warnings at startup for visibility
        System.out.println("--- Initial Inventory Stock Check ---");
        List<Item> lowStock = getLowStockItems();
        List<Item> outOfStock = getOutOfStockItems();
        if (!lowStock.isEmpty()) {
             System.out.println("LOW STOCK WARNINGS:");
            lowStock.forEach(item -> System.out.printf("  - %s (%s): %d left (Threshold <= %d)%n",
                    item.getName(), item.getBarcode(), item.getQuantityInStock(), LOW_STOCK_THRESHOLD));
        }
        if (!outOfStock.isEmpty()) {
            System.out.println("OUT OF STOCK ITEMS:");
            outOfStock.forEach(item -> System.out.printf("  - %s (%s)%n", item.getName(), item.getBarcode()));
        }
        if (lowStock.isEmpty() && outOfStock.isEmpty()) {
            System.out.println("All items are sufficiently stocked.");
        }
        System.out.println("-------------------------------------");
    }

    /** Finds an item by its barcode using the Inventory object. */
    public Optional<Item> findItem(String barcode) {
        return inventory.findItemByBarcode(barcode);
    }

    /**
     * Processes the sale of an item, decreasing its stock via the Inventory object.
     * Checks for low stock warning after the sale.
     * @return true if the sale was successful (stock decreased), false otherwise.
     */
    public boolean sell(String barcode, int quantity) {
         // Attempt to decrease stock in the underlying inventory
         boolean success = inventory.sellItem(barcode, quantity);
         if(success) {
             // After successful sale, check if the item is now low stock or out of stock
             // Use the findItemByBarcode method to get the updated item state for the warning check
             inventory.findItemByBarcode(barcode).ifPresent(inventory::checkLowStockWarning);
             // Alternatively, could directly call: inventory.checkLowStockWarning(barcode);
             // if that method internally finds the item. Using the Optional ensures we check the updated item.
         }
         return success;
    }

    /**
     * Restocks an item, increasing its stock via the Inventory object, subject to employee permissions.
     * Checks for low stock warning after a successful restock.
     * @param barcode Barcode of the item.
     * @param quantity Quantity to add (must be positive).
     * @param employee Employee performing the restock.
     * @return true if restock was successful, false otherwise (permission denied, item not found, or invalid quantity).
     */
    public boolean restock(String barcode, int quantity, Employee employee) {
        if (quantity <= 0) {
            System.err.println("Restock failed: Quantity must be positive.");
            return false;
        }
        if (employee == null) {
            System.err.println("Restock failed: Employee information is missing.");
            return false;
        }

        // Check if the employee has permission using methods on Employee subclasses
        boolean hasPermission = false;
        // Use pattern variable binding (Java 16+) for cleaner casting
        if (employee instanceof Manager manager) {
            hasPermission = manager.canRestock();
        } else if (employee instanceof Cashier cashier) {
             hasPermission = cashier.canRestock(); // Check if Cashiers have permission based on their class definition
        }
        // Add checks for other potential roles here if they exist

        if (hasPermission) {
            // Employee has permission, attempt to restock in the inventory
             boolean success = inventory.restockItem(barcode, quantity);
              if (!success) {
                 // Log failure if item wasn't found (restockItem returns false in that case)
                 System.err.println("Restock failed: Item with barcode '" + barcode + "' not found in inventory.");
              } else {
                  // Check stock level after successful restock
                  System.out.println("Restock successful for item: " + barcode + ", Quantity added: " + quantity);
                  inventory.findItemByBarcode(barcode).ifPresent(inventory::checkLowStockWarning);
              }
             return success; // Return the result of the inventory operation
        } else {
            // Log permission denial clearly
            System.err.println("Permission Denied: Employee " + employee.getEmployeeId() + " (" + employee.getRole() + ") cannot perform restock operations.");
            return false; // Return false because permission was denied
        }
    }

    /** Gets a list of items currently at or below the low stock threshold (but not out of stock) from Inventory. */
    public List<Item> getLowStockItems() {
        return inventory.getLowStockItems();
    }

    /** Gets a list of items currently out of stock (quantity 0) from Inventory. */
     public List<Item> getOutOfStockItems() {
        return inventory.getOutOfStockItems();
    }

    /** Gets a string indicating the stock status (e.g., "In Stock", "LOW STOCK (5)", "OUT OF STOCK") from Inventory. */
     public String getStockStatus(String barcode) {
         return inventory.checkStockLevelStatus(barcode);
     }

    /** Returns a list of all items currently in the inventory by delegating to Inventory. */
     public List<Item> getAllItems() {
         return inventory.getAllItems();
     }

    /**
     * Returns the defined low stock threshold value used by this service/system.
     * Allows UI or other components to access the threshold consistently.
     * @return The low stock threshold quantity.
     */
     public int getLowStockThreshold() {
        // Return the value defined in this service.
        return LOW_STOCK_THRESHOLD;
     }
}