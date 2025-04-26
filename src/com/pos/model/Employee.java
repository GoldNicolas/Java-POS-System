package com.pos.model;

import java.util.Objects;

public abstract class Employee {
    protected String employeeId;
    protected String name;
    protected double workedHours;

    public Employee(String employeeId, String name) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee ID cannot be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name cannot be empty.");
        }
        this.employeeId = employeeId;
        this.name = name;
        this.workedHours = 0.0;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getName() {
        return name;
    }

    public double getWorkedHours() {
        return workedHours;
    }

    public void addWorkedHours(double hours) {
        if (hours > 0) {
            this.workedHours += hours;
        }
    }

    // Abstract method to be implemented by subclasses
    public abstract String getRole();

    // Optional: Override equals and hashCode for comparisons/use in collections
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return Objects.equals(employeeId, employee.employeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId);
    }

    @Override
    public String toString() {
        return "ID: " + employeeId + ", Name: " + name + ", Role: " + getRole();
    }
}