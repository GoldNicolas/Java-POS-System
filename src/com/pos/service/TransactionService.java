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
     * @param receiptId The ID of the receipt to find.
     * @return An Optional containing the Receipt if found, otherwise empty.
     */
    public Optional<Receipt> findReceiptById(String receiptId) {
        return Optional.ofNullable(completedTransactions.get(receiptId));
    }

    /**
     * Processes a sale transaction.
     * Validates stock and decreases inventory for items found in the main inventory.
     * Allows temporary items (not found in inventory) to be included in the sale without stock checks/decreases.
     * Creates and stores a receipt including all items.
     *
     * @param items The list of items being purchased (can include temporary items).
     * @param employee The employee processing the sale.
     * @return The generated Receipt for the sale.
     * @throws TransactionException If the sale cannot be processed (e.g., insufficient stock for an inventory item).
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

        // 1. Validate stock ONLY for items found in inventory
        for (TransactionItem transItem : items) {
            Optional<Item> inventoryItemOpt = inventoryService.findItem(transItem.getItem().getBarcode());

            if (inventoryItemOpt.isPresent()) {
                // Item exists in inventory - Perform stock validation
                Item inventoryItem = inventoryItemOpt.get();
                if (inventoryItem.getQuantityInStock() < transItem.getQuantity()) {
                    // Insufficient stock for an item that IS in inventory - Sale fails
                    throw new TransactionException("Insufficient stock for item: " + inventoryItem.getName() +
                            " (Required: " + transItem.getQuantity() + ", Available: " + inventoryItem.getQuantityInStock() + ")");
                }
                // Add to the list of items whose stock needs decreasing later
                inventoryItemsToProcess.add(transItem);
            } else {
                // Item NOT found in inventory - Assume it's a temporary item added via UI.
                // No stock check or validation needed for this item.
                // It will still be included in the final receipt.
                System.out.println("Info: Item " + transItem.getItem().getBarcode() + " not found in inventory, processing as temporary for this sale.");
            }
        }

        // 2. Decrease stock ONLY for the validated inventory items
        // This loop now only iterates through items confirmed to be in inventory
        for (TransactionItem itemToSell : inventoryItemsToProcess) {
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
     * Determines refund amount based on employee role and input, increases inventory, creates and stores a return receipt.
     *
     * @param originalReceiptId The ID of the original purchase receipt (required).
     * @param itemsToReturn The list of items being returned.
     * @param employee The employee processing the return.
     * @param customRefundAmount Optional: A specific refund amount (only used if employee is Manager). If null, standard price is used.
     * @return The generated Receipt for the return.
     * @throws TransactionException If the return cannot be processed (e.g., missing original ID, invalid input).
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

        // Optional: Find and validate against original receipt (enhancement)
        // Optional<Receipt> originalReceiptOpt = findReceiptById(originalReceiptId);
        // if (!originalReceiptOpt.isPresent()) {
        //      System.out.println("Warning: Original receipt " + originalReceiptId + " not found. Proceeding with return.");
        // }

        // 2. Determine refund amount based on employee role
        double finalRefundAmount;
        double calculatedRefund = itemsToReturn.stream()
                                            .mapToDouble(TransactionItem::getSubtotal)
                                            .sum();

        if (employee instanceof Manager manager && manager.canDoFlexibleRefund() && customRefundAmount != null) { // Simplified check
            // Manager provided a custom refund amount
            if (customRefundAmount < 0) {
                 throw new TransactionException("Custom refund amount cannot be negative.");
            }
            finalRefundAmount = customRefundAmount;
             System.out.println("Manager (" + employee.getName() + ") processing return with custom amount: $" + String.format("%.2f", customRefundAmount));
        } else {
            // Cashier or Manager (without custom amount) refunds standard calculated price
             finalRefundAmount = calculatedRefund;
              if (employee instanceof Manager) { // Manager using standard amount
                 System.out.println("Manager (" + employee.getName() + ") processing return with standard calculated amount: $" + String.format("%.2f", calculatedRefund));
             } else if (employee instanceof Cashier) { // Cashier always uses standard amount
                  // Check if Cashier tried to give more via the customRefundAmount parameter (should be null for Cashier)
                  if (customRefundAmount != null && customRefundAmount > calculatedRefund) {
                       throw new TransactionException("Cashier cannot refund more than the item's calculated value. Calculated: $" + String.format("%.2f", calculatedRefund) + ", Attempted: $" + String.format("%.2f", customRefundAmount));
                   } else if (customRefundAmount != null && customRefundAmount < calculatedRefund) {
                       // This case might be allowed depending on policy, currently no check
                       System.out.println("Info: Cashier processing return with specified refund amount: $" + String.format("%.2f", customRefundAmount));
                       finalRefundAmount = customRefundAmount; // Allow if needed, but typically standard is used.
                   } else {
                        System.out.println("Cashier (" + employee.getName() + ") processing return with standard calculated amount: $" + String.format("%.2f", calculatedRefund));
                   }
              }
        }

        // 3. Increase stock in inventory (restock returned items)
        // IMPORTANT: Returns typically only apply to items that *were* in inventory.
        // The current PosPanel logic for returns already enforces this by only allowing
        // returns for barcodes found via inventoryService.findItem.
        // Therefore, we can reasonably assume itemsToReturn contains only inventory items here.
        for (TransactionItem transItem : itemsToReturn) {
            boolean success = inventoryService.restock(
                transItem.getItem().getBarcode(),
                transItem.getQuantity(),
                employee // Pass the employee performing the return for permission check
            );

            if (!success) {
                // Log a warning if restocking failed
                System.err.println("Warning: Could not restock item " + transItem.getItem().getBarcode() + " during return. " +
                                   "Reason: Could be insufficient permissions for employee " + employee.getEmployeeId() + ".");
                // Policy decision: Continue return or fail? Continue for now.
            }
        }

        // 4. Create RETURN Receipt
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