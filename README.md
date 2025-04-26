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
## Github

## Contact