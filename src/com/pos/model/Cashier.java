package com.pos.model;

public class Cashier extends Employee {

    public Cashier(String employeeId, String name) {
        super(employeeId, name);
    }

    @Override
    public String getRole() {
        return "Cashier";
    }

    // --- Permissions ---
     public boolean canRestock() {
        return false; // Cashiers typically cannot restock
    }

    public boolean canDoFlexibleRefund() {
        return false; // Cashiers do standard refunds
    }
}