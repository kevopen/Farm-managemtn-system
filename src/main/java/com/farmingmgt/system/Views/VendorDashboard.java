package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.farmingmgt.system.Views.VendorLayout;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.yaml.snakeyaml.error.Mark;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executors;

@Route(value = "vendor-dashboard", layout = VendorLayout.class)
@PageTitle("Vendor Dashboard")
public class VendorDashboard extends VerticalLayout {
    private final UI uiInstance;
    private H2 dashboardTitle;
    private Span loadingIndicator;
    private Grid<Marketplace> salesGrid;
    private Grid<Purchase> purchasesGrid;
    private HorizontalLayout statsLayout;
    private String vendorId;
    Grid<Marketplace> grid = new Grid<>(Marketplace.class);
    private String farmId;
    private String farmName;

    public VendorDashboard() {
        this.uiInstance = UI.getCurrent();
        this.vendorId = UserSession.getUserUid();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        createLoadingIndicator();
        fetchVendorAndFarmData();
    }

    private void createLoadingIndicator() {
        loadingIndicator = new Span("Loading dashboard data...");
        loadingIndicator.getStyle().set("font-style", "italic");
        add(loadingIndicator);
    }

    private void fetchVendorAndFarmData() {
        // Get Firestore instance
        Firestore db = FirestoreClient.getFirestore();

        // Fetch vendor data
        db.collection("vendors").document(vendorId).get()
                .addListener(() -> {
                    // Run on UI thread
                    uiInstance.access(() -> {
                        try {
                            DocumentSnapshot vendorDoc = db.collection("vendors").document(vendorId).get().get();

                            if (vendorDoc.exists()) {
                                // Extract farmId from vendor document
                                farmId = vendorDoc.getString("farmId");

                                // Now fetch farm details using farmId
                                fetchFarmDetails(farmId);
                            } else {
                                showError("Vendor information not found.");
                            }
                        } catch (Exception e) {
                            showError("Error fetching vendor data: " + e.getMessage());
                        }
                    });
                }, Executors.newSingleThreadExecutor());
    }

    private void fetchFarmDetails(String farmId) {
        Firestore db = FirestoreClient.getFirestore();

        db.collection("farm_settings").document(farmId).get()
                .addListener(() -> {
                    // Run on UI thread
                    uiInstance.access(() -> {
                        try {
                            DocumentSnapshot farmDoc = db.collection("farm_settings").document(farmId).get().get();

                            if (farmDoc.exists()) {
                                // Extract farm details
                                farmName = farmDoc.getString("farmName");
                                String farmType = farmDoc.getString("farmType");
                                String currency = farmDoc.getString("currency");

                                // Remove loading indicator
                                remove(loadingIndicator);

                                // Initialize the dashboard with farm data
                                initializeDashboard(farmName, farmType, currency);

                                // Fetch sales and purchases
                                fetchSalesData();
                                fetchPurchasesData();
                            } else {
                                showError("Farm information not found.");
                            }
                        } catch (Exception e) {
                            showError("Error fetching farm data: " + e.getMessage());
                        }
                    });
                }, Executors.newSingleThreadExecutor());
    }

    private void initializeDashboard(String farmName, String farmType, String currency) {
        // Create dashboard title
        dashboardTitle = new H2(farmName + " - Vendor Dashboard");
        Span subtitleSpan = new Span("Farm Type: " + farmType + " | Currency: " + currency);
        subtitleSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout headerLayout = new VerticalLayout(dashboardTitle, subtitleSpan);
        headerLayout.setPadding(false);
        headerLayout.setSpacing(false);
        add(headerLayout);

        // Create statistics cards
        createStatsLayout();

        // Create tabs for sales and purchases
        createDataTabs();
    }

    private void createStatsLayout() {
        statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.addClassName("stats-layout");

        // Sales Today Card
        Div salesTodayCard = createStatsCard("Sales Today", "0", "TRENDING_UP", "green");

        // Total Sales Card
        Div totalSalesCard = createStatsCard("Total Sales", "0", "DOLLAR", "blue");

        // Purchases Card
        Div purchasesCard = createStatsCard("Customer Purchases", "0", "CART", "purple");

        statsLayout.add(salesTodayCard, totalSalesCard, purchasesCard);
        add(statsLayout);
    }

    private Div createStatsCard(String title, String value, String iconName, String color) {
        Div card = new Div();
        card.setWidth("100%");
        card.getStyle().set("padding", "1rem");
        card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-xs)");
        card.getStyle().set("background-color", "var(--lumo-base-color)");

        VerticalLayout cardContent = new VerticalLayout();
        cardContent.setPadding(false);
        cardContent.setSpacing(false);

        Icon cardIcon = VaadinIcon.valueOf(iconName).create();
        cardIcon.setColor(color);
        cardIcon.setSize("24px");

        H3 valueText = new H3(value);
        valueText.getStyle().set("margin", "0.5rem 0");

        Span titleText = new Span(title);
        titleText.getStyle().set("color", "var(--lumo-secondary-text-color)");

        cardContent.add(cardIcon, valueText, titleText);
        card.add(cardContent);

        return card;
    }

    private void createDataTabs() {
        Tabs tabs = new Tabs();
        Tab salesTab = new Tab("Sales");
        Tab purchasesTab = new Tab("Customer Purchases");
        tabs.add(salesTab, purchasesTab);

        // Create sales grid
        salesGrid = createSalesGrid();
        salesGrid.setVisible(true);

        // Create purchases grid
        purchasesGrid = createPurchasesGrid();
        purchasesGrid.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(salesTab)) {
                salesGrid.setVisible(true);
                purchasesGrid.setVisible(false);
            } else {
                salesGrid.setVisible(false);
                purchasesGrid.setVisible(true);
            }
        });

        add(tabs, salesGrid, purchasesGrid);
    }

    private Grid<Marketplace> createSalesGrid() {
        Grid<Marketplace> grid = new Grid<>(Marketplace.class);
        grid.setColumns("id", "createdAt", "name", "category", "itemType", "quantity", "price", "total");

        grid.getColumnByKey("id").setHeader("Item ID");
        grid.getColumnByKey("createdAt").setHeader("Date Added");
        grid.getColumnByKey("name").setHeader("Name");
        grid.getColumnByKey("category").setHeader("Category");
        grid.getColumnByKey("itemType").setHeader("Type");
        grid.getColumnByKey("quantity").setHeader("Qty");
        grid.getColumnByKey("price").setHeader("Price");
        grid.getColumnByKey("total").setHeader("Total");

// Add actions
        grid.addComponentColumn(item -> {
            Button viewButton = new Button("View", new Icon(VaadinIcon.SEARCH));
            viewButton.addClickListener(e -> viewItemDetails(item));
            return new HorizontalLayout(viewButton);
        }).setHeader("Actions");


        // Add new sale button
        Button addSaleButton = new Button("Add New Sale", new Icon(VaadinIcon.PLUS));
        addSaleButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        //addSaleButton.addClickListener(e -> openSaleEditor(null));

        VerticalLayout salesLayout = new VerticalLayout();
        salesLayout.add(addSaleButton, grid);
        salesLayout.setPadding(false);

        return grid;
    }

    private void viewItemDetails(Marketplace item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Item Details");

        VerticalLayout contentLayout = new VerticalLayout();

        contentLayout.add(new H5("Name: " + item.getName()));
        contentLayout.add(new Span("Category: " + item.getCategory()));
        contentLayout.add(new Span("Type: " + item.getItemType()));
        contentLayout.add(new Span("Price: KES " + item.getPrice()));
        contentLayout.add(new Span("Quantity: " + item.getQuantity()));
        contentLayout.add(new Span("Total: KES " + item.getTotal()));

        if (item.getCreatedAt() != null) {
            contentLayout.add(new Span("Created At: " + item.getCreatedAt().toDate()));
        }

        dialog.add(contentLayout);
        dialog.getFooter().add(new Button("Close", e -> dialog.close()));
        dialog.open();
    }


    private Grid<Purchase> createPurchasesGrid() {
        Grid<Purchase> grid = new Grid<>(Purchase.class);
        //grid.setHeightByRows(true);
        grid.setColumns("id", "date", "customer", "product", "quantity", "total");
        grid.getColumnByKey("id").setHeader("Purchase ID");
        grid.getColumnByKey("date").setHeader("Date");
        grid.getColumnByKey("customer").setHeader("Customer");
        grid.getColumnByKey("product").setHeader("Product");
        grid.getColumnByKey("quantity").setHeader("Quantity");
        grid.getColumnByKey("total").setHeader("Total");

        return grid;
    }

    private void fetchSalesData() {
        Firestore db = FirestoreClient.getFirestore();

        db.collection("marketplace")
                .whereEqualTo("vendorId", vendorId)
                .get()
                .addListener(() -> {
                    uiInstance.access(() -> {
                        try {
                            QuerySnapshot querySnapshot = db.collection("marketplace")
                                    .whereEqualTo("vendorId", vendorId)
                                    .get()
                                    .get();

                            List<Marketplace> items = new ArrayList<>();

                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Marketplace item = new Marketplace();
                                item.setId(doc.getString("itemId"));
                                item.setName(doc.getString("name"));
                                item.setCategory(doc.getString("category"));
                                item.setItemType(doc.getString("itemType"));
                                item.setQuantity(doc.getLong("quantity").intValue());
                                item.setPrice(doc.getDouble("price"));
                                item.setCreatedAt(doc.getTimestamp("createdAt"));

                                items.add(item);
                            }

                            // Set grid items
                            salesGrid.setItems(items);

                        } catch (Exception e) {
                            showError("Error fetching marketplace items: " + e.getMessage());
                        }
                    });
                }, Executors.newSingleThreadExecutor());
    }

    private void fetchPurchasesData() {
        Firestore db = FirestoreClient.getFirestore();

        // Query purchases collection for purchases related to this farm
        db.collection("purchases")
                .whereEqualTo("farmId", farmId)
                .get()
                .addListener(() -> {
                    uiInstance.access(() -> {
                        try {
                            QuerySnapshot querySnapshot = db.collection("purchases")
                                    .whereEqualTo("farmId", farmId)
                                    .get()
                                    .get();

                            List<Purchase> purchases = new ArrayList<>();

                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                Purchase purchase = document.toObject(Purchase.class);
                                if (purchase != null) {
                                    purchase.setId(document.getId());
                                    purchases.add(purchase);
                                }
                            }

                            // Update purchases grid
                            purchasesGrid.setItems(purchases);

                            // Update statistics
                            //updatePurchaseStatistics(purchases);

                        } catch (Exception e) {
                            showError("Error fetching purchase data: " + e.getMessage());
                        }
                    });
                }, Executors.newSingleThreadExecutor());
    }

    /*private void updateSalesStatistics(List<Marketplace> sales) {
        // Calculate total sales
        double totalSales = sales.stream().mapToDouble(Marketplace::getTotal).sum();

        // Calculate sales today
        LocalDate today = LocalDate.now();
        double salesToday = sales.stream()
                .filter(sale -> {
                    // Convert com.google.cloud.Timestamp to LocalDate
                    return sale.getDate() != null &&
                            sale.getDate().toDate().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate().equals(today);
                })
                .mapToDouble(Marketplace::getTotal).sum();

        // Update statistics cards
        Component salesTodayCard = statsLayout.getComponentAt(0);
        Component totalSalesCard = statsLayout.getComponentAt(1);

        if (salesTodayCard instanceof Div) {
            updateCardValue((Div) salesTodayCard, String.format("%.2f", salesToday));
        }

        if (totalSalesCard instanceof Div) {
            updateCardValue((Div) totalSalesCard, String.format("%.2f", totalSales));
        }
    }

    private void updatePurchaseStatistics(List<Purchase> purchases) {
        // Calculate total purchases
        double totalPurchases = purchases.stream().mapToDouble(Purchase::getTotal).sum();

        // Update statistics card
        Component purchasesCard = statsLayout.getComponentAt(2);

        if (purchasesCard instanceof Div) {
            updateCardValue((Div) purchasesCard, String.format("%.2f", totalPurchases));
        }
    }
*/
    private void updateCardValue(Div card, String value) {
        // Find the H3 value component within the card's VerticalLayout
        Optional<Component> contentOptional = card.getChildren().findFirst();

        if (contentOptional.isPresent() && contentOptional.get() instanceof VerticalLayout) {
            VerticalLayout content = (VerticalLayout) contentOptional.get();

            // Try to find the H3 component (should be the second child)
            Optional<Component> valueComponentOptional = content.getChildren()
                    .filter(component -> component instanceof H3)
                    .findFirst();

            if (valueComponentOptional.isPresent()) {
                H3 valueComponent = (H3) valueComponentOptional.get();
                valueComponent.setText(value);
            }
        }
    }

   /* private void openSaleEditor(Marketplace sale) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(sale == null ? "Add New Sale" : "Edit Sale");

        FormLayout form = new FormLayout();

        // Create form fields
        TextField productField = new TextField("Product");
        NumberField quantityField = new NumberField("Quantity");
        NumberField priceField = new NumberField("Unit Price");
        DatePicker dateField = new DatePicker("Date");
        dateField.setValue(LocalDate.now());

        // Fill fields if editing existing sale


        form.add(productField, quantityField, priceField, dateField);

        // Buttons
       /* Button saveButton = new Button("Save", e -> {
            if (validateSaleForm(productField, quantityField, priceField)) {
                saveSale(sale, productField.getValue(),
                        quantityField.getValue().intValue(),
                        priceField.getValue(),
                        dateField.getValue());
                dialog.close();
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        /dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }*/

    private boolean validateSaleForm(TextField productField, NumberField quantityField, NumberField priceField) {
        boolean valid = true;

        if (productField.isEmpty()) {
            productField.setInvalid(true);
            productField.setErrorMessage("Product is required");
            valid = false;
        }

        if (quantityField.isEmpty() || quantityField.getValue() <= 0) {
            quantityField.setInvalid(true);
            quantityField.setErrorMessage("Quantity must be greater than 0");
            valid = false;
        }

        if (priceField.isEmpty() || priceField.getValue() <= 0) {
            priceField.setInvalid(true);
            priceField.setErrorMessage("Price must be greater than 0");
            valid = false;
        }

        return valid;
    }

    private void saveSale(Marketplace existingSale, String product, int quantity, double price, LocalDate date) {
        Firestore db = FirestoreClient.getFirestore();

        Map<String, Object> saleData = new HashMap<>();
        saleData.put("product", product);
        saleData.put("quantity", quantity);
        saleData.put("price", price);
        saleData.put("total", price * quantity);

        // Convert LocalDate to java.util.Date then to com.google.cloud.Timestamp
        Date utilDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        saleData.put("date", com.google.cloud.Timestamp.of(utilDate));

        saleData.put("vendorId", vendorId);
        saleData.put("farmId", farmId);

        if (existingSale == null) {
            // Create new sale
            db.collection("sales").add(saleData)
                    .addListener(() -> {
                        uiInstance.access(() -> {
                            Notification.show("Sale added successfully!", 3000, Notification.Position.BOTTOM_CENTER);
                            fetchSalesData();
                        });
                    }, Executors.newSingleThreadExecutor());
        } else {
            // Update existing sale
            db.collection("sales").document(existingSale.getId()).update(saleData)
                    .addListener(() -> {
                        uiInstance.access(() -> {
                            Notification.show("Sale updated successfully!", 3000, Notification.Position.BOTTOM_CENTER);
                            fetchSalesData();
                        });
                    }, Executors.newSingleThreadExecutor());
        }
    }

    private void confirmDeleteSale(Marketplace sale) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm Delete");
        dialog.setText("Are you sure you want to delete this sale?");

        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(event -> deleteSale(sale));

        dialog.open();
    }

    private void deleteSale(Marketplace sale) {
        Firestore db = FirestoreClient.getFirestore();

        db.collection("sales").document(sale.getId()).delete()
                .addListener(() -> {
                    uiInstance.access(() -> {
                        Notification.show("Sale deleted successfully!", 3000, Notification.Position.BOTTOM_CENTER);
                        fetchSalesData();
                    });
                }, Executors.newSingleThreadExecutor());
    }

    private void showError(String message) {
        Notification notification = new Notification(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    // Sale model class
    // Purchase model class
    public static class Purchase {
        private String id;
        private String product;
        private String customer;
        private int quantity;
        private double total;
        private com.google.cloud.Timestamp date;
        private String farmId;

        public Purchase() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public String getCustomer() {
            return customer;
        }

        public void setCustomer(String customer) {
            this.customer = customer;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public com.google.cloud.Timestamp getDate() {
            return date;
        }

        public void setDate(com.google.cloud.Timestamp date) {
            this.date = date;
        }

        public String getFarmId() {
            return farmId;
        }

        public void setFarmId(String farmId) {
            this.farmId = farmId;
        }
    }
}