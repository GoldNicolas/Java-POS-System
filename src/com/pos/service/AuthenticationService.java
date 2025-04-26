package com.pos.service;

import com.pos.model.Cashier;
import com.pos.model.Employee;
import com.pos.model.Manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AuthenticationService {
    // In a real app, this would connect to a database or secure storage
    private final Map<String, Employee> employeeDatabase;

    public AuthenticationService() {
        employeeDatabase = new HashMap<>();
        // Add some sample employees
        employeeDatabase.put("MGR001", new Manager("MGR001", "Alice Manager"));
        employeeDatabase.put("CSH001", new Cashier("CSH001", "Bob Cashier"));
        employeeDatabase.put("CSH002", new Cashier("CSH002", "Charlie Cashier"));
    }

    public Optional<Employee> login(String employeeId) {
        return Optional.ofNullable(employeeDatabase.get(employeeId));
    }

     // Method to add employees dynamically if needed (e.g., for testing or admin features)
     public void addEmployee(Employee employee) {
         if (employee != null && !employeeDatabase.containsKey(employee.getEmployeeId())) {
             employeeDatabase.put(employee.getEmployeeId(), employee);
         }
     }
}