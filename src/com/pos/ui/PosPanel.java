package com.pos.ui;

import com.pos.model.*; // Import model classes
import com.pos.service.InventoryService;
import com.pos.service.TransactionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PosPanel extends JPanel {

    private final MainFrame mainFrame; // Reference to parent frame
    private JLabel employeeInfoLabel;
    private JTextField barcodeInput;
    private JButton addItemButton;
    private JButton completeSaleButton;
    private JButton startReturnButton;
    private JButton restockButton; // Manager only
    private JButton checkLowStockButton;
    private JButton logoutButton;
    private JTable currentSaleTable;
    private DefaultTableModel saleTableModel;
    private JLabel totalLabel;

    // This list holds items for the transaction currently being built (sale or return)
    private List<TransactionItem> currentTransactionItems;

    public PosPanel(MainFrame mainFrame) {
        if (mainFrame == null) {
            throw new IllegalArgumentException("MainFrame cannot be null");
        }
        this.mainFrame = mainFrame;
        this.currentTransactionItems = new ArrayList<>(); // Initialize the list
        setLayout(new BorderLayout(10, 10)); // Main layout with spacing
        setBorder(new EmptyBorder(10, 10, 10, 10)); // Padding around the panel

        // --- Top Panel (Employee Info & Logout) ---
        JPanel topPanel = new JPanel(new BorderLayout());
        employeeInfoLabel = new JLabel("Employee: Not Logged In");
        employeeInfoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> mainFrame.logout());
        logoutButton.setToolTipText("Log out the current user");
        topPanel.add(employeeInfoLabel, BorderLayout.WEST);
        topPanel.add(logoutButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);


        // --- Center Panel (Barcode Input & Sale Table) ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        // Barcode Input Area
        JPanel barcodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barcodePanel.add(new JLabel("Scan Barcode:"));
        barcodeInput = new JTextField(20);
        barcodeInput.setToolTipText("Enter item barcode here and press Enter or click 'Add Item'");
        addItemButton = new JButton("Add Item");
        addItemButton.setToolTipText("Add the item with the entered barcode to the current transaction");
        barcodePanel.add(barcodeInput);
        barcodePanel.add(addItemButton);
        centerPanel.add(barcodePanel, BorderLayout.NORTH);

        // Sale Table Area
        String[] columnNames = {"Barcode", "Name", "Qty", "Price", "Subtotal"};
        // Make table model non-editable by default
        saleTableModel = new DefaultTableModel(columnNames, 0) {
             @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Cells are not editable
            }
        };
        currentSaleTable = new JTable(saleTableModel);
        currentSaleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Allow selecting rows
        currentSaleTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        currentSaleTable.getTableHeader().setReorderingAllowed(false); // Prevent column reordering
        // Set preferred column widths
        currentSaleTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Barcode
        currentSaleTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Name
        currentSaleTable.getColumnModel().getColumn(2).setPreferredWidth(50);  // Qty
        currentSaleTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Price
        currentSaleTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Subtotal


        JScrollPane tableScrollPane = new JScrollPane(currentSaleTable);
        tableScrollPane.setBorder(new TitledBorder("Current Transaction Items"));
        centerPanel.add(tableScrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);


        // --- Right Panel (Actions & Total) ---
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS)); // Vertical alignment
        rightPanel.setBorder(new EmptyBorder(0, 10, 0, 0)); // Left padding

        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 18));
        totalLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center label horizontally

        // Create action buttons using helper method
        completeSaleButton = createActionButton("Complete Sale");
        completeSaleButton.setToolTipText("Finalize the current sale transaction");
        startReturnButton = createActionButton("Start Return");
        startReturnButton.setToolTipText("Initiate a return process using an original receipt ID");
        restockButton = createActionButton("Restock Item");
        restockButton.setToolTipText("Add stock for an existing item (Manager only)");
        checkLowStockButton = createActionButton("Check Low Stock");
        checkLowStockButton.setToolTipText("View items that are low on stock or out of stock");

        // Add components with spacing
        rightPanel.add(totalLabel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spacer
        rightPanel.add(completeSaleButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        rightPanel.add(startReturnButton);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        rightPanel.add(restockButton);
         rightPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacer
        rightPanel.add(checkLowStockButton);
        rightPanel.add(Box.createVerticalGlue()); // Pushes components up


        add(rightPanel, BorderLayout.EAST);

        // --- Action Listeners ---
        addItemButton.addActionListener(this::addItemAction);
        barcodeInput.addActionListener(this::addItemAction); // Add item on Enter key press in barcode field
        completeSaleButton.addActionListener(this::completeSaleAction);
        startReturnButton.addActionListener(this::startReturnAction);
        restockButton.addActionListener(this::restockAction);
        checkLowStockButton.addActionListener(this::checkLowStockAction);

        // Initialize panel state (mostly disabled until login)
        resetPanel();
    }

     /** Helper method to create styled action buttons */
     private JButton createActionButton(String text) {
         JButton button = new JButton(text);
         button.setFont(new Font("Arial", Font.PLAIN, 14));
         button.setAlignmentX(Component.CENTER_ALIGNMENT); // Center text/icon
         // Make buttons expand horizontally within the BoxLayout panel
         button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
         return button;
     }


    // --- UI Update Methods ---

    /** Updates the employee info label and enables/disables controls based on role */
    public void updateEmployeeInfo(Employee employee) {
        if (employee != null) {
            employeeInfoLabel.setText(String.format("Employee: %s (%s - %s)",
                    employee.getName(), employee.getEmployeeId(), employee.getRole()));

            // Enable core functionality
            barcodeInput.setEnabled(true);
            addItemButton.setEnabled(true);
            completeSaleButton.setEnabled(true);
            startReturnButton.setEnabled(true);
            checkLowStockButton.setEnabled(true);

            // Enable/Disable manager-specific functions
            boolean isManager = employee instanceof Manager;
            restockButton.setEnabled(isManager);

            // Set focus to barcode input for quick scanning
            barcodeInput.requestFocusInWindow();

        } else {
            resetPanel(); // Revert to logged-out state
        }
    }

    /** Resets the panel to its initial (logged-out or post-transaction) state */
    public void resetPanel() {
        employeeInfoLabel.setText("Employee: Not Logged In");
        barcodeInput.setText("");
        currentTransactionItems.clear(); // Clear the internal list
        saleTableModel.setRowCount(0); // Clear the visual table
        updateTotal(); // Reset total label

        // Disable buttons that require login or an active transaction
        barcodeInput.setEnabled(false);
        addItemButton.setEnabled(false);
        completeSaleButton.setEnabled(false);
        startReturnButton.setEnabled(false);
        restockButton.setEnabled(false);
        checkLowStockButton.setEnabled(false); // Can argue this could be enabled, but let's tie it to login
    }

    /** Recalculates and updates the total amount displayed */
    private void updateTotal() {
        double total = 0.0;
        for (TransactionItem item : currentTransactionItems) {
            total += item.getSubtotal();
        }
        totalLabel.setText(String.format("Total: $%.2f", total));
    }

    /** Adds an item to the visual table and the internal transaction list */
    private void addItemToTable(TransactionItem transItem) {
         // Check if item (by barcode) is already in the list to increment quantity
         for(int i = 0; i < currentTransactionItems.size(); i++) {
             TransactionItem existingTransItem = currentTransactionItems.get(i);
             if (existingTransItem.getItem().getBarcode().equals(transItem.getItem().getBarcode())) {
                 // Combine quantities
                 int newQuantity = existingTransItem.getQuantity() + transItem.getQuantity();

                 // Check stock ONLY if it's a main inventory item
                 boolean isInventoryItem = mainFrame.getInventoryService().isInventoryItem(transItem.getItem().getBarcode());
                 int availableStock = Integer.MAX_VALUE; // Assume infinite for temporary items

                 if (isInventoryItem) {
                      // Re-fetch from inventory to get current stock count just before adding
                      Optional<Item> inventoryItemOpt = mainFrame.getInventoryService().findItem(transItem.getItem().getBarcode());
                      availableStock = inventoryItemOpt.map(Item::getQuantityInStock).orElse(0); // Default to 0 if somehow gone
                 }

                 if (availableStock >= newQuantity) {
                     // Create a new TransactionItem reflecting the updated quantity
                     // Use the item reference from the *existing* transaction item to ensure consistency
                     TransactionItem updatedTransItem = new TransactionItem(existingTransItem.getItem(), newQuantity);
                     currentTransactionItems.set(i, updatedTransItem); // Replace existing item in the list

                     // Update the corresponding row in the table
                     saleTableModel.setValueAt(updatedTransItem.getQuantity(), i, 2); // Update Qty column
                     saleTableModel.setValueAt(String.format("%.2f", updatedTransItem.getSubtotal()), i, 4); // Update Subtotal column

                     updateTotal(); // Recalculate total
                     return; // Item updated, exit the method
                 } else {
                       // Not enough stock for the increased quantity (only applies to inventory-managed items)
                       JOptionPane.showMessageDialog(this,
                         "Not enough stock to add more '" + transItem.getItem().getName() + "'.\nRequested total: " + newQuantity + ", Available: " + availableStock,
                         "Stock Error", JOptionPane.WARNING_MESSAGE);
                     return; // Exit without adding/updating
                 }
             }
         }

         // If the loop finishes, the item wasn't already in the list, so add it as a new entry
         currentTransactionItems.add(transItem);
         saleTableModel.addRow(new Object[]{
                 transItem.getItem().getBarcode(),
                 transItem.getItem().getName(),
                 transItem.getQuantity(),
                 String.format("%.2f", transItem.getPriceAtTransaction()), // Format price
                 String.format("%.2f", transItem.getSubtotal())          // Format subtotal
         });
         updateTotal(); // Recalculate total
    }

    // --- Action Handlers ---

    /** Handles adding an item via barcode input (button click or Enter key) */
    private void addItemAction(ActionEvent e) {
        String barcode = barcodeInput.getText().trim();
         if (barcode.isEmpty()) {
             return;
         }

        Employee currentEmployee = mainFrame.getLoggedInEmployee();
        if (currentEmployee == null) {
             JOptionPane.showMessageDialog(this, "No employee logged in.", "Error", JOptionPane.ERROR_MESSAGE);
             mainFrame.logout(); // Force logout if state is inconsistent
            return;
        }

         InventoryService invService = mainFrame.getInventoryService();
         // Use the modified findItem which checks both inventory and temporary storage
         Optional<Item> itemOpt = invService.findItem(barcode);

         if (itemOpt.isPresent()) {
             // --- ITEM FOUND (EITHER IN INVENTORY OR TEMPORARILY) ---
             Item item = itemOpt.get();
             boolean isInventoryItem = invService.isInventoryItem(barcode); // Check if it's from main inventory

             if (isInventoryItem) {
                 // It's an inventory item, check stock
                 if (item.getQuantityInStock() > 0) {
                     // Add quantity 1 (addItemToTable handles increments if already present)
                     TransactionItem transItem = new TransactionItem(item, 1);
                     addItemToTable(transItem); // Add/Update item in the list/table
                 } else {
                      JOptionPane.showMessageDialog(this,
                         "Item '" + item.getName() + "' (ID: " + barcode + ") is out of stock.",
                         "Stock Error", JOptionPane.ERROR_MESSAGE);
                 }
             } else {
                 // It's a temporary item found in the session store, no stock check needed
                 System.out.println("Found previously added item: " + barcode);
                 TransactionItem transItem = new TransactionItem(item, 1);
                 addItemToTable(transItem); // Add/Update item in the list/table
             }
             // --- END OF ITEM FOUND LOGIC ---

         } else {
            // --- ITEM *NOT* FOUND ANYWHERE (INVENTORY OR TEMPORARY STORE) ---
            // Ask to add as a new temporary item for the session
            int choice = JOptionPane.showConfirmDialog(this,
                    "Item with barcode '" + barcode + "' not found in inventory.\nDo you want to add it to sale?",
                    "Add New Item?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                // --- Get Item Details from User ---
                String name = "";
                while (name.isEmpty()) {
                    name = JOptionPane.showInputDialog(this, "Enter name for new item (Barcode: " + barcode + "):");
                    if (name == null) { // User cancelled name input
                        barcodeInput.setText("");
                        barcodeInput.requestFocusInWindow();
                        return; // Cancelled adding temporary item
                    }
                    name = name.trim();
                    if (name.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "Item name cannot be empty.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    }
                }

                double price = -1.0;
                while (price < 0) { // Loop until valid price or cancellation
                    String priceStr = JOptionPane.showInputDialog(this, "Enter price for '" + name + "':");
                    if (priceStr == null) { // User cancelled price input
                        barcodeInput.setText("");
                        barcodeInput.requestFocusInWindow();
                        return; // Cancelled adding temporary item
                    }
                    try {
                        price = Double.parseDouble(priceStr.trim());
                        if (price < 0) {
                            JOptionPane.showMessageDialog(this, "Price cannot be negative. Please enter a valid price.", "Input Error", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(this, "Invalid price format. Please enter a number (e.g., 4.99).", "Input Error", JOptionPane.WARNING_MESSAGE);
                        price = -1.0; // Reset price to ensure loop continues
                    }
                }

                // --- Create Temporary Item and add to InventoryService's temporary store ---
                // Use quantity 0 for the Item itself, as stock isn't tracked.
                Item tempItem = new Item(barcode, name, price, 0);
                try {
                    invService.addTemporaryItem(tempItem); // <<<< STORE PERSISTENTLY FOR SESSION
                } catch (IllegalArgumentException ex) {
                    // Handle potential errors from addTemporaryItem, e.g., conflict with inventory barcode if that check was enabled
                    JOptionPane.showMessageDialog(this, "Error adding temporary item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    barcodeInput.setText("");
                    barcodeInput.requestFocusInWindow();
                    return;
                }


                // --- Create TransactionItem and Add to Current Sale Table ---
                TransactionItem transItem = new TransactionItem(tempItem, 1); // Add quantity 1 to the sale
                addItemToTable(transItem); // Add to the visual table/list

                JOptionPane.showMessageDialog(this, "Temporary item '" + name + "' added for this session.", "Item Added Temporarily", JOptionPane.INFORMATION_MESSAGE);

            } else {
                // User chose NO to adding the temporary item
                JOptionPane.showMessageDialog(this,
                   "Item with barcode '" + barcode + "' not found and was not added.",
                   "Lookup Error",
                   JOptionPane.ERROR_MESSAGE);
            }
             // --- END OF ITEM NOT FOUND LOGIC ---
         }

         // Clear input field and set focus back for next scan, regardless of outcome
         barcodeInput.setText("");
         barcodeInput.requestFocusInWindow();
    }


    /** Handles completing the current sale transaction */
    private void completeSaleAction(ActionEvent e) {
        Employee currentEmployee = mainFrame.getLoggedInEmployee();
         if (currentEmployee == null) {
              JOptionPane.showMessageDialog(this, "Error: No employee logged in.", "Error", JOptionPane.ERROR_MESSAGE);
              return;
         }

        if (currentTransactionItems.isEmpty()) {
             JOptionPane.showMessageDialog(this, "Cannot complete sale. No items have been added.", "Empty Sale", JOptionPane.WARNING_MESSAGE);
            return;
        }

         TransactionService transService = mainFrame.getTransactionService();
         try {
             // processSale logic in TransactionService already handles not decrementing stock
             // for items not found in main inventory (which now includes our temporary items)
             Receipt receipt = transService.processSale(new ArrayList<>(currentTransactionItems), currentEmployee);

             // Display success message with receipt details
             JTextArea receiptArea = new JTextArea(receipt.getFormattedReceipt());
             receiptArea.setEditable(false);
             receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
             JScrollPane scrollPane = new JScrollPane(receiptArea);
             scrollPane.setPreferredSize(new Dimension(450, 350));

             JOptionPane.showMessageDialog(this, scrollPane, "Sale Completed - Receipt ID: " + receipt.getReceiptId(), JOptionPane.INFORMATION_MESSAGE);

             // Clear the current sale state AFTER successful completion
             currentTransactionItems.clear();
             saleTableModel.setRowCount(0);
             updateTotal();
             barcodeInput.requestFocusInWindow(); // Ready for next transaction

         } catch (TransactionService.TransactionException ex) {
             JOptionPane.showMessageDialog(this, "Error processing sale: " + ex.getMessage(), "Sale Failed", JOptionPane.ERROR_MESSAGE);
         } catch (Exception ex) {
              JOptionPane.showMessageDialog(this, "An unexpected error occurred during sale processing:\n" + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
             ex.printStackTrace();
         }
    }

    /** Handles initiating the return process */
    private void startReturnAction(ActionEvent e) {
         Employee currentEmployee = mainFrame.getLoggedInEmployee();
         if (currentEmployee == null) {
             JOptionPane.showMessageDialog(this, "Error: No employee logged in.", "Error", JOptionPane.ERROR_MESSAGE);
             return;
         }

        String originalReceiptId = JOptionPane.showInputDialog(this, "Enter the original Receipt ID for the return:", "Start Return Process", JOptionPane.QUESTION_MESSAGE);
         if (originalReceiptId == null || originalReceiptId.trim().isEmpty()) {
             return;
         }
        originalReceiptId = originalReceiptId.trim().toUpperCase();

        List<TransactionItem> itemsToReturn = new ArrayList<>();
        InventoryService invService = mainFrame.getInventoryService();

        while (true) {
             String barcode = JOptionPane.showInputDialog(this, "Enter barcode of item to return (or leave blank to finish):", "Add Item to Return", JOptionPane.QUESTION_MESSAGE);
             if (barcode == null || barcode.trim().isEmpty()) {
                 break;
             }
             barcode = barcode.trim();

             // Use the updated findItem - can now find temporary items for return too
             Optional<Item> itemOpt = invService.findItem(barcode);
             if (!itemOpt.isPresent()) {
                 // If not found in inventory OR temporary store, cannot return.
                 JOptionPane.showMessageDialog(this, "Item with barcode '" + barcode + "' not found.\nCannot process return for unknown items.", "Return Error", JOptionPane.WARNING_MESSAGE);
                 continue;
             }
             Item item = itemOpt.get(); // Item could be from inventory OR temporary store

             String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity of '" + item.getName() + "' to return:", "Return Quantity", JOptionPane.QUESTION_MESSAGE);
             int quantity;
             try {
                 if (qtyStr == null) continue; // User cancelled quantity input
                 quantity = Integer.parseInt(qtyStr.trim());
                 if (quantity <= 0) throw new NumberFormatException();
             } catch (NumberFormatException ex) {
                  JOptionPane.showMessageDialog(this, "Invalid quantity. Please enter a positive whole number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                 continue;
             }

             // Capture the item details (including price) at the time of return lookup
             // Note: For more accuracy, a real system would look up the price from the *original* receipt.
            // Here, we use the current price (from inventory or temporary store).
            itemsToReturn.add(new TransactionItem(item, quantity));
            JOptionPane.showMessageDialog(this, quantity + " x '" + item.getName() + "' added to return list.", "Item Added", JOptionPane.INFORMATION_MESSAGE);
        }

        if (itemsToReturn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items were specified for return.", "Return Cancelled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Double customRefundAmount = null;
        // Calculate refund based on current prices (or prices stored in temporary items)
        double calculatedRefund = itemsToReturn.stream().mapToDouble(TransactionItem::getSubtotal).sum();

        if (currentEmployee instanceof Manager manager && manager.canDoFlexibleRefund()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    String.format("Standard calculated refund is $%.2f.\nSpecify a different amount?", calculatedRefund),
                    "Manager Refund Option",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                String customAmountStr = JOptionPane.showInputDialog(this, "Enter the total custom refund amount:", "Custom Refund Amount", JOptionPane.QUESTION_MESSAGE);
                try {
                    if (customAmountStr != null) {
                        customRefundAmount = Double.parseDouble(customAmountStr.trim());
                        if (customRefundAmount < 0) {
                            JOptionPane.showMessageDialog(this, "Refund amount cannot be negative. Using calculated.", "Input Error", JOptionPane.WARNING_MESSAGE);
                            customRefundAmount = null; // Revert to calculated
                        }
                    } // else user cancelled, customRefundAmount remains null
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid amount. Using calculated refund.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    customRefundAmount = null; // Revert to calculated
                }
            }
        }

        TransactionService transService = mainFrame.getTransactionService();
        try {
            // The processReturn method in TransactionService needs modification
            // to check if an item is temporary before attempting restock.
            Receipt returnReceipt = transService.processReturn(originalReceiptId, itemsToReturn, currentEmployee, customRefundAmount);

            JTextArea receiptArea = new JTextArea(returnReceipt.getFormattedReceipt());
            receiptArea.setEditable(false);
            receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(receiptArea);
            scrollPane.setPreferredSize(new Dimension(450, 350));

            JOptionPane.showMessageDialog(this, scrollPane, "Return Completed - Return Receipt ID: " + returnReceipt.getReceiptId(), JOptionPane.INFORMATION_MESSAGE);

         } catch (TransactionService.TransactionException ex) {
            JOptionPane.showMessageDialog(this, "Error processing return: " + ex.getMessage(), "Return Failed", JOptionPane.ERROR_MESSAGE);
         } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "An unexpected error occurred during return processing:\n" + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /** Handles restocking an item (Manager only) - Should only apply to inventory items */
    private void restockAction(ActionEvent e) {
        Employee currentEmployee = mainFrame.getLoggedInEmployee();
        if (!(currentEmployee instanceof Manager)) {
            JOptionPane.showMessageDialog(this, "Only Managers have permission to restock items.", "Permission Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String barcode = JOptionPane.showInputDialog(this, "Enter barcode of INVENTORY item to restock:", "Restock Item", JOptionPane.QUESTION_MESSAGE);
        if (barcode == null || barcode.trim().isEmpty()) return;
        barcode = barcode.trim();

        InventoryService invService = mainFrame.getInventoryService();
        // Check specifically if it's an inventory item before proceeding
        if (!invService.isInventoryItem(barcode)) {
            if (invService.findItem(barcode).isPresent()) { // Check if it's a known temporary item
                JOptionPane.showMessageDialog(this, "Item '" + barcode + "' is a temporary item.\nRestocking only applies to main inventory items.", "Restock Error", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Item '" + barcode + "' not found in main inventory.", "Restock Error", JOptionPane.WARNING_MESSAGE);
                // Optionally offer to add as *new inventory* item here if desired feature
            }
            return;
        }

        // If we reach here, it IS an inventory item. Proceed as before.
        Optional<Item> itemOpt = invService.findItem(barcode); // Will find the inventory item
        // This check is now redundant due to isInventoryItem check, but safe to keep
        if (!itemOpt.isPresent()) {
            JOptionPane.showMessageDialog(this, "Error: Item '"+barcode+"' not found despite passing inventory check.", "Internal Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Item itemToRestock = itemOpt.get();
        String currentStockInfo = "Current stock for '" + itemToRestock.getName() + "': " + itemToRestock.getQuantityInStock();

        String qtyStr = JOptionPane.showInputDialog(this, currentStockInfo + "\nEnter quantity to add:", "Restock Quantity", JOptionPane.QUESTION_MESSAGE);
        if (qtyStr == null || qtyStr.trim().isEmpty()) return;

        try {
            int quantityToAdd = Integer.parseInt(qtyStr.trim());
            if (quantityToAdd <= 0) {
                JOptionPane.showMessageDialog(this, "Restock quantity must be positive.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Use the restock method which only affects inventory items
            boolean success = invService.restock(barcode, quantityToAdd, currentEmployee);

            if (success) {
                Optional<Item> updatedItemOpt = invService.findItem(barcode); // Find again to show updated stock
                String successMsg = "Restocked " + quantityToAdd + " units of '" + itemToRestock.getName() + "'.";
                if(updatedItemOpt.isPresent()){ // Should always be present here
                    successMsg += "\nNew stock level: " + updatedItemOpt.get().getQuantityInStock();
                }
                JOptionPane.showMessageDialog(this, successMsg, "Restock Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Failure reason logged by restock method (permission or item vanished)
                JOptionPane.showMessageDialog(this, "Failed to restock item " + barcode + ". Check logs or permissions.", "Restock Failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity entered.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "An unexpected error during restock:\n" + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /** Handles checking and displaying low/out-of-stock items (from main inventory) */
    private void checkLowStockAction(ActionEvent e) {
        InventoryService invService = mainFrame.getInventoryService();
        // These methods only query the main inventory
        List<Item> lowStock = invService.getLowStockItems();
        List<Item> outOfStock = invService.getOutOfStockItems();

        StringBuilder message = new StringBuilder("--- Main Inventory Stock Levels ---\n\n");
        int lowStockThreshold = invService.getLowStockThreshold();

        if (!lowStock.isEmpty()) {
            message.append(String.format("** LOW STOCK ITEMS (<= %d Units) **\n", lowStockThreshold));
            lowStock.forEach(item -> message.append(String.format("  - %s (%s): %d left\n",
                    item.getName(), item.getBarcode(), item.getQuantityInStock())));
        } else {
            message.append("** No items currently low on stock in main inventory. **\n");
        }

        message.append("\n------------------------------------\n\n");

          if (!outOfStock.isEmpty()) {
            message.append("** OUT OF STOCK ITEMS (Main Inventory) **\n");
            outOfStock.forEach(item -> message.append(String.format("  - %s (%s)\n",
                    item.getName(), item.getBarcode())));
        } else {
            message.append("** No items currently out of stock in main inventory. **\n");
        }

        // Note: We are not displaying the list of temporary items here, but could be added if needed.

        JTextArea textArea = new JTextArea(message.toString());
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 350));

        JOptionPane.showMessageDialog(this, scrollPane, "Low Stock Report (Main Inventory)", JOptionPane.INFORMATION_MESSAGE);
    }
}