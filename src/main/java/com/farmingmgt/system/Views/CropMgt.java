package com.farmingmgt.system.Views;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Route(value = "crops", layout = ManagerLayout.class)
@PageTitle("Crops")
public class CropMgt extends VerticalLayout {
    private final UI uiInstance;
    private final Firestore firestore;
    private Grid<Crop> cropGrid;
    private Registration cropListenerRegistration;
    private ComboBox<String> statusFilter;
    private ComboBox<String> fieldFilter;
    private Set<String> statuses = new HashSet<>();
    private Set<String> fields = new HashSet<>();
    private Span cropCountLabel = new Span("0 crops");

    public CropMgt() {
        this.uiInstance = UI.getCurrent();
        this.firestore = FirestoreClient.getFirestore();
        
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        
        add(createHeader());
        add(createActionBar());
        add(createCropGrid());
        
        // Load data
        loadCropData();
    }
    
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        
        H2 title = new H2("Crop Management");
        Span subtitle = new Span("Manage your farm's crops");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        VerticalLayout titleLayout = new VerticalLayout(title, subtitle);
        titleLayout.setSpacing(false);
        titleLayout.setPadding(false);
        
        header.add(titleLayout);
        header.expand(titleLayout);
        
        return header;
    }
    
    private HorizontalLayout createActionBar() {
        HorizontalLayout actionBar = new HorizontalLayout();
        actionBar.setWidthFull();
        actionBar.setPadding(false);
        actionBar.setAlignItems(FlexComponent.Alignment.CENTER);
        
        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setAlignItems(FlexComponent.Alignment.BASELINE);
        leftSide.add(cropCountLabel);
        
        // Create filter components
        statusFilter = new ComboBox<>("Status");
        statusFilter.setPlaceholder("All Statuses");
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> applyFilters());
        
        fieldFilter = new ComboBox<>("Field");
        fieldFilter.setPlaceholder("All Fields");
        fieldFilter.setClearButtonVisible(true);
        fieldFilter.addValueChangeListener(e -> applyFilters());
        
        HorizontalLayout filters = new HorizontalLayout(statusFilter, fieldFilter);
        filters.setSpacing(true);
        
        Button resetFiltersBtn = new Button("Reset Filters", e -> resetFilters());
        resetFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        filters.add(resetFiltersBtn);
        
        Button addCropBtn = new Button("Add Crop", VaadinIcon.PLUS.create());
        addCropBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addCropBtn.addClickListener(e -> openAddCropDialog());
        
        actionBar.add(leftSide, filters, addCropBtn);
        actionBar.expand(filters);
        
        return actionBar;
    }
    
    private Grid<Crop> createCropGrid() {
        cropGrid = new Grid<>();
        cropGrid.setWidthFull();
        cropGrid.setHeight("600px");
        cropGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        
        // Configure columns
        cropGrid.addColumn(Crop::getName)
                .setHeader("Crop Name")
                .setKey("name")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);
        
        cropGrid.addColumn(Crop::getVariety)
                .setHeader("Variety")
                .setKey("variety")
                .setAutoWidth(true)
                .setSortable(true);
        
        cropGrid.addColumn(Crop::getFieldLocation)
                .setHeader("Field Location")
                .setKey("fieldLocation")
                .setAutoWidth(true)
                .setSortable(true);
        
        cropGrid.addColumn(Crop::getPlantingDate)
                .setHeader("Planting Date")
                .setKey("plantingDate")
                .setAutoWidth(true)
                .setSortable(true);
        
        cropGrid.addColumn(Crop::getExpectedHarvestDate)
                .setHeader("Expected Harvest")
                .setKey("expectedHarvestDate")
                .setAutoWidth(true)
                .setSortable(true);
        
        cropGrid.addColumn(Crop::getStatus)
                .setHeader("Status")
                .setKey("status")
                .setAutoWidth(true)
                .setSortable(true)
                .setRenderer(new ComponentRenderer<>(crop -> {
                    String status = crop.getStatus();
                    Span badge = new Span(status);
                    badge.getElement().getThemeList().add("badge");
                    
                    switch (status.toLowerCase()) {
                        case "planted":
                            badge.getElement().getThemeList().add("success");
                            break;
                        case "growing":
                            badge.getElement().getThemeList().add("primary");
                            break;
                        case "ready for harvest":
                            badge.getElement().getThemeList().add("contrast");
                            break;
                        case "harvested":
                            badge.getElement().getThemeList().add("success");
                            break;
                        default:
                            badge.getElement().getThemeList().add("contrast");
                    }
                    
                    return badge;
                }));
        
        // Add action column
        cropGrid.addColumn(new ComponentRenderer<>(crop -> {
            HorizontalLayout actions = new HorizontalLayout();
            
            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editBtn.addClickListener(e -> openEditCropDialog(crop));
            
            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> confirmDelete(crop));
            
            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Actions").setAutoWidth(true);
        
        return cropGrid;
    }
    
    private void loadCropData() {
        try {
            // Set up real-time listener for crops collection
            setupCropListener();
        } catch (Exception e) {
            Notification.show("Error loading crop data: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void setupCropListener() {
        if (cropListenerRegistration != null) {
            cropListenerRegistration.remove();
        }
        
        ListenerRegistration registration = firestore.collection("crops")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        uiInstance.access(() -> {
                            Notification.show("Error loading crops: " + error.getMessage())
                                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        });
                        return;
                    }
                    
                    if (snapshots != null) {
                        List<Crop> crops = new ArrayList<>();
                        statuses.clear();
                        fields.clear();
                        
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Crop crop = doc.toObject(Crop.class);
                            if (crop != null) {
                                crop.setId(doc.getId());
                                crops.add(crop);
                                
                                // Collect unique statuses and fields for filters
                                if (crop.getStatus() != null) {
                                    statuses.add(crop.getStatus());
                                }
                                if (crop.getFieldLocation() != null) {
                                    fields.add(crop.getFieldLocation());
                                }
                            }
                        }
                        
                        uiInstance.access(() -> {
                            cropGrid.setItems(crops);
                            cropCountLabel.setText(crops.size() + " crops");
                            
                            // Update filter options
                            statusFilter.setItems(statuses);
                            fieldFilter.setItems(fields);
                        });
                    }
                });
        
        // Store registration for cleanup
        this.cropListenerRegistration = () -> registration.remove();
    }
    
    private void applyFilters() {
        try {
            Query query = firestore.collection("crops");
            
            // Apply status filter if selected
            if (statusFilter.getValue() != null) {
                query = query.whereEqualTo("status", statusFilter.getValue());
            }
            
            // Apply field filter if selected
            if (fieldFilter.getValue() != null) {
                query = query.whereEqualTo("fieldLocation", fieldFilter.getValue());
            }
            
            // Execute query
            ApiFuture<QuerySnapshot> future = query.get();
            List<Crop> filteredCrops = new ArrayList<>();
            
            for (DocumentSnapshot doc : future.get().getDocuments()) {
                Crop crop = doc.toObject(Crop.class);
                if (crop != null) {
                    crop.setId(doc.getId());
                    filteredCrops.add(crop);
                }
            }
            
            cropGrid.setItems(filteredCrops);
            cropCountLabel.setText(filteredCrops.size() + " crops");
            
        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error applying filters: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void resetFilters() {
        statusFilter.clear();
        fieldFilter.clear();
        
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("crops").get();
            List<Crop> crops = new ArrayList<>();
            
            for (DocumentSnapshot doc : future.get().getDocuments()) {
                Crop crop = doc.toObject(Crop.class);
                if (crop != null) {
                    crop.setId(doc.getId());
                    crops.add(crop);
                }
            }
            
            cropGrid.setItems(crops);
            cropCountLabel.setText(crops.size() + " crops");
            
        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error resetting filters: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void openAddCropDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Crop");
        
        // Form fields
        TextField nameField = new TextField("Crop Name");
        nameField.setWidthFull();
        nameField.setRequired(true);
        
        TextField varietyField = new TextField("Variety");
        varietyField.setWidthFull();
        
        TextField fieldLocationField = new TextField("Field Location");
        fieldLocationField.setWidthFull();
        fieldLocationField.setRequired(true);
        
        DatePicker plantingDateField = new DatePicker("Planting Date");
        plantingDateField.setWidthFull();
        plantingDateField.setValue(LocalDate.now());
        plantingDateField.setRequired(true);
        
        DatePicker expectedHarvestDateField = new DatePicker("Expected Harvest Date");
        expectedHarvestDateField.setWidthFull();
        expectedHarvestDateField.setValue(LocalDate.now().plusMonths(3)); // Default to 3 months later
        expectedHarvestDateField.setRequired(true);
        
        ComboBox<String> statusField = new ComboBox<>("Status");
        statusField.setItems("Planted", "Growing", "Ready for Harvest", "Harvested");
        statusField.setValue("Planted");
        statusField.setWidthFull();
        statusField.setRequired(true);
        
        TextArea notesField = new TextArea("Notes");
        notesField.setWidthFull();
        notesField.setHeight("100px");
        
        // Buttons
        Button saveButton = new Button("Save", e -> {
            if (nameField.isEmpty() || fieldLocationField.isEmpty() || 
                plantingDateField.isEmpty() || expectedHarvestDateField.isEmpty() || 
                statusField.isEmpty()) {
                Notification.show("Please fill in all required fields")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            // Create new crop
            Crop newCrop = new Crop();
            newCrop.setName(nameField.getValue());
            newCrop.setVariety(varietyField.getValue());
            newCrop.setFieldLocation(fieldLocationField.getValue());
            newCrop.setPlantingDate(plantingDateField.getValue().toString());
            newCrop.setExpectedHarvestDate(expectedHarvestDateField.getValue().toString());
            newCrop.setStatus(statusField.getValue());
            newCrop.setNotes(notesField.getValue());
            
            // Save to Firestore
            saveCrop(newCrop);
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        VerticalLayout dialogLayout = new VerticalLayout(
                nameField, varietyField, fieldLocationField,
                plantingDateField, expectedHarvestDateField, statusField,
                notesField, buttonLayout);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialogLayout.setSizeFull();
        
        dialog.add(dialogLayout);
        dialog.setWidth("500px");
        dialog.open();
    }
    
    private void openEditCropDialog(Crop crop) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Crop");
        
        // Form fields with existing values
        TextField nameField = new TextField("Crop Name", crop.getName());
        nameField.setWidthFull();
        nameField.setRequired(true);
        
        TextField varietyField = new TextField("Variety", crop.getVariety());
        varietyField.setWidthFull();
        
        TextField fieldLocationField = new TextField("Field Location", crop.getFieldLocation());
        fieldLocationField.setWidthFull();
        fieldLocationField.setRequired(true);
        
        DatePicker plantingDateField = new DatePicker("Planting Date");
        plantingDateField.setWidthFull();
        plantingDateField.setRequired(true);
        if (crop.getPlantingDate() != null) {
            plantingDateField.setValue(LocalDate.parse(crop.getPlantingDate()));
        }
        
        DatePicker expectedHarvestDateField = new DatePicker("Expected Harvest Date");
        expectedHarvestDateField.setWidthFull();
        expectedHarvestDateField.setRequired(true);
        if (crop.getExpectedHarvestDate() != null) {
            expectedHarvestDateField.setValue(LocalDate.parse(crop.getExpectedHarvestDate()));
        }
        
        ComboBox<String> statusField = new ComboBox<>("Status");
        statusField.setItems("Planted", "Growing", "Ready for Harvest", "Harvested");
        statusField.setValue(crop.getStatus());
        statusField.setWidthFull();
        statusField.setRequired(true);
        
        TextArea notesField = new TextArea("Notes", crop.getNotes());
        notesField.setWidthFull();
        notesField.setHeight("100px");
        
        // Buttons
        Button saveButton = new Button("Save", e -> {
            if (nameField.isEmpty() || fieldLocationField.isEmpty() || 
                plantingDateField.isEmpty() || expectedHarvestDateField.isEmpty() || 
                statusField.isEmpty()) {
                Notification.show("Please fill in all required fields")
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            // Update crop
            crop.setName(nameField.getValue());
            crop.setVariety(varietyField.getValue());
            crop.setFieldLocation(fieldLocationField.getValue());
            crop.setPlantingDate(plantingDateField.getValue().toString());
            crop.setExpectedHarvestDate(expectedHarvestDateField.getValue().toString());
            crop.setStatus(statusField.getValue());
            crop.setNotes(notesField.getValue());
            
            // Save to Firestore
            updateCrop(crop);
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        VerticalLayout dialogLayout = new VerticalLayout(
                nameField, varietyField, fieldLocationField,
                plantingDateField, expectedHarvestDateField, statusField,
                notesField, buttonLayout);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialogLayout.setSizeFull();
        
        dialog.add(dialogLayout);
        dialog.setWidth("500px");
        dialog.open();
    }
    
    private void saveCrop(Crop crop) {
        try {
            // Add to Firestore
            DocumentReference docRef = firestore.collection("crops").document();
            ApiFuture<WriteResult> result = docRef.set(crop);
            
            result.get(); // Wait for operation to complete
            Notification.show("Crop added successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error adding crop: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void updateCrop(Crop crop) {
        try {
            // Update in Firestore
            DocumentReference docRef = firestore.collection("crops").document(crop.getId());
            ApiFuture<WriteResult> result = docRef.set(crop);
            
            result.get(); // Wait for operation to complete
            Notification.show("Crop updated successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error updating crop: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void confirmDelete(Crop crop) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm Delete");
        dialog.setText("Are you sure you want to delete the crop \"" + crop.getName() + "\"?");
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");
        
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> deleteCrop(crop));
        
        dialog.open();
    }
    
    private void deleteCrop(Crop crop) {
        try {
            // Delete from Firestore
            DocumentReference docRef = firestore.collection("crops").document(crop.getId());
            ApiFuture<WriteResult> result = docRef.delete();
            
            result.get(); // Wait for operation to complete
            Notification.show("Crop deleted successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error deleting crop: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}


