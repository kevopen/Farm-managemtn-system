package com.farmingmgt.system.Views;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.charts.model.style.SolidColor;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "analytics", layout = HomeLayout.class)
@PageTitle("Analytics")
public class Analytics extends VerticalLayout {
    private final Firestore firestore;
    private final UI uiInstance;
    // Standard Grid components
    private Grid<InventoryItem> inventoryGrid;
    private Grid<ActivityItem> activitiesGrid;
    private Grid<CategorySummary> categorySummaryGrid;

    // Basic charts using HTML components
    private Div inventoryChart;
    private Div activityChart;

    // Date filters
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    // Dashboard components
    private H3 inventoryBelowThreshold;
    private H3 activitiesCount;
    private H3 totalInventoryCount;

    public Analytics() {
        this.uiInstance = UI.getCurrent();
        this.firestore = FirestoreClient.getFirestore();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 pageTitle = new H2("Farm Analytics Dashboard");
        pageTitle.getStyle().set("margin-top", "0");

        configureFilters();
        configureGrids();
        configureCharts();
        configureDashboardCards();

        Button refreshButton = new Button("Refresh Data", e -> refreshData());
        refreshButton.getStyle().set("margin-bottom", "20px");

        add(
                pageTitle,
                createFilterSection(),
                createDashboardSummary(),
                createGridLayout(),
                createChartsLayout()
                //createCashAnalyticsPlaceholder(),
               // refreshButton
        );

        // Load initial data
        refreshData();
    }

    private void configureFilters() {
        startDatePicker = new DatePicker("Start Date");
        endDatePicker = new DatePicker("End Date");

        // Set default date range to last 30 days
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        startDatePicker.setValue(startDate);
        endDatePicker.setValue(endDate);

        startDatePicker.addValueChangeListener(e -> refreshData());
        endDatePicker.addValueChangeListener(e -> refreshData());
    }

    private Component createFilterSection() {
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setAlignItems(Alignment.BASELINE);

        filterLayout.add(
                startDatePicker,
                endDatePicker
        );

        return filterLayout;
    }

    private void configureGrids() {
        // Inventory Grid
        inventoryGrid = new Grid<>();
        inventoryGrid.addColumn(InventoryItem::getName).setHeader("Item").setSortable(true);
        inventoryGrid.addColumn(InventoryItem::getCategory).setHeader("Category").setSortable(true);
        inventoryGrid.addColumn(InventoryItem::getQuantity).setHeader("Quantity").setSortable(true);
        inventoryGrid.addColumn(InventoryItem::getReorderLevel).setHeader("Reorder Level").setSortable(true);
        inventoryGrid.addColumn(InventoryItem::getUnit).setHeader("Unit");
        inventoryGrid.addComponentColumn(item -> {
            Span status = new Span();
            if (item.getQuantity() < item.getReorderLevel()) {
                status.setText("Low Stock");
                status.getStyle().set("color", "red");
            } else {
                status.setText("OK");
                status.getStyle().set("color", "green");
            }
            return status;
        }).setHeader("Status");

       // inventoryGrid.setHeightByRows(true);
        inventoryGrid.setWidthFull();

        // Activities Grid
        activitiesGrid = new Grid<>();
        activitiesGrid.addColumn(ActivityItem::getTitle).setHeader("Title").setSortable(true);
        activitiesGrid.addColumn(ActivityItem::getType).setHeader("Type").setSortable(true);
        activitiesGrid.addColumn(ActivityItem::getLocation).setHeader("Location").setSortable(true);
        activitiesGrid.addColumn(ActivityItem::getDate).setHeader("Date").setSortable(true);

        //activitiesGrid.setHeightByRows(true);
        activitiesGrid.setWidthFull();

        // Category Summary Grid
        categorySummaryGrid = new Grid<>();
        categorySummaryGrid.addColumn(CategorySummary::getCategory).setHeader("Category").setSortable(true);
        categorySummaryGrid.addColumn(CategorySummary::getItemCount).setHeader("Items").setSortable(true);
        categorySummaryGrid.addColumn(CategorySummary::getBelowReorderCount).setHeader("Low Stock").setSortable(true);
        categorySummaryGrid.addComponentColumn(item -> {
            ProgressBar progress = new ProgressBar();
            double value = (double) item.getBelowReorderCount() / (item.getItemCount() > 0 ? item.getItemCount() : 1);
            progress.setValue(value);

            if (value > 0.5) {
                progress.getStyle().set("--lumo-primary-color", "red");
            } else if (value > 0.25) {
                progress.getStyle().set("--lumo-primary-color", "orange");
            }

            return progress;
        }).setHeader("Status");

       // categorySummaryGrid.setHeightByRows(true);
        categorySummaryGrid.setWidthFull();
    }

    private void configureDashboardCards() {
        inventoryBelowThreshold = new H3("0");
        activitiesCount = new H3("0");
        totalInventoryCount = new H3("0");
    }

    private void configureCharts() {
        // Create simple chart placeholders
        inventoryChart = new Div();
        inventoryChart.setHeightFull();
        inventoryChart.setWidthFull();
        inventoryChart.getStyle()
                .set("overflow", "hidden")
                .set("min-height", "250px");

        activityChart = new Div();
        activityChart.setHeightFull();
        activityChart.setWidthFull();
        activityChart.getStyle()
                .set("overflow", "hidden")
                .set("min-height", "250px");
    }

    private Component createDashboardSummary() {
        HorizontalLayout summaryLayout = new HorizontalLayout();
        summaryLayout.setWidthFull();
        summaryLayout.setPadding(true);
        summaryLayout.setSpacing(true);

        // Card 1: Total Inventory
        Div card1 = createSummaryCard("Total Inventory Items", totalInventoryCount);

        // Card 2: Items Below Threshold
        Div card2 = createSummaryCard("Items Below Threshold", inventoryBelowThreshold);
        inventoryBelowThreshold.getStyle().set("color", "red");

        // Card 3: Activities Count
        Div card3 = createSummaryCard("Activities", activitiesCount);

        summaryLayout.add(card1, card2, card3);

        // Equal width distribution
        card1.getStyle().set("flex", "1");
        card2.getStyle().set("flex", "1");
        card3.getStyle().set("flex", "1");

        return summaryLayout;
    }

    private Div createSummaryCard(String title, Component value) {
        Div card = new Div();
        card.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 5px rgba(0, 0, 0, 0.1)")
                .set("padding", "16px")
                .set("text-align", "center");

        H4 cardTitle = new H4(title);
        cardTitle.getStyle()
                .set("margin-top", "0")
                .set("color", "#666");

        card.add(cardTitle, value);

        return card;
    }

    private Component createGridLayout() {
        Tabs tabs = new Tabs();
        Div pages = new Div();
        pages.setSizeFull();

        Tab inventoryTab = new Tab("Inventory Analysis");
        Tab activitiesTab = new Tab("Activities Log");
        Tab categoriesTab = new Tab("Category Summary");

        tabs.add(inventoryTab, activitiesTab, categoriesTab);

        // Add grid components to respective div containers
        Div inventoryPage = new Div(inventoryGrid);
        Div activitiesPage = new Div(activitiesGrid);
        Div categoriesPage = new Div(categorySummaryGrid);

        pages.add(inventoryPage, activitiesPage, categoriesPage);

        // By default show first tab content
        inventoryPage.setVisible(true);
        activitiesPage.setVisible(false);
        categoriesPage.setVisible(false);

        // Style the pages
        for (Component page : pages.getChildren().collect(Collectors.toList())) {
            ((Div) page).getStyle()
                    .set("padding", "16px")
                    .set("box-sizing", "border-box");
        }

        // Set tabs change handler
        tabs.addSelectedChangeListener(event -> {
            // Hide all
            inventoryPage.setVisible(false);
            activitiesPage.setVisible(false);
            categoriesPage.setVisible(false);

            // Show selected
            Component selectedTab = event.getSelectedTab();
            if (selectedTab == inventoryTab) {
                inventoryPage.setVisible(true);
            } else if (selectedTab == activitiesTab) {
                activitiesPage.setVisible(true);
            } else if (selectedTab == categoriesTab) {
                categoriesPage.setVisible(true);
            }
        });

        // Wrapper
        Div cardWrapper = new Div();
        cardWrapper.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 5px rgba(0, 0, 0, 0.1)")
                .set("margin-top", "20px")
                .set("margin-bottom", "20px");

        VerticalLayout content = new VerticalLayout(tabs, pages);
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();

        cardWrapper.add(content);
        cardWrapper.setWidthFull();

        return cardWrapper;
    }

    private Component createChartsLayout() {
        // Header
        H3 chartsTitle = new H3("Visualizations");

        // Chart containers
        HorizontalLayout chartsLayout = new HorizontalLayout();
        chartsLayout.setWidthFull();

        // Chart 1: Inventory
        Div chart1 = new Div();
        chart1.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 5px rgba(0, 0, 0, 0.1)")
                .set("padding", "16px")
                .set("flex", "1");

        H4 chart1Title = new H4("Inventory Status");
        chart1.add(chart1Title, inventoryChart);

        // Chart 2: Activities
        Div chart2 = new Div();
        chart2.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 5px rgba(0, 0, 0, 0.1)")
                .set("padding", "16px")
                .set("flex", "1");

        H4 chart2Title = new H4("Activities by Type");
        chart2.add(chart2Title, activityChart);

        chartsLayout.add(chart1, chart2);

        VerticalLayout wrapper = new VerticalLayout(chartsTitle, chartsLayout);
        wrapper.setPadding(false);
        wrapper.setSpacing(true);

        return wrapper;
    }

    private void setupCashTracking() {
        // Dialog for cash tracking setup
        Dialog dialog = new Dialog();
        dialog.setWidth("400px");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);

        H3 dialogTitle = new H3("Setup Cash Analytics");
        dialogTitle.getStyle().set("margin-top", "0");

        TextField collectionNameField = new TextField("Collection Name");
        collectionNameField.setValue("cash_transactions");
        collectionNameField.setWidthFull();

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button createButton = new Button("Create", e -> {
            createCashCollection(collectionNameField.getValue());
            dialog.close();
            showNotification("Cash collection created successfully!");
        });

        buttonLayout.add(cancelButton, createButton);

        dialogLayout.add(dialogTitle, collectionNameField, buttonLayout);
        dialog.add(dialogLayout);

        dialog.open();
    }

    private void createCashCollection(String collectionName) {
        try {
            // Create an initial document to ensure the collection exists
            DocumentReference docRef = firestore.collection(collectionName).document();
            Map<String, Object> initialData = new HashMap<>();
            initialData.put("created", LocalDate.now().toString());
            initialData.put("type", "setup");

            // Use ApiFuture to wait for the write operation to complete
            ApiFuture<WriteResult> result = docRef.set(initialData);
            result.get(); // Wait for operation to complete
        } catch (Exception e) {
            showError("Failed to create cash collection: " + e.getMessage());
        }
    }

    private void refreshData() {
        try {
            // Execute in background to avoid UI freezing
            new Thread(() -> {
                try {
                    QuerySnapshot inventorySnapshot = firestore.collection("inventory").get().get();
                    QuerySnapshot activitiesSnapshot = loadActivitiesData();

                    // Update UI on the UI thread
                    uiInstance.access(() -> {
                        updateInventoryGrid(inventorySnapshot);
                        updateActivitiesGrid(activitiesSnapshot);
                        updateCategorySummary(inventorySnapshot);
                        updateDashboardSummary(inventorySnapshot, activitiesSnapshot);
                        updateVisualizationCharts(inventorySnapshot, activitiesSnapshot);
                    });
                } catch (Exception e) {
                    uiInstance.access(() -> {
                        showError("Error refreshing data: " + e.getMessage());
                    });
                }
            }).start();
        } catch (Exception e) {
            showError("Failed to refresh data: " + e.getMessage());
        }
    }

    private QuerySnapshot loadActivitiesData() throws Exception {
        // Get date range for filtering
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        // Convert to Firestore date format
        String startDateStr = startDate.toString();
        String endDateStr = endDate.toString();

        // Get activities data using synchronous API with date filtering
        return firestore.collection("activities")
                .whereGreaterThanOrEqualTo("date", startDateStr)
                .whereLessThanOrEqualTo("date", endDateStr)
                .get().get();
    }

    private void updateInventoryGrid(QuerySnapshot querySnapshot) {
        List<InventoryItem> inventoryItems = new ArrayList<>();

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            String id = doc.getId();
            String name = doc.getString("name");
            String category = doc.getString("category");
            Long quantity = doc.getLong("quantity");
            Long reorderLevel = doc.getLong("reorderLevel");
            String unit = doc.getString("unit");
            String lastRestocked = doc.getString("lastRestocked");

            if (name != null && category != null && quantity != null && reorderLevel != null) {
                inventoryItems.add(new InventoryItem(
                        id, name, category, quantity.intValue(), reorderLevel.intValue(),
                        unit != null ? unit : "N/A",
                        lastRestocked != null ? lastRestocked : "Unknown"
                ));
            }
        }

        inventoryGrid.setItems(inventoryItems);
    }

    private void updateActivitiesGrid(QuerySnapshot querySnapshot) {
        List<ActivityItem> activityItems = new ArrayList<>();

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            String id = doc.getId();
            String title = doc.getString("title");
            String type = doc.getString("type");
            String location = doc.getString("location");
            String date = doc.getString("date");

            if (title != null && type != null && date != null) {
                activityItems.add(new ActivityItem(
                        id, title, type,
                        location != null ? location : "N/A",
                        date
                ));
            }
        }

        activitiesGrid.setItems(activityItems);
    }

    private void updateCategorySummary(QuerySnapshot querySnapshot) {
        Map<String, CategorySummary> categorySummaries = new HashMap<>();

        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            String category = doc.getString("category");
            Long quantity = doc.getLong("quantity");
            Long reorderLevel = doc.getLong("reorderLevel");

            if (category != null) {
                CategorySummary summary = categorySummaries.getOrDefault(
                        category, new CategorySummary(category)
                );

                summary.addItem();

                if (quantity != null && reorderLevel != null && quantity < reorderLevel) {
                    summary.addBelowReorder();
                }

                categorySummaries.put(category, summary);
            }
        }

        categorySummaryGrid.setItems(categorySummaries.values());
    }

    private void updateDashboardSummary(QuerySnapshot inventorySnapshot, QuerySnapshot activitiesSnapshot) {
        // Count total inventory items
        totalInventoryCount.setText(String.valueOf(inventorySnapshot.size()));

        // Count activities
        activitiesCount.setText(String.valueOf(activitiesSnapshot.size()));

        // Count items below threshold
        int belowThreshold = 0;
        for (DocumentSnapshot doc : inventorySnapshot.getDocuments()) {
            Long quantity = doc.getLong("quantity");
            Long reorderLevel = doc.getLong("reorderLevel");

            if (quantity != null && reorderLevel != null && quantity < reorderLevel) {
                belowThreshold++;
            }
        }

        inventoryBelowThreshold.setText(String.valueOf(belowThreshold));
    }

    private void updateVisualizationCharts(QuerySnapshot inventorySnapshot, QuerySnapshot activitiesSnapshot) {
        // Create simple visual representations using HTML/CSS since we're not using Vaadin Pro charts

        // 1. Inventory Chart - horizontal bars showing inventory levels
        StringBuilder inventoryHtml = new StringBuilder();
        inventoryHtml.append("<div style='height: 100%; overflow-y: auto; padding: 10px;'>");

        // Get top 5 inventory items
        List<InventoryItem> items = new ArrayList<>();
        for (DocumentSnapshot doc : inventorySnapshot.getDocuments()) {
            String name = doc.getString("name");
            Long quantity = doc.getLong("quantity");
            Long reorderLevel = doc.getLong("reorderLevel");

            if (name != null && quantity != null && reorderLevel != null) {
                items.add(new InventoryItem(
                        doc.getId(), name, "", quantity.intValue(), reorderLevel.intValue(), "", ""
                ));
            }
        }

        // Sort by quantity
        items.sort((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()));

        // Take top 5
        List<InventoryItem> topItems = items.stream().limit(5).collect(Collectors.toList());

        // Find max for scaling
        int maxQuantity = topItems.stream()
                .mapToInt(item -> Math.max(item.getQuantity(), item.getReorderLevel()))
                .max()
                .orElse(1);

        // Create bars
        for (InventoryItem item : topItems) {
            double quantityPercentage = (item.getQuantity() * 100.0) / maxQuantity;
            double reorderPercentage = (item.getReorderLevel() * 100.0) / maxQuantity;

            inventoryHtml.append("<div style='margin-bottom: 15px;'>");
            inventoryHtml.append("<div style='font-weight: bold;'>").append(item.getName()).append("</div>");

            // Quantity bar
            inventoryHtml.append("<div style='display: flex; align-items: center; margin-top: 5px;'>");
            inventoryHtml.append("<div style='width: 80px;'>Quantity:</div>");
            inventoryHtml.append("<div style='height: 20px; background-color: #2196F3; width: ")
                    .append(quantityPercentage)
                    .append("%; position: relative;'></div>");
            inventoryHtml.append("<div style='margin-left: 10px;'>").append(item.getQuantity()).append("</div>");
            inventoryHtml.append("</div>");

            // Reorder level
            inventoryHtml.append("<div style='display: flex; align-items: center; margin-top: 5px;'>");
            inventoryHtml.append("<div style='width: 80px;'>Reorder:</div>");
            inventoryHtml.append("<div style='height: 20px; background-color: #F44336; width: ")
                    .append(reorderPercentage)
                    .append("%; position: relative;'></div>");
            inventoryHtml.append("<div style='margin-left: 10px;'>").append(item.getReorderLevel()).append("</div>");
            inventoryHtml.append("</div>");

            inventoryHtml.append("</div>");
        }

        inventoryHtml.append("</div>");
        inventoryChart.getElement().setProperty("innerHTML", inventoryHtml.toString());

        // 2. Activities Chart - show activity types in a simple visual way
        StringBuilder activityHtml = new StringBuilder();
        activityHtml.append("<div style='height: 100%; padding: 10px;'>");

        // Count activities by type
        Map<String, Integer> typeCounts = new HashMap<>();
        for (DocumentSnapshot doc : activitiesSnapshot.getDocuments()) {
            String type = doc.getString("type");
            if (type != null) {
                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            }
        }

        // Total for percentage calculation
        int totalActivities = typeCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (totalActivities > 0) {
            activityHtml.append("<div style='display: flex; height: 200px; align-items: flex-end;'>");

            // Colors for different bars
            String[] colors = {"#4CAF50", "#2196F3", "#FFC107", "#9C27B0", "#FF5722"};
            int colorIndex = 0;

            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / totalActivities;
                double height = (entry.getValue() * 180.0) / totalActivities;

                String color = colors[colorIndex % colors.length];
                colorIndex++;

                activityHtml.append("<div style='display: flex; flex-direction: column; align-items: center; margin-right: 20px;'>");
                activityHtml.append("<div style='height: ").append(height).append("px; width: 40px; background-color: ")
                        .append(color).append(";'></div>");
                activityHtml.append("<div style='margin-top: 5px; text-align: center; word-break: break-word; width: 60px;'>")
                        .append(entry.getKey()).append("<br>").append(entry.getValue()).append(" (")
                        .append(String.format("%.1f", percentage)).append("%)</div>");
                activityHtml.append("</div>");
            }

            activityHtml.append("</div>");
        } else {
            activityHtml.append("<div style='text-align: center; padding: 50px;'>No activities to display</div>");
        }

        activityHtml.append("</div>");
        activityChart.getElement().setProperty("innerHTML", activityHtml.toString());
    }

    private void showNotification(String message) {
        Notification notification = Notification.show(message);
        notification.setPosition(Notification.Position.TOP_END);
        notification.setDuration(3000);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message);
        notification.getElement().getThemeList().add("error");
        notification.setPosition(Notification.Position.TOP_END);
        notification.setDuration(5000);
    }

    // Data classes
    private static class InventoryItem {
        private final String id;
        private final String name;
        private final String category;
        private final int quantity;
        private final int reorderLevel;
        private final String unit;
        private final String lastRestocked;

        public InventoryItem(String id, String name, String category, int quantity, int reorderLevel, String unit, String lastRestocked) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.quantity = quantity;
            this.reorderLevel = reorderLevel;
            this.unit = unit;
            this.lastRestocked = lastRestocked;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public int getQuantity() { return quantity; }
        public int getReorderLevel() { return reorderLevel; }
        public String getUnit() { return unit; }
        public String getLastRestocked() { return lastRestocked; }
    }

    private static class ActivityItem {
        private final String id;
        private final String title;
        private final String type;
        private final String location;
        private final String date;

        public ActivityItem(String id, String title, String type, String location, String date) {
            this.id = id;
            this.title = title;
            this.type = type;
            this.location = location;
            this.date = date;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getType() { return type; }
        public String getLocation() { return location; }
        public String getDate() { return date; }
    }

    private static class CategorySummary {
        private final String category;
        private int itemCount = 0;
        private int belowReorderCount = 0;

        public CategorySummary(String category) {
            this.category = category;
        }

        public String getCategory() { return category; }
        public int getItemCount() { return itemCount; }
        public int getBelowReorderCount() { return belowReorderCount; }

        public void addItem() { this.itemCount++; }
        public void addBelowReorder() { this.belowReorderCount++; }
    }
}