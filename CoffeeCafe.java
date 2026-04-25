import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

// --------------------
// Custom Exception
// --------------------
class InvalidOrderException extends Exception {
    public InvalidOrderException(String msg) {
        super(msg);
    }
}

// --------------------
// Interface
// --------------------
interface Discount {
    double giveDiscount(double amount);
}

// --------------------
// Super class
// --------------------
abstract class Product {
    String name;
    double price;

    abstract double getTotalPrice(int qty);
}

// --------------------
// Sub class 1
// --------------------
class CoffeeBean extends Product {

    CoffeeBean() {
        name = "Coffee Bean";
        price = 500;
    }

    double getTotalPrice(int qty) {
        return price * qty;
    }
}

// --------------------
// Sub class 2
// --------------------
class BrewedCoffee extends Product {

    BrewedCoffee() {
        name = "Brewed Coffee";
        price = 300;
    }

    double getTotalPrice(int qty) {
        return (price * qty) + 50; // brewing charge
    }
}

// --------------------
// Sub class 3
// --------------------
class Customer {
    String name;
    int loyaltyPoints;

    Customer(String name, int points) {
        this.name = name;
        this.loyaltyPoints = points;
    }
}

// --------------------
// Invoice class
// --------------------
class Invoice implements Discount {

    Customer customer;

    Invoice(Customer customer) {
        this.customer = customer;
    }

    public double giveDiscount(double amount) {
        if (customer.loyaltyPoints >= 100) {
            return amount - (amount * 0.10);
        }
        return amount;
    }
}

// --------------------
// MAIN CLASS
// --------------------
public class CoffeeCafe extends JFrame {

    JTextField txtName, txtQty;
    JComboBox<String> combo;
    JLabel lblResult;

    CoffeeCafe() {

        setTitle("Specialized Coffee Roastery & Cafe");
        setSize(400, 280);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(6, 2, 10, 10));
        setLocationRelativeTo(null);

        add(new JLabel("Customer Name:"));
        txtName = new JTextField();
        add(txtName);

        add(new JLabel("Coffee Type:"));
        combo = new JComboBox<>();
        combo.addItem("Coffee Bean");
        combo.addItem("Brewed Coffee");
        add(combo);

        add(new JLabel("Quantity:"));
        txtQty = new JTextField();
        add(txtQty);

        JButton btn = new JButton("Generate Invoice");
        add(btn);

        lblResult = new JLabel("Total: ");
        add(lblResult);

        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    generateBill();
                } catch (InvalidOrderException ex) {
                    JOptionPane.showMessageDialog(null, ex.getMessage());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Invalid input!");
                }
            }
        });
    }

    // --------------------
    // Bill calculation
    // --------------------
    void generateBill() throws Exception {

        String cname = txtName.getText();
        int qty = Integer.parseInt(txtQty.getText());

        if (qty <= 0) {
            throw new InvalidOrderException("Quantity must be greater than 0");
        }

        Product product;

        if (combo.getSelectedItem().equals("Coffee Bean")) {
            product = new CoffeeBean();
        } else {
            product = new BrewedCoffee();
        }

        Customer customer = new Customer(cname, 120); // fixed loyalty points
        Invoice invoice = new Invoice(customer);

        double total = product.getTotalPrice(qty);
        total = invoice.giveDiscount(total);

        lblResult.setText("Total: " + total + " BDT");

        saveInvoice(cname, product.name, qty, total);
    }

    // --------------------
    // File handling
    // --------------------
    void saveInvoice(String cname, String product, int qty, double total) {

        try {
            FileWriter fw = new FileWriter("invoice.txt", true);
            fw.write("Customer: " + cname +
                    ", Product: " + product +
                    ", Quantity: " + qty +
                    ", Total: " + total + " BDT\n");
            fw.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "File error!");
        }
    }

    // --------------------
    // Main method
    // --------------------
    public static void main(String[] args) {
        new CoffeeCafe().setVisible(true);
    }
}