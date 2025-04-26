package com.pos.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginPanel extends JPanel {

    private final MainFrame mainFrame; // Reference to parent frame
    private JTextField employeeIdField;
    private JButton loginButton;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new GridBagLayout()); // Use GridBagLayout for better centering
        setBackground(new Color(240, 240, 240)); // Light gray background

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Padding

        // --- Title ---
        JLabel titleLabel = new JLabel("POS System Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Span across two columns
        gbc.anchor = GridBagConstraints.CENTER;
        add(titleLabel, gbc);


        // --- Employee ID Label ---
        JLabel idLabel = new JLabel("Employee ID:");
        idLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1; // Reset span
        gbc.anchor = GridBagConstraints.LINE_END; // Align right
        add(idLabel, gbc);

        // --- Employee ID Text Field ---
        employeeIdField = new JTextField(15); // Set preferred width
         employeeIdField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START; // Align left
        add(employeeIdField, gbc);

         // --- Login Button ---
        loginButton = new JButton("Login");
         loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setBackground(new Color(70, 130, 180)); // Steel blue
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false); // Remove focus border
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // Span columns
        gbc.anchor = GridBagConstraints.CENTER; // Center button
        add(loginButton, gbc);


        // --- Action Listener for Login Button ---
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String employeeId = employeeIdField.getText().trim();
                if (!employeeId.isEmpty()) {
                    mainFrame.attemptLogin(employeeId);
                } else {
                    JOptionPane.showMessageDialog(LoginPanel.this,
                            "Please enter an Employee ID.",
                            "Input Required",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });

         // Allow login on pressing Enter in the text field
         employeeIdField.addActionListener(e -> loginButton.doClick());
    }

     // Method to clear the input field (e.g., after failed login)
    public void clearPassword() {
        employeeIdField.setText("");
        employeeIdField.requestFocusInWindow(); // Set focus back
    }
}