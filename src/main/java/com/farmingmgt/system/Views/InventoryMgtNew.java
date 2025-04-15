package com.farmingmgt.system.Views;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Route(value = "inventorymgtnew", layout = ManagerLayout.class)
@PageTitle("Inventory Management")
public class InventoryMgtNew extends VerticalLayout {
    private final UI uiInstance;
    private final Firestore firestore;

    // Dashboard statistics
    private final Map<String, Span> statsCountSpans = new HashMap<>();
    private int inventoryCount = 0;
    private int vendorCount = 0;
    private int pendingOrdersCount = 0;
    private int lowStockCount = 0;

    // UI Components
    private Grid<InventoryItem> inventoryGrid;
    private Grid<Order> ordersGrid;
    private Tabs viewTabs;
    private Div contentContainer;
    private Div inventoryContent;
    private Div ordersContent;
    private Div vendorsContent;

    public InventoryMgtNew() {
        this.uiInstance = UI.getCurrent();
        this.firestore = FirestoreClient.getFirestore();
        setWidthFull();
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Create header with title and date
        add(createHeader());

        // Create overview section with statistics cards
        add(createStatisticsSection());

        // Create tabbed content area
        add(createTabbedContent());

        // Load data from Firestore
        loadSupplyChainData();
    }

    private HorizontalLayout createHeader() {
        H1 title = new H1("Inventory Management");
        title.getStyle().set("margin", "0");

        // Current date display
        LocalDate today = LocalDate.now();
        Span dateSpan = new Span(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dateSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        // Search field
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search inventory...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.getStyle().set("margin-left", "auto");

        // Add new item button
        Button addButton = new Button("Add Item", VaadinIcon.PLUS.create());
        addButton.addClickListener(e -> showAddItemDialog());

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        header.add(title, dateSpan, searchField, addButton);
        header.expand(searchField);
        header.getStyle().set("padding", "var(--lumo-space-m)");

        return header;
    }

    private HorizontalLayout createStatisticsSection() {
        // Inventory Items Card
        Div inventoryCard = createStatCard(
                "Inventory Items",
                "0",
                "inventoryCount",
                VaadinIcon.PACKAGE,
                "#5E81AC");

        // Vendors Card
        Div vendorsCard = createStatCard(
                "Vendors",
                "0",
                "vendorCount",
                VaadinIcon.USERS,
                "#A3BE8C");

        // Pending Orders Card
        Div ordersCard = createStatCard(
                "Pending Orders",
                "0",
                "pendingOrdersCount",
                VaadinIcon.TRUCK,
                "#EBCB8B");

        // Low Stock Items Card
        Div lowStockCard = createStatCard(
                "Low Stock Items",
                "0",
                "lowStockCount",
                VaadinIcon.WARNING,
                "#BF616A");

        HorizontalLayout statsLayout = new HorizontalLayout(inventoryCard, vendorsCard, ordersCard, lowStockCard);
        statsLayout.setWidthFull();
        statsLayout.setPadding(true);
        statsLayout.setSpacing(true);
        statsLayout.getStyle().set("padding", "var(--lumo-space-m)");

        return statsLayout;
    }

    private Div createStatCard(String title, String initialValue, String key, VaadinIcon icon, String color) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background-color", "white")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-m)")
                .set("display", "flex")
                .set("flex-direction", "column");

        Icon cardIcon = icon.create();
        cardIcon.getStyle()
                .set("background-color", color + "33")
                .set("color", color)
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "var(--lumo-space-s)")
                .set("width", "48px")
                .set("height", "48px");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin-top", "var(--lumo-space-s)");

        Span countSpan = new Span(initialValue);
        countSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("margin-top", "var(--lumo-space-xs)");

        statsCountSpans.put(key, countSpan);

        card.add(cardIcon, titleSpan, countSpan);
        return card;
    }

    private VerticalLayout createTabbedContent() {
        // Create tabs
        Tab inventoryTab = new Tab(VaadinIcon.PACKAGE.create(), new Span("Inventory"));
        //Tab ordersTab = new Tab(VaadinIcon.TRUCK.create(), new Span("Orders"));
        // Tab vendorsTab = new Tab(VaadinIcon.USERS.create(), new Span("Vendors"));

        viewTabs = new Tabs(inventoryTab);
        viewTabs.getStyle().set("margin", "0");

        // Create content areas
        contentContainer = new Div();
        contentContainer.setSizeFull();
        //contentContainer.getStyle().set("padding", "var(--lumo-space-m)");

        // Initialize content for each tab
        inventoryContent = createInventoryContent();
        ordersContent = createOrdersContent();
        vendorsContent = createVendorsContent();

        // Show default tab content
        contentContainer.add(inventoryContent);

        // Handle tab selection
        viewTabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(inventoryTab)) {
                contentContainer.add(inventoryContent);
            } else {
                contentContainer.add(vendorsContent);
            }
        });

        VerticalLayout layout = new VerticalLayout(viewTabs, contentContainer);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setSizeFull();
        layout.expand(contentContainer);

        return layout;
    }

    private Div createInventoryContent() {
        Div content = new Div();
        content.setSizeFull();
        // Create inventory grid
        inventoryGrid = new Grid<>();
        inventoryGrid.setWidthFull();
        inventoryGrid.addColumn(InventoryItem::getName).setHeader("Item Name").setSortable(true);
        inventoryGrid.addColumn(InventoryItem::getCategory).setHeader("Category").setSortable(true);
        inventoryGrid.addColumn(InventoryItem::getQuantity).setHeader("Quantity").setSortable(true);
        inventoryGrid.addColumn(InventoryItem::getUnit).setHeader("Unit");
        inventoryGrid.addColumn(InventoryItem::getLastRestocked).setHeader("Last Restocked").setSortable(true);
        inventoryGrid.addColumn(InventoryItem::getSupplier).setHeader("Supplier");

        // Add action column
        inventoryGrid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editButton.addClickListener(e -> showEditItemDialog(item));
            editButton.getStyle().set("background-color", "blue");
            editButton.getStyle().set("color", "white");
            Button deleteButton = new Button(new Icon(VaadinIcon.TRASH));
            deleteButton.getStyle().set("background-color", "red");
            deleteButton.getStyle().set("color", "white");
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> deleteInventoryItem(item));

            actions.add(editButton, deleteButton);
            return actions;
        })).setHeader("Actions").setFlexGrow(0);

        inventoryGrid.setSizeFull();
        content.add(inventoryGrid);

        return content;
    }

    private Div createOrdersContent() {
        Div content = new Div();
        content.setSizeFull();

        // Create orders grid
        ordersGrid = new Grid<>();
        ordersGrid.addColumn(Order::getOrderId).setHeader("Order ID").setSortable(true);
        ordersGrid.addColumn(Order::getSupplier).setHeader("Supplier").setSortable(true);
        ordersGrid.addColumn(Order::getItems).setHeader("Items");
        ordersGrid.addColumn(Order::getOrderDate).setHeader("Order Date").setSortable(true);
        ordersGrid.addColumn(Order::getExpectedDelivery).setHeader("Expected Delivery").setSortable(true);
        ordersGrid.addColumn(Order::getStatus).setHeader("Status").setSortable(true);

        // Add action column
        ordersGrid.addColumn(new ComponentRenderer<>(order -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button viewButton = new Button(new Icon(VaadinIcon.EYE));
            viewButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            viewButton.addClickListener(e -> showOrderDetails(order));

            Button updateButton = new Button(new Icon(VaadinIcon.CHECK));
            updateButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            updateButton.addClickListener(e -> updateOrderStatus(order));

            actions.add(viewButton, updateButton);
            return actions;
        })).setHeader("Actions").setFlexGrow(0);

        ordersGrid.setSizeFull();

        // Add new order button
        Button addOrderButton = new Button("New Order", VaadinIcon.PLUS.create());
        addOrderButton.addClickListener(e -> showAddOrderDialog());

        VerticalLayout layout = new VerticalLayout(addOrderButton, ordersGrid);
        layout.setSizeFull();
        layout.expand(ordersGrid);
        content.add(layout);

        return content;
    }

    private Div createVendorsContent() {
        Div content = new Div();
        content.setSizeFull();

        // Create vendors grid
        Grid<Vendor> vendorsGrid = new Grid<>();
        vendorsGrid.addColumn(Vendor::getName).setHeader("Vendor Name").setSortable(true);
        vendorsGrid.addColumn(Vendor::getContactPerson).setHeader("Contact Person");
        vendorsGrid.addColumn(Vendor::getPhone).setHeader("Phone");
        vendorsGrid.addColumn(Vendor::getEmail).setHeader("Email");
        vendorsGrid.addColumn(Vendor::getItemsSupplied).setHeader("Items Supplied");
        vendorsGrid.addColumn(Vendor::getLastOrderDate).setHeader("Last Order").setSortable(true);

        // Add action column
        vendorsGrid.addColumn(new ComponentRenderer<>(vendor -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editButton.addClickListener(e -> showEditVendorDialog(vendor));

            Button orderButton = new Button(new Icon(VaadinIcon.CART));
            orderButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SUCCESS);
            orderButton.addClickListener(e -> createOrderFromVendor(vendor));

            actions.add(editButton, orderButton);
            return actions;
        })).setHeader("Actions").setFlexGrow(0);

        vendorsGrid.setSizeFull();

        // Add new vendor button
        Button addVendorButton = new Button("Add Vendor", VaadinIcon.PLUS.create());
        addVendorButton.addClickListener(e -> showAddVendorDialog());

        VerticalLayout layout = new VerticalLayout(addVendorButton, vendorsGrid);
        layout.setSizeFull();
        layout.expand(vendorsGrid);
        content.add(layout);

        return content;
    }

    private void loadSupplyChainData() {
        try {
            // Load inventory items
            QuerySnapshot inventorySnapshot = firestore.collection("inventory").get().get();
            List<InventoryItem> inventoryItems = new ArrayList<>();

            for (QueryDocumentSnapshot doc : inventorySnapshot) {
                InventoryItem item = doc.toObject(InventoryItem.class);
                item.setId(doc.getId());
                inventoryItems.add(item);

                // Check for low stock
                if (item.getQuantity() < item.getReorderLevel()) {
                    lowStockCount++;
                }
            }

            inventoryCount = inventoryItems.size();
            inventoryGrid.setItems(inventoryItems);

            // Load orders
            QuerySnapshot ordersSnapshot = firestore.collection("orders").get().get();
            List<Order> orders = new ArrayList<>();

            for (QueryDocumentSnapshot doc : ordersSnapshot) {
                Order order = doc.toObject(Order.class);
                order.setId(doc.getId());
                orders.add(order);

                // Count pending orders
                if ("Pending".equals(order.getStatus())) {
                    pendingOrdersCount++;
                }
            }

            ordersGrid.setItems(orders);

            // Load vendors
            QuerySnapshot vendorsSnapshot = firestore.collection("vendors").get().get();
            vendorCount = vendorsSnapshot.size();

            // Update statistics
            updateStatistics();

        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error loading data: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateStatistics() {
        statsCountSpans.get("inventoryCount").setText(String.valueOf(inventoryCount));
        statsCountSpans.get("vendorCount").setText(String.valueOf(vendorCount));
        statsCountSpans.get("pendingOrdersCount").setText(String.valueOf(pendingOrdersCount));
        statsCountSpans.get("lowStockCount").setText(String.valueOf(lowStockCount));
    }

    private void showAddItemDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Inventory Item");
        dialog.setWidth("400px");
        // Form fields
        TextField nameField = new TextField("Item Name");
        nameField.setWidthFull();
        ComboBox<String> categoryField = new ComboBox<>("Category");
        categoryField.setWidthFull();
        categoryField.setItems("Seeds", "Fertilizer", "Pesticide", "Equipment", "Feed", "Medication", "Other");
        NumberField quantityField = new NumberField("Quantity");
        quantityField.setWidthFull();
        TextField unitField = new TextField("Unit");
        unitField.setWidthFull();
        NumberField reorderLevelField = new NumberField("Reorder Level");
        reorderLevelField.setWidthFull();
        TextField supplierField = new TextField("Supplier");
        supplierField.setWidthFull();
        DatePicker lastRestockedField = new DatePicker("Last Restocked");
        lastRestockedField.setWidthFull();
        lastRestockedField.setValue(LocalDate.now());

        // Layout
        VerticalLayout formLayout = new VerticalLayout(
                nameField, categoryField, quantityField, unitField,
                reorderLevelField, supplierField, lastRestockedField);
        formLayout.setSpacing(true);
        formLayout.setPadding(false);

        dialog.add(formLayout);

        // Buttons
        Button saveButton = new Button("Save", e -> {
            if (nameField.isEmpty() || categoryField.isEmpty() || quantityField.isEmpty() || unitField.isEmpty()) {
                Notification.show("Please fill in all required fields", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Create new inventory item
            InventoryItem newItem = new InventoryItem();
            newItem.setName(nameField.getValue());
            newItem.setCategory(categoryField.getValue());
            newItem.setQuantity(quantityField.getValue().intValue());
            newItem.setUnit(unitField.getValue());
            newItem.setReorderLevel(reorderLevelField.getValue() != null ? reorderLevelField.getValue().intValue() : 10);
            newItem.setSupplier(supplierField.getValue());
            newItem.setLastRestocked(lastRestockedField.getValue().toString());

            // Save to Firestore
            saveInventoryItem(newItem);
            dialog.close();
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void saveInventoryItem(InventoryItem item) {
        try {
            DocumentReference docRef;
            if (item.getId() != null && !item.getId().isEmpty()) {
                // Update existing item
                docRef = firestore.collection("inventory").document(item.getId());
            } else {
                // Add new item
                docRef = firestore.collection("inventory").document();
                item.setId(docRef.getId());
            }

            WriteResult result = docRef.set(item).get();

            Notification.show("Item saved successfully", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Refresh data
            loadSupplyChainData();

        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error saving item: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showEditItemDialog(InventoryItem item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Inventory Item");
        dialog.setWidth("300px");
        // Form fields with existing values
        TextField nameField = new TextField("Item Name", item.getName(), "");
        nameField.setWidthFull();
        ComboBox<String> categoryField = new ComboBox<>("Category");
        categoryField.setWidthFull();
        categoryField.setItems("Seeds", "Fertilizer", "Pesticide", "Equipment", "Feed", "Medication", "Other");
        categoryField.setValue(item.getCategory());
        NumberField quantityField = new NumberField("Quantity");
        quantityField.setWidthFull();
        quantityField.setValue(item.getQuantity());
        TextField unitField = new TextField("Unit", item.getUnit(), "");
        unitField.setWidthFull();
        NumberField reorderLevelField = new NumberField("Reorder Value");
        reorderLevelField.setValue(item.getReorderLevel());
        reorderLevelField.setWidthFull();
        TextField supplierField = new TextField("Supplier", item.getSupplier(), "");
        supplierField.setWidthFull();
        DatePicker lastRestockedField = new DatePicker("Last Restocked");
        supplierField.setWidthFull();
        lastRestockedField.setValue(LocalDate.parse(item.getLastRestocked()));
        lastRestockedField.setWidthFull();

        // Layout
        VerticalLayout formLayout = new VerticalLayout(
                nameField, categoryField, quantityField, unitField,
                reorderLevelField, supplierField, lastRestockedField);
        formLayout.setSpacing(true);
        formLayout.setPadding(false);

        dialog.add(formLayout);

        // Buttons
        Button saveButton = new Button("Save", e -> {
            if (nameField.isEmpty() || categoryField.isEmpty() || quantityField.isEmpty() || unitField.isEmpty()) {
                Notification.show("Please fill in all required fields", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Update item
            item.setName(nameField.getValue());
            item.setCategory(categoryField.getValue());
            item.setQuantity(quantityField.getValue().intValue());
            item.setUnit(unitField.getValue());
            item.setReorderLevel(reorderLevelField.getValue().intValue());
            item.setSupplier(supplierField.getValue());
            item.setLastRestocked(lastRestockedField.getValue().toString());

            // Save to Firestore
            saveInventoryItem(item);
            dialog.close();
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void deleteInventoryItem(InventoryItem item) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.add(new Text("Are you sure you want to delete this item?"));

        Button confirmButton = new Button("Delete", event -> {
            try {
                firestore.collection("inventory").document(item.getId()).delete().get();

                Notification.show("Item deleted successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Refresh data
                loadSupplyChainData();

            } catch (InterruptedException | ExecutionException e) {
                Notification.show("Error deleting item: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } finally {
                confirmDialog.close();
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        Button cancelButton = new Button("Cancel", event -> confirmDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttonLayout = new HorizontalLayout(confirmButton, cancelButton);
        confirmDialog.add(buttonLayout);

        confirmDialog.open();
    }

    private void showAddOrderDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create New Order");
        dialog.setWidth("400px");

        // Form fields
        ComboBox<String> supplierField = new ComboBox<>("Supplier");
        // TODO: Load suppliers from Firestore
        supplierField.setItems("Acme Farm Supplies", "GreenGrow Inc", "FarmTech Solutions", "AgriChem Corp");
        supplierField.setSizeFull();
        TextField itemsField = new TextField("Items (comma separated)");
        itemsField.setWidthFull();
        DatePicker orderDateField = new DatePicker("Order Date");
        orderDateField.setWidthFull();
        orderDateField.setValue(LocalDate.now());
        DatePicker expectedDeliveryField = new DatePicker("Expected Delivery");
        expectedDeliveryField.setWidthFull();
        expectedDeliveryField.setValue(LocalDate.now().plusDays(7));
        ComboBox<String> statusField = new ComboBox<>("Status");
        statusField.setWidthFull();
        statusField.setItems("Pending", "Shipped", "Delivered", "Cancelled");
        statusField.setValue("Pending");

        // Layout
        VerticalLayout formLayout = new VerticalLayout(
                supplierField, itemsField, orderDateField, expectedDeliveryField, statusField);
        formLayout.setSpacing(true);
        formLayout.setPadding(false);

        dialog.add(formLayout);

        // Buttons
        Button saveButton = new Button("Create Order", e -> {
            if (supplierField.isEmpty() || itemsField.isEmpty() || orderDateField.isEmpty() || expectedDeliveryField.isEmpty()) {
                Notification.show("Please fill in all required fields", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Create new order
            Order newOrder = new Order();
            newOrder.setOrderId("ORD-" + System.currentTimeMillis());
            newOrder.setSupplier(supplierField.getValue());
            newOrder.setItems(itemsField.getValue());
            newOrder.setOrderDate(orderDateField.getValue().toString());
            newOrder.setExpectedDelivery(expectedDeliveryField.getValue().toString());
            newOrder.setStatus(statusField.getValue());

            // Save to Firestore
            saveOrder(newOrder);
            dialog.close();
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void saveOrder(Order order) {
        try {
            DocumentReference docRef;
            if (order.getId() != null && !order.getId().isEmpty()) {
                // Update existing order
                docRef = firestore.collection("orders").document(order.getId());
            } else {
                // Add new order
                docRef = firestore.collection("orders").document();
                order.setId(docRef.getId());
            }

            WriteResult result = docRef.set(order).get();

            Notification.show("Order saved successfully", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Refresh data
            loadSupplyChainData();

        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error saving order: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showOrderDetails(Order order) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Order Details");
        dialog.setWidth("300px");

        // Create grid for order details
        Grid<OrderItem> orderItemsGrid = new Grid<>();

        // Add columns for basic order info
        H2 orderIdTitle = new H2("Order #" + order.getOrderId());

        Span supplierInfo = new Span("Supplier: " + order.getSupplier());
        Span dateInfo = new Span("Order Date: " + order.getOrderDate());
        Span deliveryInfo = new Span("Expected Delivery: " + order.getExpectedDelivery());
        Span statusInfo = new Span("Status: " + order.getStatus());
        statusInfo.getStyle().set("font-weight", "bold");

        if ("Pending".equals(order.getStatus())) {
            statusInfo.getStyle().set("color", "#EBCB8B");
        } else if ("Shipped".equals(order.getStatus())) {
            statusInfo.getStyle().set("color", "#5E81AC");
        } else if ("Delivered".equals(order.getStatus())) {
            statusInfo.getStyle().set("color", "#A3BE8C");
        } else {
            statusInfo.getStyle().set("color", "#BF616A");
        }

        VerticalLayout orderInfoLayout = new VerticalLayout(
                orderIdTitle, supplierInfo, dateInfo, deliveryInfo, statusInfo);
        orderInfoLayout.setSpacing(false);
        orderInfoLayout.setPadding(false);

        // Add items list (simplified for demonstration)
        H2 itemsTitle = new H2("Items");
        Span itemsList = new Span(order.getItems());

        VerticalLayout itemsLayout = new VerticalLayout(itemsTitle, itemsList);
        itemsLayout.setSpacing(false);
        itemsLayout.setPadding(false);

        dialog.add(orderInfoLayout, itemsLayout);

        // Close button
        Button closeButton = new Button("Close", e -> dialog.close());

        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private void updateOrderStatus(Order order) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Update Order Status");

        // Create status selector
        ComboBox<String> statusField = new ComboBox<>("Status");
        statusField.setItems("Pending", "Shipped", "Delivered", "Cancelled");
        statusField.setValue(order.getStatus());

        dialog.add(statusField);

        // Buttons
        Button saveButton = new Button("Update", e -> {
            order.setStatus(statusField.getValue());
            saveOrder(order);
            dialog.close();
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void showAddVendorDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Vendor");
        dialog.setWidth("400px");
        // Form fields
        TextField nameField = new TextField("Vendor Name");
        nameField.setWidthFull();
        TextField contactPersonField = new TextField("Contact Person");
        contactPersonField.setWidthFull();
        TextField phoneField = new TextField("Phone");
        phoneField.setWidthFull();
        TextField emailField = new TextField("Email");
        emailField.setWidthFull();
        TextField itemsSuppliedField = new TextField("Items Supplied (comma separated)");
        itemsSuppliedField.setWidthFull();
        DatePicker lastOrderDateField = new DatePicker("Last Order Date");
        lastOrderDateField.setWidthFull();
        lastOrderDateField.setValue(LocalDate.now());

        // Layout
        VerticalLayout formLayout = new VerticalLayout(
                nameField, contactPersonField, phoneField, emailField, itemsSuppliedField, lastOrderDateField);
        formLayout.setSpacing(true);
        formLayout.setPadding(false);

        dialog.add(formLayout);

        // Buttons
        Button saveButton = new Button("Save", e -> {
            if (nameField.isEmpty() || contactPersonField.isEmpty() || phoneField.isEmpty() || emailField.isEmpty()) {
                Notification.show("Please fill in all required fields", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Create new vendor
            Vendor newVendor = new Vendor();
            newVendor.setName(nameField.getValue());
            newVendor.setContactPerson(contactPersonField.getValue());
            newVendor.setPhone(phoneField.getValue());
            newVendor.setEmail(emailField.getValue());
            newVendor.setItemsSupplied(itemsSuppliedField.getValue());
            newVendor.setLastOrderDate(lastOrderDateField.getValue().toString());

            // Save to Firestore
            saveVendor(newVendor);
            dialog.close();
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void saveVendor(Vendor vendor) {
        try {
            DocumentReference docRef;
            if (vendor.getId() != null && !vendor.getId().isEmpty()) {
                // Update existing vendor
                docRef = firestore.collection("vendors").document(vendor.getId());
            } else {
                // Add new vendor
                docRef = firestore.collection("vendors").document();
                vendor.setId(docRef.getId());
            }

            WriteResult result = docRef.set(vendor).get();

            Notification.show("Vendor saved successfully", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Refresh data
            loadSupplyChainData();

        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error saving vendor: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void showEditVendorDialog(Vendor vendor) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Vendor");

        // Form fields with existing values
        TextField nameField = new TextField("Vendor Name", vendor.getName(), "");
        TextField contactPersonField = new TextField("Contact Person", vendor.getContactPerson(), "");
        TextField phoneField = new TextField("Phone", vendor.getPhone(), "");
        TextField emailField = new TextField("Email", vendor.getEmail(), "");
        TextField itemsSuppliedField = new TextField("Items Supplied", vendor.getItemsSupplied(), "");
        DatePicker lastOrderDateField = new DatePicker("Last Order Date");
        lastOrderDateField.setValue(LocalDate.parse(vendor.getLastOrderDate()));

        // Layout
        VerticalLayout formLayout = new VerticalLayout(
                nameField, contactPersonField, phoneField, emailField, itemsSuppliedField, lastOrderDateField);
        formLayout.setSpacing(true);
        formLayout.setPadding(false);

        dialog.add(formLayout);

        // Buttons
        Button saveButton = new Button("Save", e -> {
            if (nameField.isEmpty() || contactPersonField.isEmpty() || phoneField.isEmpty() || emailField.isEmpty()) {
                Notification.show("Please fill in all required fields", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Update vendor
            vendor.setName(nameField.getValue());
            vendor.setContactPerson(contactPersonField.getValue());
            vendor.setPhone(phoneField.getValue());
            vendor.setEmail(emailField.getValue());
            vendor.setItemsSupplied(itemsSuppliedField.getValue());
            vendor.setLastOrderDate(lastOrderDateField.getValue().toString());

            // Save to Firestore
            saveVendor(vendor);
            dialog.close();
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void createOrderFromVendor(Vendor vendor) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Order from Vendor: " + vendor.getName());
        dialog.setWidth("400px");

        // Form fields
        TextField itemsField = new TextField("Items (comma separated)");
        itemsField.setValue(vendor.getItemsSupplied());
        DatePicker orderDateField = new DatePicker("Order Date");
        orderDateField.setValue(LocalDate.now());
        DatePicker expectedDeliveryField = new DatePicker("Expected Delivery");
        expectedDeliveryField.setValue(LocalDate.now().plusDays(7));
        ComboBox<String> statusField = new ComboBox<>("Status");
        statusField.setItems("Pending", "Shipped", "Delivered", "Cancelled");
        statusField.setValue("Pending");

        // Layout
        VerticalLayout formLayout = new VerticalLayout(
                itemsField, orderDateField, expectedDeliveryField, statusField);
        formLayout.setSpacing(true);
        formLayout.setPadding(false);

        dialog.add(formLayout);

        // Buttons
        Button saveButton = new Button("Create Order", e -> {
            if (itemsField.isEmpty() || orderDateField.isEmpty() || expectedDeliveryField.isEmpty()) {
                Notification.show("Please fill in all required fields", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Create new order
            Order newOrder = new Order();
            newOrder.setOrderId("ORD-" + System.currentTimeMillis());
            newOrder.setSupplier(vendor.getName());
            newOrder.setItems(itemsField.getValue());
            newOrder.setOrderDate(orderDateField.getValue().toString());
            newOrder.setExpectedDelivery(expectedDeliveryField.getValue().toString());
            newOrder.setStatus(statusField.getValue());

            // Save to Firestore
            saveOrder(newOrder);

            // Update vendor's last order date
            vendor.setLastOrderDate(LocalDate.now().toString());
            saveVendor(vendor);

            dialog.close();
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    // Inner classes for data models
    public static class InventoryItem {
        private String id;
        private String name;
        private String category;
        private int quantity;
        private String unit;
        private int reorderLevel;
        private String supplier;
        private String lastRestocked;

        public InventoryItem() {
            // Default constructor needed for Firestore
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public double getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public double getReorderLevel() {
            return reorderLevel;
        }

        public void setReorderLevel(int reorderLevel) {
            this.reorderLevel = reorderLevel;
        }

        public String getSupplier() {
            return supplier;
        }

        public void setSupplier(String supplier) {
            this.supplier = supplier;
        }

        public String getLastRestocked() {
            return lastRestocked;
        }

        public void setLastRestocked(String lastRestocked) {
            this.lastRestocked = lastRestocked;
        }
    }

    public static class Order {
        private String id;
        private String orderId;
        private String supplier;
        private String items;
        private String orderDate;
        private String expectedDelivery;
        private String status;

        public Order() {
            // Default constructor needed for Firestore
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getSupplier() {
            return supplier;
        }

        public void setSupplier(String supplier) {
            this.supplier = supplier;
        }

        public String getItems() {
            return items;
        }

        public void setItems(String items) {
            this.items = items;
        }

        public String getOrderDate() {
            return orderDate;
        }

        public void setOrderDate(String orderDate) {
            this.orderDate = orderDate;
        }

        public String getExpectedDelivery() {
            return expectedDelivery;
        }

        public void setExpectedDelivery(String expectedDelivery) {
            this.expectedDelivery = expectedDelivery;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class OrderItem {
        private String name;
        private int quantity;
        private String unit;
        private double price;

        public OrderItem() {
            // Default constructor
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }
    }

    public static class Vendor {
        private String id;
        private String name;
        private String contactPerson;
        private String phone;
        private String email;
        private String itemsSupplied;
        private String lastOrderDate;

        public Vendor() {
            // Default constructor needed for Firestore
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getContactPerson() {
            return contactPerson;
        }

        public void setContactPerson(String contactPerson) {
            this.contactPerson = contactPerson;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getItemsSupplied() {
            return itemsSupplied;
        }

        public void setItemsSupplied(String itemsSupplied) {
            this.itemsSupplied = itemsSupplied;
        }

        public String getLastOrderDate() {
            return lastOrderDate;
        }

        public void setLastOrderDate(String lastOrderDate) {
            this.lastOrderDate = lastOrderDate;
        }
    }
}