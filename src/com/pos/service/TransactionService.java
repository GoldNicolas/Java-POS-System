package com.pos.service;

import com.pos.model.*; // Import necessary model classes

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TransactionService {

    private final InventoryService inventoryService;
    // Store completed transactions (In-memory for this example)
    private final Map<String, Receipt> completedTransactions;

    public TransactionService(InventoryService inventoryService) {
        if (inventoryService == null) {
            throw new IllegalArgumentException("InventoryService cannot be null.");
        }
        this.inventoryService = inventoryService;
        this.completedTransactions = new HashMap<>(); // Using standard HashMap is fine for single-threaded Swing app
    }

    /**
     * Finds a completed receipt by its ID.
     * param receiptId The ID of the receipt to find.
     * return An Optional containing the Receipt if found, otherwise empty.
     */
    public Optional<Receipt> findReceiptById(String receiptId) {
        return Optional.ofNullable(completedTransactions.get(receiptId));
    }

    /**
     * Processes a sale transaction.
     * Validates stock and decreases inventory for items found in the main inventory.
     * Allows temporary items (found via InventoryService.findItem) to be included
     * in the sale without stock checks/decreases.
     * Creates and stores a receipt including all items.
     *
     * param items The list of items being purchased (can include temporary items).
     * param employee The employee processing the sale.
     * return The generated Receipt for the sale.
     * throws TransactionException If the sale cannot be processed (e.g., insufficient stock for an inventory item).
     */
    public Receipt processSale(List<TransactionItem> items, Employee employee) throws TransactionException {
        if (items == null || items.isEmpty()) {
            throw new TransactionException("Cannot process sale with empty item list.");
        }
         if (employee == null) {
             throw new TransactionException("Cannot process sale without a valid employee.");
         }

        // List to keep track of items that ARE found in inventory and need stock decrease
        List<TransactionItem> inventoryItemsToProcess = new ArrayList<>();

        // 1. Validate stock ONLY for items found in the *main* inventory
        for (TransactionItem transItem : items) {
            // Use the helper method to determine if it's a main inventory item
            if (inventoryService.isInventoryItem(transItem.getItem().getBarcode())) {
                // Item exists in main inventory - Perform stock validation
                // We need to fetch the item again to be sure about the current stock
                Optional<Item> inventoryItemOpt = inventoryService.findItem(transItem.getItem().getBarcode());
                if (inventoryItemOpt.isPresent()) { // Should be present if isInventoryItem was true
                    Item inventoryItem = inventoryItemOpt.get();
                    if (inventoryItem.getQuantityInStock() < transItem.getQuantity()) {
                        // Insufficient stock for an item that IS in inventory - Sale fails
                        throw new TransactionException("Insufficient stock for inventory item: " + inventoryItem.getName() +
                                " (Required: " + transItem.getQuantity() + ", Available: " + inventoryItem.getQuantityInStock() + ")");
                    }
                    // Add to the list of items whose stock needs decreasing later
                    inventoryItemsToProcess.add(transItem);
                } else {
                    // This case is unlikely if isInventoryItem was true, but handle defensively
                     throw new TransactionException("Inventory inconsistency for item: " + transItem.getItem().getBarcode() + ". Sale aborted.");
                }
            } else {
                // Item NOT found in main inventory - Assume it's a temporary item added via UI/session.
                // No stock check or validation needed for this item.
                // It will still be included in the final receipt.
                System.out.println("Info: Item " + transItem.getItem().getBarcode() + " is temporary, skipping inventory stock check for sale.");
            }
        }

        // 2. Decrease stock ONLY for the validated *inventory* items
        // This loop now only iterates through items confirmed to be in main inventory
        for (TransactionItem itemToSell : inventoryItemsToProcess) {
            // Call the inventoryService.sell method which internally only acts on inventory items
            boolean success = inventoryService.sell(itemToSell.getItem().getBarcode(), itemToSell.getQuantity());
            if (!success) {
                // This check remains important. If decreasing stock fails unexpectedly
                // (e.g., concurrency issue, stock changed between check and sell), abort.
                // In a real system, implement rollback logic here.
                throw new TransactionException("Failed to decrease stock for inventory item: " + itemToSell.getItem().getBarcode() + ". Sale aborted.");
            }
        }

        // 3. Create Receipt using the ORIGINAL list of items passed in
        // This ensures both inventory items and temporary items are on the receipt.
        Receipt receipt = new Receipt(items, employee); // Uses PURCHASE constructor

        // 4. Store Receipt
        completedTransactions.put(receipt.getReceiptId(), receipt);
        System.out.println("Sale successful. Receipt ID: " + receipt.getReceiptId());
        System.out.println(receipt.getFormattedReceipt()); // Log receipt details

        return receipt;
    }

    /**
     * Processes a return transaction.
     * Determines refund amount based on employee role and input.
     * Increases inventory stock *only for items belonging to the main inventory*.
     * Creates and stores a return receipt.
     *
     * param originalReceiptId The ID of the original purchase receipt (required).
     * param itemsToReturn The list of items being returned (can include temporary items).
     * param employee The employee processing the return.
     * param customRefundAmount Optional: A specific refund amount (only used if employee is Manager). If null, standard price is used.
     * return The generated Receipt for the return.
     * throws TransactionException If the return cannot be processed (e.g., missing original ID, invalid input).
     */
    public Receipt processReturn(String originalReceiptId, List<TransactionItem> itemsToReturn, Employee employee, Double customRefundAmount) throws TransactionException {
         if (itemsToReturn == null || itemsToReturn.isEmpty()) {
            throw new TransactionException("Cannot process return with empty item list.");
        }
        if (originalReceiptId == null || originalReceiptId.trim().isEmpty()) {
             throw new TransactionException("Original receipt ID is required for returns.");
        }
         if (employee == null) {
             throw new TransactionException("Cannot process return without a valid employee.");
         }

        

        // Determine refund amount 
        double finalRefundAmount;
        // Calculate refund based on prices captured in itemsToReturn (which came from findItem at return time)
        double calculatedRefund = itemsToReturn.stream()
                                            .mapToDouble(TransactionItem::getSubtotal)
                                            .sum();

        if (employee instanceof Manager manager && manager.canDoFlexibleRefund() && customRefundAmount != null) {
            if (customRefundAmount < 0) {
                 throw new TransactionException("Custom refund amount cannot be negative.");
            }
            finalRefundAmount = customRefundAmount;
             System.out.println("Manager (" + employee.getName() + ") processing return with custom amount: $" + String.format("%.2f", customRefundAmount));
        } else {
             finalRefundAmount = calculatedRefund;
              if (employee instanceof Manager) {
                 System.out.println("Manager (" + employee.getName() + ") processing return with standard calculated amount: $" + String.format("%.2f", calculatedRefund));
             } else if (employee instanceof Cashier) {
                  if (customRefundAmount != null && customRefundAmount > calculatedRefund) {
                       throw new TransactionException("Cashier cannot refund more than the item's calculated value. Calculated: $" + String.format("%.2f", calculatedRefund) + ", Attempted: $" + String.format("%.2f", customRefundAmount));
                   } else if (customRefundAmount != null && customRefundAmount <= calculatedRefund) { // Allow <=
                       finalRefundAmount = customRefundAmount; // Allow if specified and not exceeding
                       System.out.println("Info: Cashier processing return with specified refund amount: $" + String.format("%.2f", customRefundAmount));
                   } else {
                        System.out.println("Cashier (" + employee.getName() + ") processing return with standard calculated amount: $" + String.format("%.2f", calculatedRefund));
                   }
              }
        }

        // 3. Increase stock in inventory ONLY for main inventory items
        for (TransactionItem transItem : itemsToReturn) {
            String barcode = transItem.getItem().getBarcode();
            // *** Use the helper method to check if it's an inventory item ***
            if (inventoryService.isInventoryItem(barcode)) {
                // Attempt to restock only if it's a main inventory item
                boolean success = inventoryService.restock(
                    barcode,
                    transItem.getQuantity(),
                    employee // Pass the employee performing the return for permission check in restock
                );

                if (!success) {
                    // Log a warning if restocking failed (permission denied or item vanished)
                    System.err.println("Warning: Could not restock inventory item " + barcode + " during return. " +
                                       "Check permissions for employee " + employee.getEmployeeId() + " or item status.");
                    // Policy decision: Continue return or fail? Continue for now.
                }
            } else {
                // It's a temporary item, log info but do not attempt restock
                 System.out.println("Info: Processing return for item " + barcode + ".");
            }
        }

        // 4. Create RETURN Receipt (includes all returned items, temp or inventory)
        Receipt returnReceipt = new Receipt(itemsToReturn, employee, originalReceiptId, finalRefundAmount);


        // 5. Store Return Receipt
        completedTransactions.put(returnReceipt.getReceiptId(), returnReceipt);
        System.out.println("Return successful. Return Receipt ID: " + returnReceipt.getReceiptId());
         System.out.println(returnReceipt.getFormattedReceipt()); // Log receipt details

        return returnReceipt;
    }

    /**
     * Custom Exception for Transaction processing issues.
     */
    public static class TransactionException extends Exception {
        public TransactionException(String message) {
            super(message);
        }

        public TransactionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}