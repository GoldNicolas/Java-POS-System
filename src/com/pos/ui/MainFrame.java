package com.pos.ui;

import com.pos.model.Employee;
import com.pos.service.AuthenticationService;
import com.pos.service.InventoryService;
import com.pos.service.TransactionService;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private final LoginPanel loginPanel;
    private final PosPanel posPanel;

    private final AuthenticationService authService;
    private final InventoryService inventoryService;
    private final TransactionService transactionService;

    private Employee loggedInEmployee = null;

    public MainFrame(AuthenticationService authService, InventoryService inventoryService, TransactionService transactionService) {
        this.authService = authService;
        this.inventoryService = inventoryService;
        this.transactionService = transactionService;

        setTitle("Simple POS System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center window

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        loginPanel = new LoginPanel(this); // Pass reference to MainFrame
        posPanel = new PosPanel(this);     // Pass reference to MainFrame

        mainPanel.add(loginPanel, "LoginPanel");
        mainPanel.add(posPanel, "PosPanel");

        add(mainPanel);

        // Start with login panel
        cardLayout.show(mainPanel, "LoginPanel");
    }

    public void attemptLogin(String employeeId) {
        loggedInEmployee = authService.login(employeeId).orElse(null);

        if (loggedInEmployee != null) {
            System.out.println("Login successful: " + loggedInEmployee.getName());
            posPanel.updateEmployeeInfo(loggedInEmployee); // Update PosPanel UI
            cardLayout.show(mainPanel, "PosPanel"); // Switch view
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid Employee ID.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            loginPanel.clearPassword(); // Clear field on failure
        }
    }

    public void logout() {
        System.out.println("Logging out: " + (loggedInEmployee != null ? loggedInEmployee.getName() : "Unknown"));
        loggedInEmployee = null;
        posPanel.resetPanel(); // Clear PosPanel state
        cardLayout.show(mainPanel, "LoginPanel"); // Go back to login
    }

    // --- Getters for Services and State ---
    public InventoryService getInventoryService() {
        return inventoryService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public Employee getLoggedInEmployee() {
        return loggedInEmployee;
    }
}