import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

class InvalidOrderException extends Exception {
    InvalidOrderException(String message) {
        super(message);
    }

    InvalidOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}

final class Money {

    private static final NumberFormat FORMAT;

    static {
        FORMAT = NumberFormat.getNumberInstance(Locale.US);
        FORMAT.setMinimumFractionDigits(2);
        FORMAT.setMaximumFractionDigits(2);
    }

    private Money() {
    }

    static BigDecimal of(String value) {
        return new BigDecimal(value);
    }

    static synchronized String format(BigDecimal value) {
        return FORMAT.format(value) + " BDT";
    }
}

interface Discount {

    BigDecimal apply(BigDecimal amount);

    String describe();
}

final class LoyaltyDiscount implements Discount {

    private final int points;

    LoyaltyDiscount(int points) {
        this.points = Math.max(0, points);
    }

    private static BigDecimal rateFor(int points) {
        if (points >= 1000) return Money.of("0.20");
        if (points >= 500) return Money.of("0.15");
        if (points >= 100) return Money.of("0.10");
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal apply(BigDecimal amount) {
        BigDecimal rate = rateFor(points);
        return amount.multiply(BigDecimal.ONE.subtract(rate))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String describe() {
        BigDecimal rate = rateFor(points);
        if (rate.signum() == 0) {
            return "no loyalty discount";
        }
        return rate.multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros().toPlainString() + "% loyalty discount";
    }
}

abstract class Product {

    private final String name;
    private final BigDecimal unitPrice;

    protected Product(String name, BigDecimal unitPrice) {
        this.name = name;
        this.unitPrice = unitPrice;
    }

    public final String getName() {
        return name;
    }

    public final BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public abstract BigDecimal priceFor(int quantity);

    @Override
    public final String toString() {
        return name + " (" + Money.format(unitPrice) + " each)";
    }
}

final class CoffeeBean extends Product {

    CoffeeBean() {
        super("Coffee Bean", Money.of("500.00"));
    }

    @Override
    public BigDecimal priceFor(int quantity) {
        return getUnitPrice().multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

final class BrewedCoffee extends Product {

    private static final BigDecimal BREWING_CHARGE = Money.of("50.00");

    BrewedCoffee() {
        super("Brewed Coffee", Money.of("300.00"));
    }

    @Override
    public BigDecimal priceFor(int quantity) {
        return getUnitPrice().multiply(BigDecimal.valueOf(quantity))
                .add(BREWING_CHARGE)
                .setScale(2, RoundingMode.HALF_UP);
    }
}

final class Customer {

    private final String name;
    private final int loyaltyPoints;

    Customer(String name, int loyaltyPoints) throws InvalidOrderException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidOrderException("Customer name cannot be empty.");
        }
        if (name.trim().length() > 60) {
            throw new InvalidOrderException("Customer name is too long (max 60 characters).");
        }
        if (loyaltyPoints < 0) {
            throw new InvalidOrderException("Loyalty points cannot be negative.");
        }
        this.name = name.trim();
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getName() {
        return name;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }
}

final class Order {

    private static final DateTimeFormatter CSV_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Customer customer;
    private final Product product;
    private final int quantity;
    private final BigDecimal subtotal;
    private final BigDecimal total;
    private final String discountLabel;
    private final LocalDateTime createdAt;

    Order(Customer customer,
          Product product,
          int quantity,
          BigDecimal subtotal,
          BigDecimal total,
          String discountLabel) {
        this.customer = customer;
        this.product = product;
        this.quantity = quantity;
        this.subtotal = subtotal;
        this.total = total;
        this.discountLabel = discountLabel;
        this.createdAt = LocalDateTime.now();
    }

    BigDecimal getTotal() {
        return total;
    }

    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    String toDisplayLine() {
        return String.format("%s | %-18s | %3d x %-14s | sub %11s | %-22s | TOTAL %11s",
                createdAt.format(DISPLAY_TIME),
                customer.getName(),
                quantity,
                product.getName(),
                Money.format(subtotal),
                discountLabel,
                Money.format(total));
    }

    String toCsvLine() {
        return String.join(",",
                createdAt.format(CSV_TIME),
                csv(customer.getName()),
                Integer.toString(customer.getLoyaltyPoints()),
                csv(product.getName()),
                Integer.toString(quantity),
                subtotal.toPlainString(),
                csv(discountLabel),
                total.toPlainString());
    }
}

final class Invoice {

    private static final int MAX_QUANTITY = 1000;

    private final Customer customer;
    private final Discount discountPolicy;

    private Invoice(Customer customer, Discount discountPolicy) {
        this.customer = customer;
        this.discountPolicy = discountPolicy;
    }

    static Invoice forCustomer(Customer customer) {
        return new Invoice(customer, new LoyaltyDiscount(customer.getLoyaltyPoints()));
    }

    Order createOrder(Product product, int quantity) throws InvalidOrderException {
        if (product == null) {
            throw new InvalidOrderException("Please select a coffee type.");
        }
        if (quantity <= 0) {
            throw new InvalidOrderException("Quantity must be greater than 0.");
        }
        if (quantity > MAX_QUANTITY) {
            throw new InvalidOrderException(
                    "Quantity cannot exceed " + MAX_QUANTITY + " per order.");
        }

        BigDecimal subtotal = product.priceFor(quantity);
        BigDecimal total = discountPolicy.apply(subtotal);
        return new Order(customer, product, quantity, subtotal, total, discountPolicy.describe());
    }
}

final class InvoiceRepository {

    private final Path file;

    InvoiceRepository(Path file) {
        this.file = file;
    }

    Path getFile() {
        return file;
    }

    synchronized void save(Order order) throws IOException {
        boolean isNewFile = !Files.exists(file) || Files.size(file) == 0;

        StringBuilder payload = new StringBuilder();
        if (isNewFile) {
            payload.append("timestamp,customer,loyalty_points,product,quantity,subtotal,discount,total")
                    .append(System.lineSeparator());
        }
        payload.append(order.toCsvLine()).append(System.lineSeparator());

        Files.writeString(file, payload.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}

public class CoffeeCafe extends JFrame {

    private final JTextField nameField = new JTextField(16);
    private final JSpinner pointsSpinner = new JSpinner(new SpinnerNumberModel(120, 0, 100_000, 10));
    private final JComboBox<Product> productCombo =
            new JComboBox<>(new Product[]{new CoffeeBean(), new BrewedCoffee()});
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));

    private final JLabel totalLabel = new JLabel("Total: 0.00 BDT");
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextArea logArea = new JTextArea();
    private final JButton generateButton = new JButton("Generate Invoice");

    private final InvoiceRepository repository = new InvoiceRepository(Paths.get("invoice.txt"));

    public CoffeeCafe() {
        super("Specialized Coffee Roastery & Cafe");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(buildContent());
        pack();
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(generateButton);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.add(buildForm(), BorderLayout.NORTH);
        root.add(buildLog(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("New Order"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, gbc, 0, "Customer name:", nameField);
        addRow(form, gbc, 1, "Loyalty points:", pointsSpinner);
        addRow(form, gbc, 2, "Coffee type:", productCombo);
        addRow(form, gbc, 3, "Quantity:", quantitySpinner);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(new JLabel(
                "<html><i>Loyalty tiers: 100+ → 10%, 500+ → 15%, 1000+ → 20%</i></html>"), gbc);

        return form;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc,
                        int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private JScrollPane buildLog() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Invoice History"));
        return scroll;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 8));

        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 16f));
        totalLabel.setForeground(new Color(0x1B5E20));
        footer.add(totalLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton clearButton = new JButton("Clear");

        generateButton.addActionListener(e -> generateInvoice());
        clearButton.addActionListener(e -> resetForm());

        buttons.add(clearButton);
        buttons.add(generateButton);
        footer.add(buttons, BorderLayout.EAST);

        statusLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        footer.add(statusLabel, BorderLayout.SOUTH);

        return footer;
    }

    private void generateInvoice() {
        try {
            Customer customer = new Customer(nameField.getText(),
                    (Integer) pointsSpinner.getValue());
            Product product = (Product) productCombo.getSelectedItem();
            int quantity = (Integer) quantitySpinner.getValue();

            Order order = Invoice.forCustomer(customer).createOrder(product, quantity);

            totalLabel.setText("Total: " + Money.format(order.getTotal()));
            logArea.append(order.toDisplayLine() + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());

            try {
                repository.save(order);
                status("Saved to " + repository.getFile().toAbsolutePath());
            } catch (IOException ioe) {
                status("WARNING: invoice was not saved.");
                JOptionPane.showMessageDialog(this,
                        "Could not save invoice: " + ioe.getMessage(),
                        "File error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (InvalidOrderException ex) {
            status(ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Invalid order", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            status("Unexpected error.");
            JOptionPane.showMessageDialog(this,
                    "Unexpected error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetForm() {
        nameField.setText("");
        pointsSpinner.setValue(120);
        productCombo.setSelectedIndex(0);
        quantitySpinner.setValue(1);
        totalLabel.setText("Total: 0.00 BDT");
        status("Form cleared.");
        nameField.requestFocusInWindow();
    }

    private void status(String message) {
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CoffeeCafe().setVisible(true));
    }
}
