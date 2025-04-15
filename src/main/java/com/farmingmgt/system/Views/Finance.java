package com.farmingmgt.system.Views;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

@Route(value = "finance", layout = HomeLayout.class)
@PageTitle("Finance")
public class Finance extends VerticalLayout {
    private final UI uiInstance;
    private final Firestore firestore;

    // Form components
    private final TextField descriptionField = new TextField("Description");
    private final NumberField amountField = new NumberField("Amount");
    private final DatePicker dateField = new DatePicker("Date");
    private final ComboBox<String> categoryComboBox = new ComboBox<>("Category");
    private final ComboBox<String> typeComboBox = new ComboBox<>("Type");

    // Grid to display transactions
    private Grid<Transaction> transactionGrid = new Grid<>(Transaction.class);

    // Statistics components
    private final H3 totalIncomeText = new H3("Total Income: KES 0");
    private final H3 totalExpensesText = new H3("Total Expenses: KES 0");
    private final H3 balanceText = new H3("Balance: kes 0");

    public Finance() {
        this.uiInstance = UI.getCurrent();
        this.firestore = FirestoreClient.getFirestore();

        setSizeFull();
        setPadding(true);

        configureUI();
        loadTransactions();
    }

    private void configureUI() {
        // Configure the form
        typeComboBox.setItems("Income", "Expense");
        categoryComboBox.setItems("Seeds", "Fertilizer", "Equipment", "Labor", "Sales", "Maintenance", "Other");

        dateField.setValue(LocalDate.now());

        Button saveButton = new Button("Save Transaction", event -> saveTransaction());
        saveButton.getStyle().set("background-color","blue");
        saveButton.getStyle().set("color","white");
        // Layout for the form
        FormLayout formLayout = new FormLayout();
        formLayout.add(
                typeComboBox, categoryComboBox,
                descriptionField, amountField,
                dateField, saveButton
        );
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        // Configure the grid
        transactionGrid.setColumns("date", "description", "category", "type", "amount");
        transactionGrid.setSizeFull();

        // Statistics panel
        HorizontalLayout statsLayout = new HorizontalLayout(totalIncomeText, totalExpensesText, balanceText);
        statsLayout.setWidthFull();
        statsLayout.setJustifyContentMode(JustifyContentMode.EVENLY);

        // Search component
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search transactions...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidthFull();
        searchField.addValueChangeListener(e -> filterTransactions(e.getValue()));

        // Main layout organization
        H2 title = new H2("Farm Financial Management");
        add(title);
        add(formLayout);
        add(new Hr());
        add(statsLayout);
        add(new H3("Transactions"));
        add(searchField);
        add(transactionGrid);
    }

    private void saveTransaction() {
        if (!validateForm()) {
            Notification.show("Please fill all required fields", 3000, Notification.Position.MIDDLE);
            return;
        }

        Transaction transaction = new Transaction();
        transaction.setDate(dateField.getValue().toString());
        transaction.setDescription(descriptionField.getValue());
        transaction.setCategory(categoryComboBox.getValue());
        transaction.setType(typeComboBox.getValue());
        transaction.setAmount(amountField.getValue());

        // Generate a unique ID
        String id = UUID.randomUUID().toString();
        transaction.setId(id);

        // Save to Firestore
        DocumentReference docRef = firestore.collection("transactions").document(id);
        ApiFuture<WriteResult> result = docRef.set(transaction);

        try {
            result.get();
            clearForm();
            Notification.show("Transaction saved successfully", 3000, Notification.Position.MIDDLE);
            loadTransactions();
        } catch (Exception e) {
            Notification.show("Error saving transaction: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE);
        }
    }

    private boolean validateForm() {
        return descriptionField.getValue() != null && !descriptionField.getValue().isEmpty() &&
                amountField.getValue() != null && amountField.getValue() > 0 &&
                dateField.getValue() != null &&
                categoryComboBox.getValue() != null &&
                typeComboBox.getValue() != null;
    }

    private void clearForm() {
        descriptionField.clear();
        amountField.clear();
        dateField.setValue(LocalDate.now());
        categoryComboBox.clear();
        typeComboBox.clear();
    }

    private void loadTransactions() {
        uiInstance.access(() -> {
            try {
                // Show loading indicator
                transactionGrid.setItems(Collections.emptyList());

                ApiFuture<QuerySnapshot> future = firestore.collection("transactions").get();
                List<Transaction> transactions = new ArrayList<>();

                // Get documents
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                for (QueryDocumentSnapshot document : documents) {
                    Transaction transaction = document.toObject(Transaction.class);
                    transactions.add(transaction);
                }

                // Update UI with data
                transactionGrid.setItems(transactions);
                updateStatistics(transactions);

            } catch (Exception e) {
                Notification.show("Error loading transactions: " + e.getMessage(),
                        5000, Notification.Position.MIDDLE);
            }
        });
    }

    private void filterTransactions(String filterText) {
        if (filterText == null || filterText.isEmpty()) {
            loadTransactions();
            return;
        }

        uiInstance.access(() -> {
            try {
                String lowerCaseFilter = filterText.toLowerCase();
                ApiFuture<QuerySnapshot> future = firestore.collection("transactions").get();
                List<Transaction> filteredList = new ArrayList<>();

                // Get documents and filter
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                for (QueryDocumentSnapshot document : documents) {
                    Transaction transaction = document.toObject(Transaction.class);

                    if (transaction.getDescription().toLowerCase().contains(lowerCaseFilter) ||
                            transaction.getCategory().toLowerCase().contains(lowerCaseFilter)) {
                        filteredList.add(transaction);
                    }
                }

                // Update UI with filtered data
                transactionGrid.setItems(filteredList);
                updateStatistics(filteredList);

            } catch (Exception e) {
                Notification.show("Error filtering transactions: " + e.getMessage(),
                        5000, Notification.Position.MIDDLE);
            }
        });
    }

    private void updateStatistics(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpenses = 0;

        for (Transaction transaction : transactions) {
            if ("Income".equals(transaction.getType())) {
                totalIncome += transaction.getAmount();
            } else if ("Expense".equals(transaction.getType())) {
                totalExpenses += transaction.getAmount();
            }
        }

        double balance = totalIncome - totalExpenses;
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        String formattedIncome = numberFormat.format(totalIncome);
        String formattedExpenses = numberFormat.format(totalExpenses);
        String formattedBalance = numberFormat.format(balance);

// Total Income
        Span incomeValue = new Span("KES " + formattedIncome);
        incomeValue.getStyle().set("color", "green");
        totalIncomeText.removeAll();
        totalIncomeText.add(new Text("Total Income: "), incomeValue);

// Total Expenses
        Span expensesValue = new Span("KES " + formattedExpenses);
        expensesValue.getStyle().set("color", "red");
        totalExpensesText.removeAll();
        totalExpensesText.add(new Text("Total Expenses: "), expensesValue);

// Balance
        Span balanceValue = new Span("KES " + formattedBalance);
        balanceValue.getStyle().set("color", "blue");
        balanceText.removeAll();
        balanceText.add(new Text("Balance: "), balanceValue);


    }

    // Transaction class to represent financial transactions
    public static class Transaction {
        private String id;
        private String date;
        private String description;
        private String category;
        private String type; // Income or Expense
        private double amount;

        // Default constructor required for Firestore
        public Transaction() {}

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
    }
}

