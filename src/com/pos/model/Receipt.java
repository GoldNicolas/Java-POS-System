package com.pos.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID; // For unique IDs

public class Receipt {
    private final String receiptId;
    private final LocalDateTime timestamp;
    private final List<TransactionItem> items;
    private double totalAmount; // Calculated total
    private final Employee processedBy;
    private final TransactionType type;
    private final String originalReceiptId; // For RETURN transactions

    // Constructor for PURCHASE
    public Receipt(List<TransactionItem> items, Employee processedBy) {
        this(items, processedBy, TransactionType.PURCHASE, null);
    }

    // Constructor for RETURN
    public Receipt(List<TransactionItem> returnedItems, Employee processedBy, String originalReceiptId, double refundAmount) {
         this(returnedItems, processedBy, TransactionType.RETURN, originalReceiptId);
         // For returns, the totalAmount is typically negative (representing refund)
         // The refundAmount might be custom (manager) or calculated (cashier)
         this.totalAmount = -Math.abs(refundAmount); // Ensure it's negative
    }


    // Private common constructor
    private Receipt(List<TransactionItem> items, Employee processedBy, TransactionType type, String originalReceiptId) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Receipt must contain at least one item.");
        }
        if (processedBy == null) {
            throw new IllegalArgumentException("Processed by employee cannot be null.");
        }
         if (type == TransactionType.RETURN && (originalReceiptId == null || originalReceiptId.trim().isEmpty()) ) {
            throw new IllegalArgumentException("Original Receipt ID is required for returns.");
        }

        this.receiptId = UUID.randomUUID().toString().substring(0, 8).toUpperCase(); // Short unique ID
        this.timestamp = LocalDateTime.now();
        this.items = new ArrayList<>(items); // Copy the list
        this.processedBy = processedBy;
        this.type = type;
        this.originalReceiptId = originalReceiptId;

        // Calculate total only if it's a PURCHASE (Return total is set explicitly)
        if (type == TransactionType.PURCHASE) {
            calculateTotal();
        }
    }


    private void calculateTotal() {
        this.totalAmount = 0.0;
        for (TransactionItem item : items) {
            this.totalAmount += item.getSubtotal();
        }
    }

    // --- Getters ---
    public String getReceiptId() { return receiptId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<TransactionItem> getItems() { return Collections.unmodifiableList(items); } // Return immutable view
    public double getTotalAmount() { return totalAmount; }
    public Employee getProcessedBy() { return processedBy; }
    public TransactionType getType() { return type; }
    public String getOriginalReceiptId() { return originalReceiptId; }


    public String getFormattedReceipt() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        sb.append("========================================\n");
        sb.append("          ").append(type).append(" RECEIPT\n");
        sb.append("========================================\n");
        sb.append("Receipt ID: ").append(receiptId).append("\n");
        sb.append("Timestamp:  ").append(timestamp.format(formatter)).append("\n");
        sb.append("Processed By: ").append(processedBy.getName()).append(" (").append(processedBy.getEmployeeId()).append(")\n");
        if (type == TransactionType.RETURN && originalReceiptId != null) {
             sb.append("Original Purchase ID: ").append(originalReceiptId).append("\n");
        }
        sb.append("----------------------------------------\n");
        sb.append("Items:\n");
        for (TransactionItem item : items) {
            sb.append("- ").append(item.toString()).append("\n");
        }
        sb.append("----------------------------------------\n");
        if (type == TransactionType.PURCHASE) {
             sb.append(String.format("TOTAL AMOUNT: $%.2f\n", totalAmount));
        } else { // RETURN
             sb.append(String.format("TOTAL REFUND: $%.2f\n", Math.abs(totalAmount))); // Display as positive refund
        }
        sb.append("========================================\n");

        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedReceipt();
    }
}