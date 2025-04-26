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
     * Validates stock, decreases inventory, creates and stores a receipt.
     *
     * @param items The list of items being purchased.
     * @param employee The employee processing the sale.
     * @return The generated Receipt for the sale.
     * @throws TransactionException If the sale cannot be processed (e.g., insufficient stock, empty list).
     */
    public Receipt processSale(List<TransactionItem> items, Employee employee) throws TransactionException {
        if (items == null || items.isEmpty()) {
            throw new TransactionException("Cannot process sale with empty item list.");
        }
         if (employee == null) {
             throw new TransactionException("Cannot process sale without a valid employee.");
         }

        // 1. Validate stock for all items *before* processing
        for (TransactionItem transItem : items) {
            Item inventoryItem = inventoryService.findItem(transItem.getItem().getBarcode())
                    .orElseThrow(() -> new TransactionException("Item not found in inventory: " + transItem.getItem().getBarcode()));
            if (inventoryItem.getQuantityInStock() < transItem.getQuantity()) {
                throw new TransactionException("Insufficient stock for item: " + inventoryItem.getName() +
                        " (Required: " + transItem.getQuantity() + ", Available: " + inventoryItem.getQuantityInStock() + ")");
            }
        }

        // 2. Decrease stock in inventory
        for (TransactionItem transItem : items) {
            boolean success = inventoryService.sell(transItem.getItem().getBarcode(), transItem.getQuantity());
            if (!success) {
                // This shouldn't happen if validation passed, but good practice to check
                // In a real system with concurrency, this could potentially fail.
                // Consider implementing rollback logic here if needed (add stock back for items already processed).
                throw new TransactionException("Failed to decrease stock for item: " + transItem.getItem().getBarcode() + ". Sale aborted.");
            }
        }

        // 3. Create Receipt
        Receipt receipt = new Receipt(items, employee); // Uses PURCHASE constructor

        // 4. Store Receipt
        completedTransactions.put(receipt.getReceiptId(), receipt);
        System.out.println("Sale successful. Receipt ID: " + receipt.getReceiptId());
        System.out.println(receipt.getFormattedReceipt()); // Log receipt details to console

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

        if (employee instanceof Manager && customRefundAmount != null) {
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
                   }
                   System.out.println("Cashier (" + employee.getName() + ") processing return with standard calculated amount: $" + String.format("%.2f", calculatedRefund));
              }
        }

        // 3. Increase stock in inventory (restock returned items)
        for (TransactionItem transItem : itemsToReturn) {
            // *** THIS IS THE CORRECTED PART ***
            // Call the public restock method in InventoryService.
            // It handles the inventory update and checks permissions internally.
            // Note: If a Cashier without restock permission performs a return,
            // the restock method *will* deny the stock increase based on its permission check.
            // This might be desired behavior (items returned by cashiers aren't put back in stock)
            // or might require a different approach (e.g., a separate 'returnItemToStock' method
            // in InventoryService that bypasses normal permission checks).
            // We proceed with the standard restock call for now.
            boolean success = inventoryService.restock(
                transItem.getItem().getBarcode(),
                transItem.getQuantity(),
                employee // Pass the employee performing the return for permission check
            );

            if (!success) {
                // Log a warning if restocking failed (could be due to permissions or item not found)
                System.err.println("Warning: Could not restock item " + transItem.getItem().getBarcode() + " during return. " +
                                   "Reason: Could be missing item or insufficient permissions for employee " + employee.getEmployeeId() + ".");
                // Decide policy: Continue return or fail? Let's continue for now.
            }
        }

        // 4. Create RETURN Receipt
        // The Receipt constructor for RETURN takes the determined finalRefundAmount
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