package com.pos.model;

public class Manager extends Employee {

    public Manager(String employeeId, String name) {
        super(employeeId, name);
    }

    @Override
    public String getRole() {
        return "Manager";
    }

    // --- Permissions ---
    public boolean canRestock() {
        return true;
    }

    public boolean canDoFlexibleRefund() {
        return true;
    }
}