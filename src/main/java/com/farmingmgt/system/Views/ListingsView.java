package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@Route(value = "listings", layout = VendorLayout.class)
@PageTitle("Inventory Listings")
public class ListingsView extends VerticalLayout {
    private final UI uiInstance;
    private final String vendorId;
    private String farmId;
    private Firestore firestore;
    
    // UI Components
    private Tabs inventoryTabs;
    private Grid<Map<String, Object>> equipmentGrid;
    private Grid<Crop> cropGrid;
    private Grid<InventoryItem> inventoryGrid;
    private Div equipmentContent;
    private Div cropContent;
    private Div inventoryContent;
    private Span equipmentCountLabel = new Span("0 items");
    private Span cropCountLabel = new Span("0 items");
    private Span inventoryCountLabel = new Span("0 items");
    
    // Filters
    private ComboBox<String> equipmentCategoryFilter;
    private ComboBox<String> equipmentStatusFilter;
    private ComboBox<String> cropStatusFilter;
    private ComboBox<String> inventoryCategoryFilter;
    
    // Filter sets
    private Set<String> equipmentCategories = new HashSet<>();
    private Set<String> equipmentStatuses = new HashSet<>();
    private Set<String> cropStatuses = new HashSet<>();
    private Set<String> inventoryCategories = new HashSet<>();

    public ListingsView() {
        this.uiInstance = UI.getCurrent();
        this.vendorId = UserSession.getUserUid();
        this.firestore = FirestoreClient.getFirestore();
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        // Create header
        add(createHeader());
        
        // Create tabbed content
        add(createTabbedContent());
        
        // Fetch farm ID and then load data
        fetchVendorAndFarmData();
    }
    
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        
        H2 title = new H2("Inventory Listings");
        Span subtitle = new Span("List your inventory items for sale");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        VerticalLayout titleLayout = new VerticalLayout(title, subtitle);
        titleLayout.setSpacing(false);
        titleLayout.setPadding(false);
        
        header.add(titleLayout);
        header.expand(titleLayout);
        
        return header;
    }
    
    private VerticalLayout createTabbedContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        
        // Create tabs
        Tab equipmentTab = new Tab("Equipment");
        Tab cropsTab = new Tab("Crops");
        Tab inventoryTab = new Tab("Inventory");
        
        inventoryTabs = new Tabs(equipmentTab, cropsTab, inventoryTab);
        inventoryTabs.setWidthFull();
        
        // Create content areas
        equipmentContent = new Div();
        equipmentContent.setSizeFull();
        equipmentContent.add(createEquipmentContent());
        
        cropContent = new Div();
        cropContent.setSizeFull();
        cropContent.add(createCropContent());
        cropContent.setVisible(false);
        
        inventoryContent = new Div();
        inventoryContent.setSizeFull();
        inventoryContent.add(createInventoryContent());
        inventoryContent.setVisible(false);
        
        // Add tab change listener
        inventoryTabs.addSelectedChangeListener(event -> {
            hideAllTabs();
            if (event.getSelectedTab().equals(equipmentTab)) {
                equipmentContent.setVisible(true);
            } else if (event.getSelectedTab().equals(cropsTab)) {
                cropContent.setVisible(true);
            } else if (event.getSelectedTab().equals(inventoryTab)) {
                inventoryContent.setVisible(true);
            }
        });
        
        layout.add(inventoryTabs, equipmentContent, cropContent, inventoryContent);
        return layout;
    }
    
    private void hideAllTabs() {
        equipmentContent.setVisible(false);
        cropContent.setVisible(false);
        inventoryContent.setVisible(false);
    }
    
    private VerticalLayout createEquipmentContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        
        // Create filter bar
        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        
        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setAlignItems(FlexComponent.Alignment.BASELINE);
        leftSide.add(equipmentCountLabel);
        
        // Create filter components
        equipmentCategoryFilter = new ComboBox<>("Category");
        equipmentCategoryFilter.setPlaceholder("All Categories");
        equipmentCategoryFilter.setClearButtonVisible(true);
        equipmentCategoryFilter.addValueChangeListener(e -> applyEquipmentFilters());
        
        equipmentStatusFilter = new ComboBox<>("Status");
        equipmentStatusFilter.setPlaceholder("All Statuses");
        equipmentStatusFilter.setClearButtonVisible(true);
        equipmentStatusFilter.addValueChangeListener(e -> applyEquipmentFilters());
        
        HorizontalLayout filters = new HorizontalLayout(equipmentCategoryFilter, equipmentStatusFilter);
        filters.setSpacing(true);
        
        Button resetFiltersBtn = new Button("Reset Filters", e -> resetEquipmentFilters());
        resetFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        filters.add(resetFiltersBtn);
        
        filterBar.add(leftSide, filters);
        filterBar.expand(filters);
        
        // Create equipment grid
        equipmentGrid = new Grid<>();
        equipmentGrid.setWidthFull();
        equipmentGrid.setHeight("500px");
        equipmentGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        
        // Configure columns
        equipmentGrid.addColumn(item -> item.getOrDefault("name", ""))
                .setHeader("Equipment Name")
                .setKey("name")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);
        
        equipmentGrid.addColumn(item -> item.getOrDefault("category", ""))
                .setHeader("Category")
                .setKey("category")
                .setAutoWidth(true)
                .setSortable(true);
        
        equipmentGrid.addColumn(item -> formatDate(item.get("purchaseDate")))
                .setHeader("Purchase Date")
                .setKey("purchaseDate")
                .setAutoWidth(true)
                .setSortable(true);
        
        equipmentGrid.addColumn(item -> item.getOrDefault("status", ""))
                .setHeader("Status")
                .setKey("status")
                .setAutoWidth(true)
                .setSortable(true)
                .setRenderer(new ComponentRenderer<>(item -> {
                    String status = (String) item.getOrDefault("status", "Unknown");
                    Span badge = new Span(status);
                    badge.getElement().getThemeList().add("badge");
                    
                    switch (status.toLowerCase()) {
                        case "available":
                            badge.getElement().getThemeList().add("success");
                            break;
                        case "in use":
                            badge.getElement().getThemeList().add("primary");
                            break;
                        case "maintenance":
                            badge.getElement().getThemeList().add("warning");
                            break;
                        case "broken":
                            badge.getElement().getThemeList().add("error");
                            break;
                        default:
                            badge.getElement().getThemeList().add("contrast");
                    }
                    
                    return badge;
                }));
        
        equipmentGrid.addColumn(item -> formatCurrency(item.get("value")))
                .setHeader("Value")
                .setKey("value")
                .setAutoWidth(true)
                .setSortable(true);
        
        // Add action column
        equipmentGrid.addComponentColumn(item -> {
            Button listForSaleBtn = new Button("List for Sale", VaadinIcon.CART.create());
            listForSaleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            listForSaleBtn.addClickListener(e -> openListForSaleDialog("equipment", item));
            return listForSaleBtn;
        }).setHeader("Actions").setAutoWidth(true);
        
        layout.add(filterBar, equipmentGrid);
        return layout;
    }
    
    private VerticalLayout createCropContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        
        // Create filter bar
        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        
        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setAlignItems(FlexComponent.Alignment.BASELINE);
        leftSide.add(cropCountLabel);
        
        // Create filter components
        cropStatusFilter = new ComboBox<>("Status");
        cropStatusFilter.setPlaceholder("All Statuses");
        cropStatusFilter.setClearButtonVisible(true);
        cropStatusFilter.addValueChangeListener(e -> applyCropFilters());
        
        HorizontalLayout filters = new HorizontalLayout(cropStatusFilter);
        filters.setSpacing(true);
        
        Button resetFiltersBtn = new Button("Reset Filters", e -> resetCropFilters());
        resetFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        filters.add(resetFiltersBtn);
        
        filterBar.add(leftSide, filters);
        filterBar.expand(filters);
        
        // Create crop grid
        cropGrid = new Grid<>(Crop.class);
        cropGrid.setWidthFull();
        cropGrid.setHeight("500px");
        cropGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        
        // Configure columns
        cropGrid.setColumns("name", "variety", "fieldLocation", "plantingDate", "expectedHarvestDate", "status");
        cropGrid.getColumnByKey("name").setHeader("Crop Name");
        cropGrid.getColumnByKey("variety").setHeader("Variety");
        cropGrid.getColumnByKey("fieldLocation").setHeader("Field Location");
        cropGrid.getColumnByKey("plantingDate").setHeader("Planting Date");
        cropGrid.getColumnByKey("expectedHarvestDate").setHeader("Expected Harvest");
        cropGrid.getColumnByKey("status").setHeader("Status");
        
        // Add action column
        cropGrid.addComponentColumn(crop -> {
            Button listForSaleBtn = new Button("List for Sale", VaadinIcon.CART.create());
            listForSaleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            listForSaleBtn.addClickListener(e -> openListForSaleDialog("crop", crop));
            return listForSaleBtn;
        }).setHeader("Actions").setAutoWidth(true);
        
        layout.add(filterBar, cropGrid);
        return layout;
    }
    
    private VerticalLayout createInventoryContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setPadding(false);
        
        // Create filter bar
        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        
        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setAlignItems(FlexComponent.Alignment.BASELINE);
        leftSide.add(inventoryCountLabel);
        
        // Create filter components
        inventoryCategoryFilter = new ComboBox<>("Category");
        inventoryCategoryFilter.setPlaceholder("All Categories");
        inventoryCategoryFilter.setClearButtonVisible(true);
        inventoryCategoryFilter.addValueChangeListener(e -> applyInventoryFilters());
        
        HorizontalLayout filters = new HorizontalLayout(inventoryCategoryFilter);
        filters.setSpacing(true);
        
        Button resetFiltersBtn = new Button("Reset Filters", e -> resetInventoryFilters());
        resetFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        filters.add(resetFiltersBtn);
        
        filterBar.add(leftSide, filters);
        filterBar.expand(filters);
        
        // Create inventory grid
        inventoryGrid = new Grid<>(InventoryItem.class);
        inventoryGrid.setWidthFull();
        inventoryGrid.setHeight("500px");
        inventoryGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        
        // Configure columns
        inventoryGrid.setColumns("name", "category", "quantity", "unit", "reorderLevel", "lastRestocked");
        inventoryGrid.getColumnByKey("name").setHeader("Item Name");
        inventoryGrid.getColumnByKey("category").setHeader("Category");
        inventoryGrid.getColumnByKey("quantity").setHeader("Quantity");
        inventoryGrid.getColumnByKey("unit").setHeader("Unit");
        inventoryGrid.getColumnByKey("reorderLevel").setHeader("Reorder Level");
        inventoryGrid.getColumnByKey("lastRestocked").setHeader("Last Restocked");
        
        // Add action column
        inventoryGrid.addComponentColumn(item -> {
            Button listForSaleBtn = new Button("List for Sale", VaadinIcon.CART.create());
            listForSaleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            listForSaleBtn.addClickListener(e -> openListForSaleDialog("inventory", item));
            return listForSaleBtn;
        }).setHeader("Actions").setAutoWidth(true);
        
        layout.add(filterBar, inventoryGrid);
        return layout;
    }
    
    private void fetchVendorAndFarmData() {
        // Fetch vendor data to get the farm ID
        firestore.collection("vendors").document(vendorId).get()
                .addListener(() -> {
                    // Run on UI thread
                    uiInstance.access(() -> {
                        try {
                            DocumentSnapshot vendorDoc = firestore.collection("vendors").document(vendorId).get().get();
                            
                            if (vendorDoc.exists()) {
                                // Extract farmId from vendor document
                                farmId = vendorDoc.getString("farmId");
                                
                                // Load data for all tabs
                                loadEquipmentData();
                                loadCropData();
                                loadInventoryData();
                            } else {
                                showError("Vendor information not found.");
                            }
                        } catch (Exception e) {
                            showError("Error fetching vendor data: " + e.getMessage());
                        }
                    });
                }, Executors.newSingleThreadExecutor());
    }
    
    private void loadEquipmentData() {
        if (farmId == null) return;
        
        // First load initial data
        new Thread(() -> {
            try {
                ApiFuture<QuerySnapshot> future = firestore.collection("equipment")
                        .whereEqualTo("farmId", farmId)
                        .get();
                
                QuerySnapshot snapshot = future.get();
                updateEquipmentGrid(snapshot);
                
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Failed to load equipment data: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }
    
    private void updateEquipmentGrid(QuerySnapshot snapshot) {
        List<Map<String, Object>> equipmentList = new ArrayList<>();
        equipmentCategories.clear();
        equipmentStatuses.clear();
        
        // Add default categories if we're going to have an empty set
        if (snapshot.isEmpty()) {
            // Default equipment categories
            equipmentCategories.add("Tractor");
            equipmentCategories.add("Harvester");
            equipmentCategories.add("Plow");
            equipmentCategories.add("Seeder");
            equipmentCategories.add("Irrigation");
            equipmentCategories.add("Vehicle");
            equipmentCategories.add("Tool");
            equipmentCategories.add("Other");
            
            // Default statuses
            equipmentStatuses.add("Available");
            equipmentStatuses.add("In Use");
            equipmentStatuses.add("Maintenance");
            equipmentStatuses.add("Broken");
        }
        
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Map<String, Object> data = doc.getData();
            // Add document ID to the data for reference
            data.put("id", doc.getId());
            equipmentList.add(data);
            
            // Collect unique categories and statuses for filters
            String category = (String) data.getOrDefault("category", "");
            String status = (String) data.getOrDefault("status", "");
            
            if (!category.isEmpty()) equipmentCategories.add(category);
            if (!status.isEmpty()) equipmentStatuses.add(status);
        }
        
        uiInstance.access(() -> {
            // Update grid data
            equipmentGrid.setItems(equipmentList);
            
            // Update count label
            equipmentCountLabel.setText(equipmentList.size() +
                    (equipmentList.size() == 1 ? " item" : " items"));
            
            // Update filter options
            List<String> sortedCategories = new ArrayList<>(equipmentCategories);
            Collections.sort(sortedCategories);
            equipmentCategoryFilter.setItems(sortedCategories);
            
            List<String> sortedStatuses = new ArrayList<>(equipmentStatuses);
            Collections.sort(sortedStatuses);
            equipmentStatusFilter.setItems(sortedStatuses);
        });
    }
    
    private void loadCropData() {
        if (farmId == null) return;
        
        new Thread(() -> {
            try {
                // Create a query to get all crops for this farm
                Query query = firestore.collection("crops");
                
                // Execute query
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot snapshot = future.get();
                
                // Process results
                List<Crop> crops = new ArrayList<>();
                cropStatuses.clear();
                
                // Add default statuses if empty
                if (snapshot.isEmpty()) {
                    cropStatuses.add("Planted");
                    cropStatuses.add("Growing");
                    cropStatuses.add("Ready for Harvest");
                    cropStatuses.add("Harvested");
                }
                
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Crop crop = doc.toObject(Crop.class);
                    if (crop != null) {
                        crop.setId(doc.getId());
                        crops.add(crop);
                        
                        // Collect unique statuses for filter
                        if (crop.getStatus() != null && !crop.getStatus().isEmpty()) {
                            cropStatuses.add(crop.getStatus());
                        }
                    }
                }
                
                // Update UI
                uiInstance.access(() -> {
                    cropGrid.setItems(crops);
                    cropCountLabel.setText(crops.size() + (crops.size() == 1 ? " crop" : " crops"));
                    
                    // Update filter options
                    List<String> sortedStatuses = new ArrayList<>(cropStatuses);
                    Collections.sort(sortedStatuses);
                    cropStatusFilter.setItems(sortedStatuses);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() -> {
                    Notification.show("Error loading crops: " + e.getMessage(),
                            3000, Notification.Position.BOTTOM_CENTER);
                });
            }
        }).start();
    }
    
    private void loadInventoryData() {
        if (farmId == null) return;
        
        new Thread(() -> {
            try {
                // Query inventory items for this farm
                ApiFuture<QuerySnapshot> future = firestore.collection("inventory")
                        .get();
                
                QuerySnapshot snapshot = future.get();
                List<InventoryItem> inventoryItems = new ArrayList<>();
                inventoryCategories.clear();
                
                // Add default categories if empty
                if (snapshot.isEmpty()) {
                    inventoryCategories.add("Equipment");
                    inventoryCategories.add("Seed");
                    inventoryCategories.add("Feed");
                    inventoryCategories.add("Fertilizer");
                    inventoryCategories.add("Chemical");
                    inventoryCategories.add("Other");
                }
                
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    InventoryItem item = doc.toObject(InventoryItem.class);
                    if (item != null) {
                        item.setId(doc.getId());
                        inventoryItems.add(item);
                        
                        // Collect unique categories for filter
                        if (item.getCategory() != null && !item.getCategory().isEmpty()) {
                            inventoryCategories.add(item.getCategory());
                        }
                    }
                }
                
                // Update UI
                uiInstance.access(() -> {
                    inventoryGrid.setItems(inventoryItems);
                    inventoryCountLabel.setText(inventoryItems.size() + 
                            (inventoryItems.size() == 1 ? " item" : " items"));
                    
                    // Update filter options
                    List<String> sortedCategories = new ArrayList<>(inventoryCategories);
                    Collections.sort(sortedCategories);
                    inventoryCategoryFilter.setItems(sortedCategories);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() -> {
                    Notification.show("Error loading inventory: " + e.getMessage(),
                            3000, Notification.Position.BOTTOM_CENTER);
                });
            }
        }).start();
    }
    
    private void applyEquipmentFilters() {
        String selectedCategory = equipmentCategoryFilter.getValue();
        String selectedStatus = equipmentStatusFilter.getValue();
        
        // Create a query based on filters
        new Thread(() -> {
            try {
                Query query = firestore.collection("equipment").whereEqualTo("farmId", farmId);
                
                // Add category filter if selected
                if (selectedCategory != null && !selectedCategory.isEmpty()) {
                    query = query.whereEqualTo("category", selectedCategory);
                }
                
                // Add status filter if selected
                if (selectedStatus != null && !selectedStatus.isEmpty()) {
                    query = query.whereEqualTo("status", selectedStatus);
                }
                
                // Execute query
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot snapshot = future.get();
                
                // Update UI with filtered results
                List<Map<String, Object>> filteredList = new ArrayList<>();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    data.put("id", doc.getId());
                    filteredList.add(data);
                }
                
                uiInstance.access(() -> {
                    equipmentGrid.setItems(filteredList);
                    equipmentCountLabel.setText(filteredList.size() +
                            (filteredList.size() == 1 ? " item" : " items"));
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Error applying filters: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }
    
    private void applyCropFilters() {
        String selectedStatus = cropStatusFilter.getValue();
        
        // Create a query based on filters
        new Thread(() -> {
            try {
                Query query = firestore.collection("crops").whereEqualTo("farmId", farmId);
                
                // Add status filter if selected
                if (selectedStatus != null && !selectedStatus.isEmpty()) {
                    query = query.whereEqualTo("status", selectedStatus);
                }
                
                // Execute query
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot snapshot = future.get();

                // Process results
                List<Crop> filteredCrops = new ArrayList<>();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Crop crop = doc.toObject(Crop.class);
                    if (crop != null) {
                        crop.setId(doc.getId());
                        filteredCrops.add(crop);
                    }
                }

                // Update UI
                uiInstance.access(() -> {
                    cropGrid.setItems(filteredCrops);
                    cropCountLabel.setText(filteredCrops.size() +
                            (filteredCrops.size() == 1 ? " crop" : " crops"));
                });

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Error applying filters: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void applyInventoryFilters() {
        String selectedCategory = inventoryCategoryFilter.getValue();

        // Create a query based on filters
        new Thread(() -> {
            try {
                Query query = firestore.collection("inventory").whereEqualTo("farmId", farmId);

                // Add category filter if selected
                if (selectedCategory != null && !selectedCategory.isEmpty()) {
                    query = query.whereEqualTo("category", selectedCategory);
                }

                // Execute query
                ApiFuture<QuerySnapshot> future = query.get();
                QuerySnapshot snapshot = future.get();

                // Process results
                List<InventoryItem> filteredItems = new ArrayList<>();
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    InventoryItem item = doc.toObject(InventoryItem.class);
                    if (item != null) {
                        item.setId(doc.getId());
                        filteredItems.add(item);
                    }
                }

                // Update UI
                uiInstance.access(() -> {
                    inventoryGrid.setItems(filteredItems);
                    inventoryCountLabel.setText(filteredItems.size() +
                            (filteredItems.size() == 1 ? " item" : " items"));
                });

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Error applying filters: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void resetEquipmentFilters() {
        equipmentCategoryFilter.clear();
        equipmentStatusFilter.clear();
        loadEquipmentData();
    }

    private void resetCropFilters() {
        cropStatusFilter.clear();
        loadCropData();
    }

    private void resetInventoryFilters() {
        inventoryCategoryFilter.clear();
        loadInventoryData();
    }

    private void openListForSaleDialog(String itemType, Object item) {
        Dialog dialog = new Dialog();
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("600px");

        // Create dialog content
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        H3 title = new H3("List Item for Sale");

        // Item details
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));

        // Common fields for all item types
        TextField nameField = new TextField("Item Name");
        nameField.setWidthFull();
        nameField.setReadOnly(true);

        TextField categoryField = new TextField("Category");
        categoryField.setWidthFull();
        categoryField.setReadOnly(true);

        BigDecimalField priceField = new BigDecimalField("Listing Price");
        priceField.setWidthFull();
        priceField.setRequiredIndicatorVisible(true);
        priceField.setValue(BigDecimal.ZERO);
       // priceField.setMin(BigDecimal.ZERO);

        TextField currencyField = new TextField("Currency");
        currencyField.setWidthFull();
        currencyField.setValue("USD");

        NumberField quantityField = new NumberField("Quantity Available");
        quantityField.setWidthFull();
        quantityField.setRequiredIndicatorVisible(true);
        quantityField.setMin(1);
        quantityField.setValue(1.0);

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setHeight("150px");

        DatePicker availableDateField = new DatePicker("Available Date");
        availableDateField.setWidthFull();
        availableDateField.setValue(LocalDate.now());

        // Populate fields based on item type
        if ("equipment".equals(itemType)) {
            Map<String, Object> equipment = (Map<String, Object>) item;
            nameField.setValue((String) equipment.getOrDefault("name", ""));
            categoryField.setValue((String) equipment.getOrDefault("category", ""));

            // Set suggested price based on equipment value
            Object valueObj = equipment.get("value");
            if (valueObj instanceof Number) {
                Double value = ((Number) valueObj).doubleValue();
                priceField.setValue(BigDecimal.valueOf(value));
            }

            descriptionField.setValue((String) equipment.getOrDefault("description", ""));

            // For equipment, quantity is typically 1
            quantityField.setValue(1.0);
            quantityField.setReadOnly(true);

        } else if ("crop".equals(itemType)) {
            Crop crop = (Crop) item;
            nameField.setValue(crop.getName() != null ? crop.getName() : "");
            categoryField.setValue("Crop: " + (crop.getVariety() != null ? crop.getVariety() : ""));

            // For crops, suggest a reasonable price
            priceField.setValue(BigDecimal.valueOf(100.00));

            descriptionField.setValue(crop.getNotes() != null ? crop.getNotes() : "");

            // For crops, set available date to expected harvest date if it exists
            if (crop.getExpectedHarvestDate() != null && !crop.getExpectedHarvestDate().isEmpty()) {
                try {
                    LocalDate harvestDate = LocalDate.parse(crop.getExpectedHarvestDate());
                    availableDateField.setValue(harvestDate);
                } catch (Exception e) {
                    // Use current date if parsing fails
                    availableDateField.setValue(LocalDate.now());
                }
            }

        } else if ("inventory".equals(itemType)) {
            InventoryItem inventoryItem = (InventoryItem) item;
            nameField.setValue(inventoryItem.getName() != null ? inventoryItem.getName() : "");
            categoryField.setValue(inventoryItem.getCategory() != null ? inventoryItem.getCategory() : "");

            // Set suggested price (placeholder - would be based on actual cost in a real system)
            priceField.setValue(BigDecimal.valueOf(25.00));

            // For inventory, set quantity to available quantity but allow editing
            if (inventoryItem.getQuantity() > 0) {
                quantityField.setValue((double) inventoryItem.getQuantity());
                quantityField.setMax((double) inventoryItem.getQuantity());
            }

            // Unit information
            if (inventoryItem.getUnit() != null && !inventoryItem.getUnit().isEmpty()) {
                descriptionField.setValue("Unit: " + inventoryItem.getUnit());
            }
        }

        // Add all fields to form
        formLayout.add(nameField, categoryField, priceField, currencyField,
                quantityField, availableDateField, descriptionField);

        // Make description field span full width
        formLayout.setColspan(descriptionField, 2);

        // Create buttons
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button listButton = new Button("List for Sale", e -> {
            if (priceField.getValue() == null || priceField.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                Notification.show("Please enter a valid price greater than zero",
                        3000, Notification.Position.MIDDLE);
                return;
            }

            if (quantityField.getValue() == null || quantityField.getValue() <= 0) {
                Notification.show("Please enter a valid quantity greater than zero",
                        3000, Notification.Position.MIDDLE);
                return;
            }

            createListing(itemType, item,
                    priceField.getValue().doubleValue(),
                    currencyField.getValue(),
                    quantityField.getValue().intValue(),
                    descriptionField.getValue(),
                    availableDateField.getValue());
            dialog.close();
        });
        listButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, listButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        // Add all components to dialog
        content.add(title, formLayout, buttonLayout);
        dialog.add(content);

        dialog.open();
    }

    private void createListing(String itemType, Object item, double price, String currency,
                               int quantity, String description, LocalDate availableDate) {
        // Create a new marketplace listing in Firestore
        Map<String, Object> listing = new HashMap<>();
        listing.put("vendorId", vendorId);
        listing.put("farmId", farmId);
        listing.put("itemType", itemType);
        listing.put("price", price);
        listing.put("currency", currency);
        listing.put("quantity", quantity);
        listing.put("availableQuantity", quantity);
        listing.put("description", description != null ? description : "");
        listing.put("availableDate", availableDate.toString());
        listing.put("createdAt", Timestamp.now());
        listing.put("status", "Active");

        // Add specific item details based on type
        if ("equipment".equals(itemType)) {
            Map<String, Object> equipment = (Map<String, Object>) item;
            listing.put("itemId", equipment.get("id"));
            listing.put("name", equipment.getOrDefault("name", ""));
            listing.put("category", equipment.getOrDefault("category", ""));
            listing.put("manufacturer", equipment.getOrDefault("manufacturer", ""));
            listing.put("imageUrl", ""); // Placeholder for future image upload feature

        } else if ("crop".equals(itemType)) {
            Crop crop = (Crop) item;
            listing.put("itemId", crop.getId());
            listing.put("name", crop.getName());
            listing.put("category", "Crop");
            listing.put("variety", crop.getVariety());
            listing.put("harvestDate", crop.getExpectedHarvestDate());
            listing.put("imageUrl", ""); // Placeholder for future image upload feature

        } else if ("inventory".equals(itemType)) {
            InventoryItem inventoryItem = (InventoryItem) item;
            listing.put("itemId", inventoryItem.getId());
            listing.put("name", inventoryItem.getName());
            listing.put("category", inventoryItem.getCategory());
            listing.put("unit", inventoryItem.getUnit());
            listing.put("imageUrl", ""); // Placeholder for future image upload feature
        }

        // Add listing to Firestore
        new Thread(() -> {
            try {
                // Add document to marketplace collection
                ApiFuture<DocumentReference> future =
                        firestore.collection("marketplace").add(listing);

                // Get document ID of new listing
                DocumentReference docRef = future.get();
                String listingId = docRef.getId();

                // Update UI
                uiInstance.access(() -> {
                    Notification notification = new Notification(
                            "Item successfully listed in marketplace",
                            3000,
                            Notification.Position.MIDDLE);
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.open();

                    // You could redirect to a marketplace view here
                    // UI.getCurrent().navigate(MarketplaceView.class);
                });

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() -> {
                    Notification notification = new Notification(
                            "Failed to create listing: " + e.getMessage(),
                            3000,
                            Notification.Position.MIDDLE);
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    notification.open();
                });
            }
        }).start();
    }

    private String formatDate(Object dateObj) {
        if (dateObj == null) return "";

        if (dateObj instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) dateObj;
            return DateTimeFormatter.ofPattern("MMM d, yyyy")
                    .format(LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestamp.getSeconds() * 1000),
                            ZoneId.systemDefault()));
        } else if (dateObj instanceof String) {
            // Try to parse string date
            try {
                return DateTimeFormatter.ofPattern("MMM d, yyyy")
                        .format(LocalDate.parse((String) dateObj));
            } catch (Exception e) {
                return (String) dateObj;
            }
        }

        return dateObj.toString();
    }

    private String formatCurrency(Object valueObj) {
        if (valueObj == null) return "$0.00";

        if (valueObj instanceof Number) {
            NumberFormat formatter = NumberFormat.getCurrencyInstance();
            return formatter.format(((Number) valueObj).doubleValue());
        }

        return "$0.00";
    }

    private void showError(String message) {
        Notification notification = new Notification(message, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.open();
    }

    // Model classes

    public static class Crop {
        private String id;
        private String name;
        private String variety;
        private String fieldLocation;
        private String plantingDate;
        private String expectedHarvestDate;
        private String status;
        private String notes;

        // Default constructor required for Firestore
        public Crop() {}

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVariety() { return variety; }
        public void setVariety(String variety) { this.variety = variety; }

        public String getFieldLocation() { return fieldLocation; }
        public void setFieldLocation(String fieldLocation) { this.fieldLocation = fieldLocation; }

        public String getPlantingDate() { return plantingDate; }
        public void setPlantingDate(String plantingDate) { this.plantingDate = plantingDate; }

        public String getExpectedHarvestDate() { return expectedHarvestDate; }
        public void setExpectedHarvestDate(String expectedHarvestDate) { this.expectedHarvestDate = expectedHarvestDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class InventoryItem {
        private String id;
        private String name;
        private String category;
        private int quantity;
        private String unit;
        private int reorderLevel;
        private String lastRestocked;
        private String supplier;

        // Default constructor required for Firestore
        public InventoryItem() {}

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public int getReorderLevel() { return reorderLevel; }
        public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

        public String getLastRestocked() { return lastRestocked; }
        public void setLastRestocked(String lastRestocked) { this.lastRestocked = lastRestocked; }

        public String getSupplier() { return supplier; }
        public void setSupplier(String supplier) { this.supplier = supplier; }
    }
}