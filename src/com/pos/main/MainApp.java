package com.pos.main;

import com.pos.model.Inventory;
import com.pos.service.AuthenticationService;
import com.pos.service.InventoryService;
import com.pos.service.TransactionService;
import com.pos.ui.MainFrame;

import javax.swing.*;

public class MainApp {

    public static void main(String[] args) {
        // Set Look and Feel (Optional, makes it look slightly more modern)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Couldn't set system look and feel.");
        }

        // 1. Initialize Core Components
        Inventory inventory = new Inventory(); // The actual data store

        AuthenticationService authService = new AuthenticationService(); // Handles logins
        
        InventoryService inventoryService = new InventoryService(inventory); // Manages inventory operations + sample data
        
        TransactionService transactionService = new TransactionService(inventoryService); // Manages sales/returns

        // 2. Load initial data (Sample Data)
        inventoryService.initializeInventory(); // Add sample items via the service


        // 3. Create and Show GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(authService, inventoryService, transactionService);
            mainFrame.setVisible(true);
        });

        System.out.println("POS System Initialized.");
    }
}