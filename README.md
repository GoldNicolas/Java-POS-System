# POS System Project

Developers:
 - `Nicolas Guerra`
 - `Saud Manganhar`

## Folder Structure

The workspace contains two folders by default, where:

- `src`: the folder to maintain sources
- `lib`: the folder to maintain dependencies

Meanwhile, the compiled output files will be generated in the `bin` folder by default.

## Java Files Structure

```
POSSystem/
├── src/
│   └── com/
│       └── pos/
│           ├── main/
│           │   └── MainApp.java             # Entry point
│           ├── model/
│           │   ├── Employee.java          # Abstract Employee
│           │   ├── Manager.java           # Manager subclass
│           │   ├── Cashier.java           # Cashier subclass
│           │   ├── Item.java              # Product Item
│           │   ├── Inventory.java         # Manages Items
│           │   ├── Receipt.java           # Transaction Receipt
│           │   ├── TransactionItem.java   # Item details within a receipt
│           │   └── TransactionType.java   # Enum (PURCHASE, RETURN)
│           ├── service/
│           │   ├── AuthenticationService.java # Handles login
│           │   ├── TransactionService.java  # Handles sales and returns
│           │   └── InventoryService.java    # Wraps Inventory logic (optional but good practice)
│           └── ui/
│               ├── MainFrame.java         # Main application window (JFrame)
│               ├── LoginPanel.java        # Login screen (JPanel)
│               └── PosPanel.java          # Main POS interface (JPanel)
```

## Tech Stack

- Java SE 17
- Swing for GUI

## Features

1. **User Authentication**: Login system with different roles (Manager, Cashier).
2. **Inventory Management**: Add, remove, and update items in the inventory.
3. **Sales Transactions**: Process sales and returns with transaction receipts.
4. **UI**: User-friendly interface for easy navigation.

