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
// No need to import Collectors explicitly if only used within stream()

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

                 // Check stock for the *total combined* quantity needed
                 Optional<Item> inventoryItemOpt = mainFrame.getInventoryService().findItem(transItem.getItem().getBarcode());
                 int availableStock = inventoryItemOpt.map(Item::getQuantityInStock).orElse(0);

                 if (availableStock >= newQuantity) {
                     // Create a new TransactionItem reflecting the updated quantity
                     TransactionItem updatedTransItem = new TransactionItem(existingTransItem.getItem(), newQuantity);
                     currentTransactionItems.set(i, updatedTransItem); // Replace existing item in the list

                     // Update the corresponding row in the table
                     saleTableModel.setValueAt(updatedTransItem.getQuantity(), i, 2); // Update Qty column
                     saleTableModel.setValueAt(String.format("%.2f", updatedTransItem.getSubtotal()), i, 4); // Update Subtotal column

                     updateTotal(); // Recalculate total
                     return; // Item updated, exit the method
                 } else {
                       // Not enough stock for the increased quantity
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
             // Optionally provide feedback, or just ignore empty input
             // JOptionPane.showMessageDialog(this, "Please enter a barcode.", "Input Missing", JOptionPane.WARNING_MESSAGE);
             return;
         }

        Employee currentEmployee = mainFrame.getLoggedInEmployee();
        if (currentEmployee == null) {
             JOptionPane.showMessageDialog(this, "No employee logged in.", "Error", JOptionPane.ERROR_MESSAGE);
             mainFrame.logout(); // Force logout if state is inconsistent
            return;
        }

         InventoryService invService = mainFrame.getInventoryService();
         Optional<Item> itemOpt = invService.findItem(barcode);

         if (itemOpt.isPresent()) {
             Item item = itemOpt.get();
             if (item.getQuantityInStock() > 0) {
                 // For simplicity in this version, add quantity 1 directly.
                 // Enhancement: Ask for quantity via JOptionPane as before if needed.
                 int quantity = 1;

                 // Check if enough stock exists for the requested quantity (which is 1 here)
                 // Also consider items already added to the current transaction table
                 int quantityAlreadyInCart = 0;
                 for (TransactionItem cartItem : currentTransactionItems) {
                     if (cartItem.getItem().getBarcode().equals(item.getBarcode())) {
                         quantityAlreadyInCart = cartItem.getQuantity();
                         break;
                     }
                 }

                 if (item.getQuantityInStock() >= (quantityAlreadyInCart + quantity)) {
                     TransactionItem transItem = new TransactionItem(item, quantity);
                     addItemToTable(transItem); // Add/Update item in the list/table
                 } else {
                      JOptionPane.showMessageDialog(this,
                         "Insufficient stock for '" + item.getName() + "'. Available: " + item.getQuantityInStock() +
                         (quantityAlreadyInCart > 0 ? " (Already " + quantityAlreadyInCart + " in cart)" : ""),
                         "Stock Error", JOptionPane.WARNING_MESSAGE);
                 }

             } else {
                  JOptionPane.showMessageDialog(this,
                     "Item '" + item.getName() + "' (ID: " + barcode + ") is out of stock.",
                     "Stock Error", JOptionPane.ERROR_MESSAGE);
             }
         } else {
              JOptionPane.showMessageDialog(this,
                 "Item with barcode '" + barcode + "' not found.",
                 "Lookup Error", JOptionPane.ERROR_MESSAGE);
         }
         // Clear input field and set focus back for next scan
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
             // Pass a copy of the list to prevent modification issues
             Receipt receipt = transService.processSale(new ArrayList<>(currentTransactionItems), currentEmployee);

             // Display success message with receipt details in a scrollable text area
             JTextArea receiptArea = new JTextArea(receipt.getFormattedReceipt());
             receiptArea.setEditable(false);
             receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Use monospaced font for alignment
             JScrollPane scrollPane = new JScrollPane(receiptArea);
             scrollPane.setPreferredSize(new Dimension(450, 350)); // Adjust size

             JOptionPane.showMessageDialog(this, scrollPane, "Sale Completed - Receipt ID: " + receipt.getReceiptId(), JOptionPane.INFORMATION_MESSAGE);

             // Clear the current sale state after successful completion
             currentTransactionItems.clear();
             saleTableModel.setRowCount(0);
             updateTotal();
             barcodeInput.requestFocusInWindow(); // Ready for next transaction

         } catch (TransactionService.TransactionException ex) {
             // Show specific error message from the transaction service
             JOptionPane.showMessageDialog(this, "Error processing sale: " + ex.getMessage(), "Sale Failed", JOptionPane.ERROR_MESSAGE);
             // Note: TransactionService ideally handles its own rollback logic if needed.
         } catch (Exception ex) {
             // Catch unexpected errors
              JOptionPane.showMessageDialog(this, "An unexpected error occurred during sale processing:\n" + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
             ex.printStackTrace(); // Log stack trace for debugging
         }
    }

    /** Handles initiating the return process */
    private void startReturnAction(ActionEvent e) {
         Employee currentEmployee = mainFrame.getLoggedInEmployee();
         if (currentEmployee == null) {
             JOptionPane.showMessageDialog(this, "Error: No employee logged in.", "Error", JOptionPane.ERROR_MESSAGE);
             return;
         }

         // Prompt for the original receipt ID
        String originalReceiptId = JOptionPane.showInputDialog(this, "Enter the original Receipt ID for the return:", "Start Return Process", JOptionPane.QUESTION_MESSAGE);
         if (originalReceiptId == null || originalReceiptId.trim().isEmpty()) {
             return; // User cancelled or entered nothing
         }
        originalReceiptId = originalReceiptId.trim().toUpperCase(); // Standardize format

        // --- Feature Enhancement Idea: Look up original receipt ---
        // TransactionService transService = mainFrame.getTransactionService();
        // Optional<Receipt> originalReceiptOpt = transService.findReceiptById(originalReceiptId);
        // if (!originalReceiptOpt.isPresent()) {
        //     JOptionPane.showMessageDialog(this, "Original Receipt ID '" + originalReceiptId + "' not found in recent transactions.", "Return Error", JOptionPane.ERROR_MESSAGE);
        //     return;
        // }
        // Receipt originalReceipt = originalReceiptOpt.get();
        // Display original items and let user select items/quantities to return
        // --- End Enhancement Idea ---


        // --- Simplified Return: Ask for items by barcode manually ---
        // This approach doesn't verify against the original receipt but allows returns.
        List<TransactionItem> itemsToReturn = new ArrayList<>();
        InventoryService invService = mainFrame.getInventoryService();

        while (true) {
             String barcode = JOptionPane.showInputDialog(this, "Enter barcode of item to return (or leave blank to finish):", "Add Item to Return", JOptionPane.QUESTION_MESSAGE);
             if (barcode == null || barcode.trim().isEmpty()) {
                 break; // Finished adding items
             }
             barcode = barcode.trim();

             Optional<Item> itemOpt = invService.findItem(barcode);
             if (!itemOpt.isPresent()) {
                 JOptionPane.showMessageDialog(this, "Item with barcode '" + barcode + "' not found in inventory.", "Lookup Error", JOptionPane.WARNING_MESSAGE);
                 continue; // Ask for next barcode
             }
             Item item = itemOpt.get(); // Item exists in current inventory

             // Ask for quantity to return
             String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity of '" + item.getName() + "' to return:", "Return Quantity", JOptionPane.QUESTION_MESSAGE);
             int quantity;
             try {
                 if (qtyStr == null) continue; // User cancelled quantity input
                 quantity = Integer.parseInt(qtyStr.trim());
                 if (quantity <= 0) throw new NumberFormatException();
                 // Enhancement: Check if quantity exceeds originally purchased quantity (if original receipt is loaded)
             } catch (NumberFormatException ex) {
                  JOptionPane.showMessageDialog(this, "Invalid quantity. Please enter a positive whole number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                 continue; // Ask for barcode again
             }

             // Create a TransactionItem for the return.
             // IMPORTANT: This uses the *current* price from inventory. A better system
             // would fetch the price from the *original* transaction if possible.
             itemsToReturn.add(new TransactionItem(item, quantity));
             JOptionPane.showMessageDialog(this, quantity + " x '" + item.getName() + "' added to return list.", "Item Added", JOptionPane.INFORMATION_MESSAGE);
        } // End of while loop for adding items

        if (itemsToReturn.isEmpty()) {
             JOptionPane.showMessageDialog(this, "No items were specified for return.", "Return Cancelled", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

         // --- Determine Refund Amount ---
        Double customRefundAmount = null; // Assume standard refund initially
        double calculatedRefund = itemsToReturn.stream().mapToDouble(TransactionItem::getSubtotal).sum();

         // Check if the logged-in employee is a Manager and has flexible refund permissions
         if (currentEmployee instanceof Manager && ((Manager) currentEmployee).canDoFlexibleRefund()) {
             // Ask manager if they want to override the calculated refund
             int choice = JOptionPane.showConfirmDialog(this,
                     String.format("The standard calculated refund is $%.2f.\nDo you want to specify a different refund amount?", calculatedRefund),
                     "Manager Refund Option",
                     JOptionPane.YES_NO_OPTION,
                     JOptionPane.QUESTION_MESSAGE);

             if (choice == JOptionPane.YES_OPTION) {
                 String customAmountStr = JOptionPane.showInputDialog(this, "Enter the total custom refund amount:", "Custom Refund Amount", JOptionPane.QUESTION_MESSAGE);
                 try {
                     if (customAmountStr != null) {
                         customRefundAmount = Double.parseDouble(customAmountStr.trim());
                         if (customRefundAmount < 0) {
                             JOptionPane.showMessageDialog(this, "Refund amount cannot be negative. Using calculated amount.", "Input Error", JOptionPane.WARNING_MESSAGE);
                             customRefundAmount = null; // Revert to standard if negative
                         }
                     } else {
                          // Manager cancelled custom input, revert to standard
                         customRefundAmount = null;
                     }
                 } catch (NumberFormatException ex) {
                     JOptionPane.showMessageDialog(this, "Invalid amount entered. Using calculated refund amount.", "Input Error", JOptionPane.WARNING_MESSAGE);
                     customRefundAmount = null; // Revert to standard if invalid format
                 }
             }
             // If manager chose NO, customRefundAmount remains null, and standard calculation will be used.
         }


         // --- Process the Return via the Transaction Service ---
         TransactionService transService = mainFrame.getTransactionService();
         try {
             Receipt returnReceipt = transService.processReturn(originalReceiptId, itemsToReturn, currentEmployee, customRefundAmount);

             // Display success message with the return receipt details
             JTextArea receiptArea = new JTextArea(returnReceipt.getFormattedReceipt());
             receiptArea.setEditable(false);
             receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
             JScrollPane scrollPane = new JScrollPane(receiptArea);
             scrollPane.setPreferredSize(new Dimension(450, 350)); // Adjust size

             JOptionPane.showMessageDialog(this, scrollPane, "Return Completed - Return Receipt ID: " + returnReceipt.getReceiptId(), JOptionPane.INFORMATION_MESSAGE);

             // Note: We don't clear the main transaction table here as returns are separate actions

         } catch (TransactionService.TransactionException ex) {
              JOptionPane.showMessageDialog(this, "Error processing return: " + ex.getMessage(), "Return Failed", JOptionPane.ERROR_MESSAGE);
         } catch (Exception ex) {
              // Catch unexpected errors
              JOptionPane.showMessageDialog(this, "An unexpected error occurred during return processing:\n" + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
             ex.printStackTrace(); // Log stack trace
         }
    }

    /** Handles restocking an item (Manager only) */
    private void restockAction(ActionEvent e) {
         Employee currentEmployee = mainFrame.getLoggedInEmployee();
         // Redundant check as button should be disabled, but good practice
         if (!(currentEmployee instanceof Manager)) {
             JOptionPane.showMessageDialog(this, "Only Managers have permission to restock items.", "Permission Denied", JOptionPane.ERROR_MESSAGE);
             return;
         }

         // Get barcode from user
         String barcode = JOptionPane.showInputDialog(this, "Enter barcode of item to restock:", "Restock Item", JOptionPane.QUESTION_MESSAGE);
          if (barcode == null || barcode.trim().isEmpty()) return; // User cancelled or entered nothing
         barcode = barcode.trim();

        InventoryService invService = mainFrame.getInventoryService();
        Optional<Item> itemOpt = invService.findItem(barcode);

        if (!itemOpt.isPresent()) {
             // Ask if Manager wants to add this as a completely new item
              int createNew = JOptionPane.showConfirmDialog(this,
                  "Item with barcode '"+barcode+"' not found in inventory.\nDo you want to add it as a new item?",
                  "Item Not Found",
                  JOptionPane.YES_NO_OPTION);

              if (createNew == JOptionPane.YES_OPTION) {
                  // This would involve asking for Name, Price, and Initial Quantity
                  // Then calling a new method like inventoryService.addNewItem(barcode, name, price, quantity, currentEmployee);
                  JOptionPane.showMessageDialog(this, "Add new item functionality is not yet implemented.", "Feature Not Available", JOptionPane.INFORMATION_MESSAGE);
              }
              return; // Don't proceed with restock if item doesn't exist and not creating new
        }

        // Item exists, proceed with restocking
        Item itemToRestock = itemOpt.get();
        String currentStockInfo = "Current stock for '" + itemToRestock.getName() + "' (ID: " + barcode + "): " + itemToRestock.getQuantityInStock();

        // Get quantity to add from user
        String qtyStr = JOptionPane.showInputDialog(this, currentStockInfo + "\nEnter quantity to add:", "Restock Quantity", JOptionPane.QUESTION_MESSAGE);
        if (qtyStr == null || qtyStr.trim().isEmpty()) return; // User cancelled or entered nothing

        try {
            int quantityToAdd = Integer.parseInt(qtyStr.trim());
            if (quantityToAdd <= 0) {
                 JOptionPane.showMessageDialog(this, "Restock quantity must be a positive number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Call the inventory service to perform the restock
            boolean success = invService.restock(barcode, quantityToAdd, currentEmployee);

            if (success) {
                // Get updated item info to show new stock level
                Optional<Item> updatedItemOpt = invService.findItem(barcode);
                String successMsg = "Successfully restocked " + quantityToAdd + " units of '" + itemToRestock.getName() + "'.";
                if(updatedItemOpt.isPresent()){
                    successMsg += "\nNew stock level: " + updatedItemOpt.get().getQuantityInStock();
                }
                JOptionPane.showMessageDialog(this, successMsg, "Restock Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                 // Service layer might print details, show generic error here
                 // The service already logs permission errors. This might be item not found again (race condition?) or other issues.
                 JOptionPane.showMessageDialog(this, "Failed to restock item " + barcode + ". Check logs/console.", "Restock Failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
             JOptionPane.showMessageDialog(this, "Invalid quantity entered. Please enter a whole number.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
             // Catch unexpected errors
             JOptionPane.showMessageDialog(this, "An unexpected error occurred during restock:\n" + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /** Handles checking and displaying low/out-of-stock items */
    private void checkLowStockAction(ActionEvent e) {
        InventoryService invService = mainFrame.getInventoryService();
        List<Item> lowStock = invService.getLowStockItems();
        List<Item> outOfStock = invService.getOutOfStockItems();

        StringBuilder message = new StringBuilder("--- Inventory Stock Levels ---\n\n");

        int lowStockThreshold = invService.getLowStockThreshold(); // Get threshold from service

         if (!lowStock.isEmpty()) {
             // *** CORRECTED to use getter for threshold ***
             message.append(String.format("** LOW STOCK ITEMS (<= %d Units) **\n", lowStockThreshold));
             lowStock.forEach(item -> message.append(String.format("  - %s (%s): %d left\n",
                     item.getName(), item.getBarcode(), item.getQuantityInStock())));
         } else {
             message.append("** No items currently low on stock. **\n");
         }

         message.append("\n------------------------------------\n\n"); // Separator

          if (!outOfStock.isEmpty()) {
             message.append("** OUT OF STOCK ITEMS **\n");
             outOfStock.forEach(item -> message.append(String.format("  - %s (%s)\n",
                     item.getName(), item.getBarcode())));
         } else {
             message.append("** No items currently out of stock. **\n");
         }

         // *** CORRECTED variable name from C to textArea ***
         JTextArea textArea = new JTextArea(message.toString());
         textArea.setEditable(false);
         textArea.setWrapStyleWord(true); // Wrap longer lines
         textArea.setLineWrap(true);
         textArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Good for aligned text

         JScrollPane scrollPane = new JScrollPane(textArea); // Use the correct variable name
         scrollPane.setPreferredSize(new Dimension(450, 350)); // Adjust size as needed

         JOptionPane.showMessageDialog(this, scrollPane, "Low Stock Report", JOptionPane.INFORMATION_MESSAGE);
    }
}