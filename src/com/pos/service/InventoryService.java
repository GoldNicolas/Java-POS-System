package com.pos.service;

import com.pos.model.Inventory;
import com.pos.model.Item;
import com.pos.model.Employee;
import com.pos.model.Manager;
import com.pos.model.Cashier;

import java.util.List;
import java.util.Map; // Import Map
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap; // Keep using ConcurrentHashMap for potential future thread safety

// This service acts as a facade and adds business logic around Inventory
public class InventoryService {
    private final Inventory inventory;
    // New map to store items added temporarily during the session
    private final Map<String, Item> temporaryItems;
    // Define the threshold here, ensuring consistency with Inventory's internal logic if applicable.
    // Making it accessible via a getter allows other parts of the app (like UI) to know the value.
    private static final int LOW_STOCK_THRESHOLD = 10;

    /**
     * Constructor for InventoryService.
     * param inventory The Inventory instance to manage. Must not be null.
     * throws IllegalArgumentException if inventory is null.
     */
    public InventoryService(Inventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("Inventory cannot be null.");
        }
        this.inventory = inventory;
        // Initialize the temporary items map
        this.temporaryItems = new ConcurrentHashMap<>();
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

    /**
     * Finds an item by its barcode, checking both the main inventory and
     * the session's temporary items.
     *
     * param barcode The barcode to search for.
     * return An Optional containing the Item if found in either inventory or
     *         temporary storage, otherwise Optional.empty().
     */
    public Optional<Item> findItem(String barcode) {
        // 1. Check main inventory first
        Optional<Item> inventoryItem = inventory.findItemByBarcode(barcode);
        if (inventoryItem.isPresent()) {
            return inventoryItem;
        }
        // 2. If not in inventory, check temporary items
        return Optional.ofNullable(temporaryItems.get(barcode));
    }

    /**
     * Adds a new item to the temporary storage for the current application session.
     * These items are not persisted after the application closes and don't affect main inventory stock.
     *
     * param item The Item object to add temporarily. Should not be null.
     * throws IllegalArgumentException if item is null or barcode already exists temporarily.
     */
    public void addTemporaryItem(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot add a null temporary item.");
        }
        // Optional: Check if barcode conflicts with main inventory (policy decision)
        // if (inventory.findItemByBarcode(item.getBarcode()).isPresent()) {
        //    throw new IllegalArgumentException("Cannot add temporary item: Barcode '" + item.getBarcode() + "' already exists in main inventory.");
        // }

        // Add to temporary map, potentially overwriting if the same barcode was added temporarily before (unlikely but possible)
        if (temporaryItems.containsKey(item.getBarcode())) {
            System.out.println("Warning: Overwriting existing item with barcode " + item.getBarcode());
        }
        temporaryItems.put(item.getBarcode(), item);
        System.out.println("Added item: " + item.getBarcode() + " - " + item.getName());
    }

    /**
     * Checks if an item with the given barcode exists in the main inventory.
     *
     * param barcode The barcode to check.
     * return true if the item exists in the main inventory, false otherwise (even if it exists temporarily).
     */
    public boolean isInventoryItem(String barcode) {
        return inventory.findItemByBarcode(barcode).isPresent();
    }


    /**
     * Processes the sale of an item, decreasing its stock via the Inventory object *only*
     * if the item exists in the main inventory. Does nothing for temporary items.
     * Checks for low stock warning after the sale if it was an inventory item.
     * return true if the sale was applicable to an inventory item and stock was decreased,
     *         true if it was a temporary item (no action needed),
     *         false if decreasing stock failed for an inventory item.
     */
    public boolean sell(String barcode, int quantity) {
        // Check if it's a main inventory item first
        Optional<Item> inventoryItemOpt = inventory.findItemByBarcode(barcode);
        if(inventoryItemOpt.isPresent()) {
            // It's an inventory item, attempt to decrease stock
            boolean success = inventory.sellItem(barcode, quantity);
            if(success) {
                // After successful sale, check if the item is now low stock or out of stock
                inventory.checkLowStockWarning(barcode); // Use inventory's method directly
            }
            return success;
        } else {
            // It's either a temporary item or doesn't exist at all.
            // If it's temporary, no stock action is needed, so consider it "successful" in terms of processing the sale line.
            // If it doesn't exist, findItem should have caught it earlier, but we return true here as no *inventory* action failed.
            return true;
        }
    }

    /**
     * Restocks an item, increasing its stock via the Inventory object, subject to employee permissions.
     * This only applies to items in the main inventory. It will fail for temporary items.
     * Checks for low stock warning after a successful restock.
     * param barcode Barcode of the item (must exist in main inventory).
     * param quantity Quantity to add (must be positive).
     * param employee Employee performing the restock.
     * return true if restock was successful, false otherwise (permission denied, item not found in main inventory, or invalid quantity).
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

        // Check permissions
        boolean hasPermission = (employee instanceof Manager manager && manager.canRestock()) ||
                                (employee instanceof Cashier cashier && cashier.canRestock()); // Simplified check

        if (!hasPermission) {
            System.err.println("Permission Denied: Employee " + employee.getEmployeeId() + " (" + employee.getRole() + ") cannot perform restock operations.");
            return false;
        }

        // IMPORTANT: Restock only works on main inventory items. restockItem handles the 'not found' check.
        boolean success = inventory.restockItem(barcode, quantity);
        if (success) {
            System.out.println("Restock successful for inventory item: " + barcode + ", Quantity added: " + quantity);
            inventory.checkLowStockWarning(barcode); // Check stock after successful restock
        } else {
            // restockItem logs "not found", so we don't need redundant logging here unless adding detail.
            System.err.println("Restock failed: Item '" + barcode + "' not found in main inventory or other error occurred.");
        }
        return success;
    }

    /** Gets a list of items currently at or below the low stock threshold (but not out of stock) from main Inventory. */
    public List<Item> getLowStockItems() {
        return inventory.getLowStockItems();
    }

    /** Gets a list of items currently out of stock (quantity 0) from main Inventory. */
    public List<Item> getOutOfStockItems() {
        return inventory.getOutOfStockItems();
    }

    /** Gets a string indicating the stock status from main Inventory. Returns "ITEM NOT FOUND" if not in main inventory. */
    public String getStockStatus(String barcode) {
        // This method inherently only checks main inventory via inventory.checkStockLevelStatus
        return inventory.checkStockLevelStatus(barcode);
    }

    /** Returns a list of all items currently in the main inventory by delegating to Inventory. */
    public List<Item> getAllItems() {
        // This should probably only return main inventory items for management purposes
        return inventory.getAllItems();
    }

    /**
     * Returns the defined low stock threshold value used by this service/system.
     * Allows UI or other components to access the threshold consistently.
     * return The low stock threshold quantity.
     */
    public int getLowStockThreshold() {
        // Return the value defined in this service.
        return LOW_STOCK_THRESHOLD;
    }
}