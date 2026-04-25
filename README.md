# ☕ Specialized Coffee Roastery and Cafe Management System

## Project Description
This project is a simple Java-based management system for a specialized coffee roastery and café.  
It is developed as part of the **CSE215 Lab project** to demonstrate object-oriented programming concepts using Java.

The system helps a café owner to:
- Manage coffee products
- Take customer orders
- Calculate bills
- Apply discounts for loyal customers
- Generate invoices
- Store invoice data in a text file

The user interface is built using **Java Swing**.

---

## Features
- Add customer name and order quantity
- Select coffee type (Coffee Bean or Brewed Coffee)
- Automatic bill calculation
- Discount for loyal customers
- Custom exception handling for invalid orders
- Invoice saved in a text file
- Simple and user-friendly GUI

---

## Object Oriented Concepts Used
- **Encapsulation**: Used in classes like `Customer`
- **Inheritance**: `CoffeeBean` and `BrewedCoffee` inherit from `Product`
- **Abstraction**:
  - Abstract class: `Product`
  - Interface: `Discount`
- **Method Overriding**: Different price calculation for different coffee types
- **Custom Exception**: `InvalidOrderException`

---

## Technologies Used
- Java
- Java Swing (GUI)
- File Handling (Text File)

---

## File Information
- **Source Code File:** `CoffeeCafe.java`
- **Invoice Storage File:** `invoice.txt` (created automatically after running)

---

## How to Run the Project
1. Open the project in a Java IDE (NetBeans / IntelliJ / Eclipse)
2. Make sure the file name is `CoffeeCafe.java`
3. Run the program on a **desktop or laptop**
4. The GUI window will open
5. Enter customer details and generate invoice

⚠️ Note:  
This project uses **Swing GUI**, so it will not run in a headless environment (like mobile or online compilers).

---

## Limitations
- No database is used (only text file storage)
- Loyalty points are fixed for demonstration
- Basic UI design

---

## Conclusion
This project successfully demonstrates core Java OOP concepts, GUI development using Swing, exception handling, and file handling.  
It is suitable for small café management and fulfills all the requirements of the CSE215 Lab project.

---

## Group Members
- Member 1 (Azman): OOP Design and Logic
- Member 2 (Oli): GUI Development
- Member 3 (Moshiour): File Handling and Report Writing
