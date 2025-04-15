package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "equipment", layout = ManagerLayout.class)
@PageTitle("Equipment Management")
public class Equipment extends VerticalLayout {
    private final UI uiInstance;
    private String farmId;
    private Grid<Map<String, Object>> equipmentGrid = new Grid<>();
    private Text headerInfo = new Text("Loading equipment data...");
    private Span equipmentCountLabel = new Span("0 items");
    private Registration equipmentListenerRegistration;
    private ComboBox<String> categoryFilter;
    private ComboBox<String> statusFilter;
    private Set<String> categories = new HashSet<>();
    private Set<String> statuses = new HashSet<>();

    public Equipment() {
        this.uiInstance = UI.getCurrent();

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(createHeader());
        add(createActionBar());
        add(createEquipmentGrid());

        // Placeholder until data loads
        Div loadingIndicator = new Div(new Span("Loading equipment data..."));
        loadingIndicator.addClassName("loading-indicator");
        add(loadingIndicator);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Load farm ID first, then equipment data
        loadManagerDetails();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Clean up listeners
        if (equipmentListenerRegistration != null) {
            equipmentListenerRegistration.remove();
        }
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        H2 title = new H2("Equipment Management");
        header.add(title, headerInfo);
        header.expand(title);

        return header;
    }

    private HorizontalLayout createActionBar() {
        HorizontalLayout actionBar = new HorizontalLayout();
        actionBar.setWidthFull();
        actionBar.setPadding(false);
        actionBar.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setAlignItems(FlexComponent.Alignment.BASELINE);
        leftSide.add(equipmentCountLabel);

        // Create filter components
        categoryFilter = new ComboBox<>("Category");
        categoryFilter.setPlaceholder("All Categories");
        categoryFilter.setClearButtonVisible(true);
        categoryFilter.addValueChangeListener(e -> applyFilters());

        statusFilter = new ComboBox<>("Status");
        statusFilter.setPlaceholder("All Statuses");
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> applyFilters());

        HorizontalLayout filters = new HorizontalLayout(categoryFilter, statusFilter);
        filters.setSpacing(true);

        Button resetFiltersBtn = new Button("Reset Filters", e -> resetFilters());
        resetFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        filters.add(resetFiltersBtn);

        Button addEquipmentBtn = new Button("Add Equipment", VaadinIcon.PLUS.create());
        addEquipmentBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addEquipmentBtn.addClickListener(e -> openAddEquipmentDialog());

        actionBar.add(leftSide, filters, addEquipmentBtn);
        actionBar.expand(filters);

        return actionBar;
    }

    private Component createEquipmentGrid() {
        equipmentGrid.setWidthFull();
        equipmentGrid.setHeight("600px");
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

        equipmentGrid.addColumn(item -> item.getOrDefault("assignedTo", ""))
                .setHeader("Assigned To")
                .setKey("assignedTo")
                .setAutoWidth(true);

        equipmentGrid.addColumn(item -> formatCurrency(item.get("value")))
                .setHeader("Value")
                .setKey("value")
                .setAutoWidth(true)
                .setSortable(true);

        // Add action column
        equipmentGrid.addComponentColumn(item -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button assignBtn = new Button(VaadinIcon.USER_CARD.create());
            assignBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            assignBtn.setTooltipText("Assign/Unassign");
            assignBtn.addClickListener(e -> openAssignDialog(item));

            Button statusBtn = new Button(VaadinIcon.PENCIL.create());
            statusBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            statusBtn.setTooltipText("Change Status");
            statusBtn.addClickListener(e -> openChangeStatusDialog(item));

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editBtn.setTooltipText("Edit Details");
            editBtn.addClickListener(e -> openEditDialog(item));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteBtn.setTooltipText("Delete");
            deleteBtn.addClickListener(e -> confirmDelete(item));

            actions.add(assignBtn, statusBtn, editBtn, deleteBtn);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        return equipmentGrid;
    }

    private void loadManagerDetails() {
        String currentUid = UserSession.getUserUid();
        Firestore db = FirestoreClient.getFirestore();

        new Thread(() -> {
            try {
                ApiFuture<QuerySnapshot> future = db.collection("farm_managers")
                        .whereEqualTo("uid", currentUid)
                        .get();

                QuerySnapshot managerSnapshot = future.get();
                if (!managerSnapshot.isEmpty()) {
                    DocumentSnapshot managerDoc = managerSnapshot.getDocuments().get(0);
                    String farmOwnerId = managerDoc.getString("farmOwnerId");
                    this.farmId = farmOwnerId;

                    DocumentSnapshot farmDoc = db.collection("farm_settings")
                            .document(farmOwnerId)
                            .get()
                            .get();

                    String farmName = farmDoc.getString("farmName");

                    uiInstance.access(() -> {
                        headerInfo.setText("Farm: " + (farmName != null ? farmName : "Unnamed Farm"));
                        // Remove loading placeholder
                        getChildren().forEach(component -> {
                            if (component instanceof Div && ((Div) component).hasClassName("loading-indicator")) {
                                remove(component);
                            }
                        });
                    });

                    // Now load equipment data
                    loadEquipmentData();
                } else {
                    uiInstance.access(() -> {
                        headerInfo.setText("Manager not found - cannot load equipment");
                        Notification.show("Error: Manager not found", 3000, Notification.Position.BOTTOM_CENTER);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() -> {
                    headerInfo.setText("Error loading farm details");
                    Notification.show("Error: " + e.getMessage(), 3000, Notification.Position.BOTTOM_CENTER);
                });
            }
        }).start();
    }

    private void loadEquipmentData() {
        if (farmId == null) return;

        Firestore db = FirestoreClient.getFirestore();

        // First load initial data
        new Thread(() -> {
            try {
                ApiFuture<QuerySnapshot> future = db.collection("equipment")
                        .whereEqualTo("farmId", farmId)
                        .get();

                QuerySnapshot snapshot = future.get();
                updateEquipmentGrid(snapshot);

                // Then set up real-time listener
                setupEquipmentListener(db);

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Failed to load equipment data: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void setupEquipmentListener(Firestore db) {
        // Set up real-time listener for equipment collection
        ListenerRegistration registration = db.collection("equipment")
                .whereEqualTo("farmId", farmId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        System.err.println("Equipment listener error: " + error);
                        return;
                    }

                    updateEquipmentGrid(snapshots);
                });

        // Store registration for cleanup
        this.equipmentListenerRegistration = () -> registration.remove();
    }

    private void updateEquipmentGrid(QuerySnapshot snapshot) {
        List<Map<String, Object>> equipmentList = new ArrayList<>();
        categories.clear();
        statuses.clear();

        // Add default categories if we're going to have an empty set
        if (snapshot.isEmpty()) {
            // Default equipment categories
            categories.add("Tractor");
            categories.add("Harvester");
            categories.add("Plow");
            categories.add("Seeder");
            categories.add("Irrigation");
            categories.add("Vehicle");
            categories.add("Tool");
            categories.add("Other");
            
            // Default statuses are already set in the ComboBox in openAddEquipmentDialog
            // but we'll add them here for the filters
            statuses.add("Available");
            statuses.add("In Use");
            statuses.add("Maintenance");
            statuses.add("Broken");
        }

        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Map<String, Object> data = doc.getData();
            // Add document ID to the data for reference
            data.put("id", doc.getId());
            equipmentList.add(data);

            // Collect unique categories and statuses for filters
            String category = (String) data.getOrDefault("category", "");
            String status = (String) data.getOrDefault("status", "");

            if (!category.isEmpty()) categories.add(category);
            if (!status.isEmpty()) statuses.add(status);
        }

        uiInstance.access(() -> {
            // Update grid data
            equipmentGrid.setItems(equipmentList);

            // Update count label
            equipmentCountLabel.setText(equipmentList.size() +
                    (equipmentList.size() == 1 ? " item" : " items"));

            // Update filter options
            List<String> sortedCategories = new ArrayList<>(categories);
            Collections.sort(sortedCategories);
            categoryFilter.setItems(sortedCategories);

            List<String> sortedStatuses = new ArrayList<>(statuses);
            Collections.sort(sortedStatuses);
            statusFilter.setItems(sortedStatuses);
        });
    }

    private void applyFilters() {
        Firestore db = FirestoreClient.getFirestore();
        String selectedCategory = categoryFilter.getValue();
        String selectedStatus = statusFilter.getValue();

        // Create a query based on filters
        new Thread(() -> {
            try {
                Query query = db.collection("equipment").whereEqualTo("farmId", farmId);

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

    private void resetFilters() {
        categoryFilter.clear();
        statusFilter.clear();
        loadEquipmentData(); // Reload all data
    }

    private void openAddEquipmentDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Equipment");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        TextField nameField = new TextField("Equipment Name");
        nameField.setRequired(true);
        nameField.setWidthFull();

        ComboBox<String> categoryField = new ComboBox<>("Category");
        categoryField.setRequired(true);
        categoryField.setAllowCustomValue(true);
        categoryField.setItems(categories);
        categoryField.addCustomValueSetListener(
                e -> categoryField.setValue(e.getDetail()));

        DatePicker purchaseDateField = new DatePicker("Purchase Date");
        purchaseDateField.setValue(LocalDate.now());

        ComboBox<String> statusField = new ComboBox<>("Status");
        statusField.setRequired(true);
        statusField.setItems("Available", "In Use", "Maintenance", "Broken");
        statusField.setValue("Available");

        NumberField valueField = new NumberField("Value ($)");
        valueField.setValue(0.0);
        valueField.setMin(0);
        valueField.setStep(0.01);

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);
        descriptionField.setHeight("100px");

        TextField serialNumberField = new TextField("Serial Number");

        TextField manufacturerField = new TextField("Manufacturer");

        form.add(nameField, categoryField, purchaseDateField, statusField,
                valueField, serialNumberField, manufacturerField, descriptionField);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button saveBtn = new Button("Save", e -> {
            if (validateEquipmentForm(nameField, categoryField, statusField)) {
                saveEquipment(null, nameField.getValue(), categoryField.getValue(),
                        purchaseDateField.getValue(), statusField.getValue(),
                        valueField.getValue(), descriptionField.getValue(),
                        serialNumberField.getValue(), manufacturerField.getValue());
                dialog.close();
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openEditDialog(Map<String, Object> equipment) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Equipment");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        TextField nameField = new TextField("Equipment Name");
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setValue((String) equipment.getOrDefault("name", ""));

        ComboBox<String> categoryField = new ComboBox<>("Category");
        categoryField.setRequired(true);
        categoryField.setAllowCustomValue(true);
        categoryField.setItems(categories);
        categoryField.setValue((String) equipment.getOrDefault("category", ""));
        categoryField.addCustomValueSetListener(
                e -> categoryField.setValue(e.getDetail()));

        DatePicker purchaseDateField = new DatePicker("Purchase Date");
        if (equipment.get("purchaseDate") instanceof com.google.cloud.Timestamp) {
            com.google.cloud.Timestamp timestamp = (com.google.cloud.Timestamp) equipment.get("purchaseDate");
            LocalDate date = timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            purchaseDateField.setValue(date);
        }

        ComboBox<String> statusField = new ComboBox<>("Status");
        statusField.setRequired(true);
        statusField.setItems("Available", "In Use", "Maintenance", "Broken");
        statusField.setValue((String) equipment.getOrDefault("status", "Available"));

        NumberField valueField = new NumberField("Value ($)");
        if (equipment.get("value") instanceof Number) {
            valueField.setValue(((Number) equipment.get("value")).doubleValue());
        } else {
            valueField.setValue(0.0);
        }
        valueField.setMin(0);
        valueField.setStep(0.01);

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setMaxLength(500);
        descriptionField.setHeight("100px");
        descriptionField.setValue((String) equipment.getOrDefault("description", ""));

        TextField serialNumberField = new TextField("Serial Number");
        serialNumberField.setValue((String) equipment.getOrDefault("serialNumber", ""));

        TextField manufacturerField = new TextField("Manufacturer");
        manufacturerField.setValue((String) equipment.getOrDefault("manufacturer", ""));

        form.add(nameField, categoryField, purchaseDateField, statusField,
                valueField, serialNumberField, manufacturerField, descriptionField);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button saveBtn = new Button("Save", e -> {
            if (validateEquipmentForm(nameField, categoryField, statusField)) {
                saveEquipment((String) equipment.get("id"),
                        nameField.getValue(),
                        categoryField.getValue(),
                        purchaseDateField.getValue(),
                        statusField.getValue(),
                        valueField.getValue(),
                        descriptionField.getValue(),
                        serialNumberField.getValue(),
                        manufacturerField.getValue());
                dialog.close();
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openAssignDialog(Map<String, Object> equipment) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Assign Equipment");
        dialog.setWidth("400px");

        String equipmentName = (String) equipment.getOrDefault("name", "Selected equipment");
        String currentAssignee = (String) equipment.getOrDefault("assignedTo", "");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        Paragraph info = new Paragraph("Assign " + equipmentName + " to a farm worker or vendor.");

        RadioButtonGroup<String> assignmentType = new RadioButtonGroup<>();
        assignmentType.setLabel("Assignment");
        assignmentType.setItems("Unassigned", "Worker", "Vendor");
        assignmentType.setValue(currentAssignee.isEmpty() ? "Unassigned" : "Worker");

        ComboBox<Map<String, Object>> personSelection = new ComboBox<>();
        personSelection.setWidthFull();
        personSelection.setItemLabelGenerator(person -> (String) person.get("name"));

        // Show/hide the selection based on the assignment type
        assignmentType.addValueChangeListener(event -> {
            String selectedType = event.getValue();

            if ("Unassigned".equals(selectedType)) {
                personSelection.setVisible(false);
            } else {
                personSelection.setVisible(true);
                personSelection.setLabel(selectedType);
                loadPeopleForAssignment(selectedType.equals("Worker") ? "farm_workers" : "vendors");
            }
        });

        // Initially hide the selection if unassigned
        personSelection.setVisible(!currentAssignee.isEmpty());

        content.add(info, assignmentType, personSelection);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button saveBtn = new Button("Save", e -> {
            String assignmentValue = assignmentType.getValue();
            if ("Unassigned".equals(assignmentValue)) {
                updateEquipmentAssignment((String) equipment.get("id"), "", "");
            } else {
                Map<String, Object> selectedPerson = personSelection.getValue();
                if (selectedPerson != null) {
                    updateEquipmentAssignment(
                            (String) equipment.get("id"),
                            (String) selectedPerson.get("name"),
                            assignmentValue
                    );
                } else {
                    personSelection.setInvalid(true);
                    return;
                }
            }
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();

        // Load the appropriate people for assignment
        if (!currentAssignee.isEmpty()) {
            loadPeopleForAssignment("farm_workers"); // Default to workers first
        }
    }

    private void loadPeopleForAssignment(String collectionName) {
        if (farmId == null) return;

        Firestore db = FirestoreClient.getFirestore();

        new Thread(() -> {
            try {
                ApiFuture<QuerySnapshot> future = db.collection(collectionName)
                        .whereEqualTo("farmId", farmId)
                        .get();

                QuerySnapshot snapshot = future.get();
                List<Map<String, Object>> peopleList = new ArrayList<>();

                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    data.put("id", doc.getId());
                    peopleList.add(data);
                }

                uiInstance.access(() -> {
                    // Instead of trying to find components, store the ComboBox as a class field
                    // We need to use dialog reference directly or pass the ComboBox as parameter
                    // Since we don't have access to dialog object as a field, let's modify the openAssignDialog method

                    // For now, we'll use a workaround - get all open dialogs and find the ComboBox
                    for (Dialog dialog : uiInstance.getChildren()
                            .filter(component -> component instanceof Dialog)
                            .map(component -> (Dialog) component)
                            .collect(Collectors.toList())) {

                        Optional<ComboBox> comboBoxOptional = dialog.getChildren()
                                .filter(component -> component instanceof VerticalLayout)
                                .flatMap(verticalLayout -> verticalLayout.getChildren())
                                .filter(component -> component instanceof ComboBox && component.isVisible())
                                .map(component -> (ComboBox) component)
                                .findFirst();

                        if (comboBoxOptional.isPresent()) {
                            @SuppressWarnings("unchecked")
                            ComboBox<Map<String, Object>> personSelection = (ComboBox<Map<String, Object>>) comboBoxOptional.get();
                            personSelection.setItems(peopleList);
                            if (!peopleList.isEmpty()) {
                                personSelection.setValue(peopleList.get(0));
                            }
                            break;
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Error loading people data: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void openChangeStatusDialog(Map<String, Object> equipment) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Change Equipment Status");
        dialog.setWidth("400px");

        String equipmentName = (String) equipment.getOrDefault("name", "Selected equipment");
        String currentStatus = (String) equipment.getOrDefault("status", "Available");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        Paragraph info = new Paragraph("Update the status of " + equipmentName + ":");

        RadioButtonGroup<String> statusSelection = new RadioButtonGroup<>();
        statusSelection.setLabel("Status");
        statusSelection.setItems("Available", "In Use", "Maintenance", "Broken");
        statusSelection.setValue(currentStatus);

        TextArea notesField = new TextArea("Notes");
        notesField.setWidthFull();
        notesField.setPlaceholder("Add any relevant notes about this status change");

        content.add(info, statusSelection, notesField);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button saveBtn = new Button("Save", e -> {
            updateEquipmentStatus(
                    (String) equipment.get("id"),
                    statusSelection.getValue(),
                    notesField.getValue()
            );
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void confirmDelete(Map<String, Object> equipment) {
        String equipmentName = (String) equipment.getOrDefault("name", "selected equipment");

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm Deletion");
        dialog.setText("Are you sure you want to delete " + equipmentName + "?");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(event ->
                deleteEquipment((String) equipment.get("id")));

        dialog.open();
    }

    private boolean validateEquipmentForm(TextField nameField, ComboBox<String> categoryField, ComboBox<String> statusField) {
        boolean valid = true;

        if (nameField.isEmpty()) {
            nameField.setInvalid(true);
            valid = false;
        }

        if (categoryField.isEmpty()) {
            categoryField.setInvalid(true);
            valid = false;
        }

        if (statusField.isEmpty()) {
            statusField.setInvalid(true);
            valid = false;
        }

        return valid;
    }

    private void saveEquipment(String equipmentId, String name, String category,
                               LocalDate purchaseDate, String status, Double value,
                               String description, String serialNumber, String manufacturer) {

        Firestore db = FirestoreClient.getFirestore();

        Map<String, Object> equipmentData = new HashMap<>();
        equipmentData.put("name", name);
        equipmentData.put("category", category);
        if (purchaseDate != null) {
            equipmentData.put("purchaseDate", com.google.cloud.Timestamp.of(
                    Date.from(purchaseDate.atStartOfDay(ZoneId.systemDefault()).toInstant())));
        }
        equipmentData.put("status", status);
        equipmentData.put("value", value);
        equipmentData.put("description", description);
        equipmentData.put("serialNumber", serialNumber);
        equipmentData.put("manufacturer", manufacturer);
        equipmentData.put("farmId", farmId);

        new Thread(() -> {
            try {
                if (equipmentId == null) {
                    // Create new equipment
                    equipmentData.put("createdAt", com.google.cloud.Timestamp.now());
                    db.collection("equipment").add(equipmentData);

                    uiInstance.access(() ->
                            Notification.show("Equipment added successfully",
                                    3000,
                                    Notification.Position.BOTTOM_CENTER));
                } else {
                    // Update existing equipment
                    equipmentData.put("updatedAt", com.google.cloud.Timestamp.now());
                    db.collection("equipment").document(equipmentId).update(equipmentData);

                    uiInstance.access(() ->
                            Notification.show("Equipment updated successfully",
                                    3000,
                                    Notification.Position.BOTTOM_CENTER));
                }

                // The listener will update the grid automatically

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Error saving equipment: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void updateEquipmentStatus(String equipmentId, String status, String notes) {
        Firestore db = FirestoreClient.getFirestore();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("lastStatusChange", com.google.cloud.Timestamp.now());

        if (notes != null && !notes.isEmpty()) {
            updates.put("statusNotes", notes);
        }

        new Thread(() -> {
            try {
                db.collection("equipment").document(equipmentId).update(updates);

                // Add status change to history
                Map<String, Object> historyEntry = new HashMap<>();
                historyEntry.put("equipmentId", equipmentId);
                historyEntry.put("status", status);
                historyEntry.put("timestamp", com.google.cloud.Timestamp.now());
                historyEntry.put("notes", notes);
                historyEntry.put("farmId", farmId);

                db.collection("equipment_history").add(historyEntry);

                uiInstance.access(() ->
                        Notification.show("Status updated successfully",
                                3000,
                                Notification.Position.BOTTOM_CENTER));

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Error updating status: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void updateEquipmentAssignment(String equipmentId, String assignedTo, String assignmentType) {
        Firestore db = FirestoreClient.getFirestore();

        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedTo", assignedTo);
        updates.put("assignmentType", assignmentType);
        updates.put("lastAssignmentChange", com.google.cloud.Timestamp.now());

        new Thread(() -> {
            try {
                db.collection("equipment").document(equipmentId).update(updates);

                // Add assignment change to history
                Map<String, Object> historyEntry = new HashMap<>();
                historyEntry.put("equipmentId", equipmentId);
                historyEntry.put("assignedTo", assignedTo);
                historyEntry.put("assignmentType", assignmentType);
                historyEntry.put("timestamp", com.google.cloud.Timestamp.now());
                historyEntry.put("farmId", farmId);

                db.collection("equipment_assignment_history").add(historyEntry);

                uiInstance.access(() ->
                        Notification.show("Assignment updated successfully",
                                3000,
                                Notification.Position.BOTTOM_CENTER));

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Error updating assignment: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void deleteEquipment(String equipmentId) {
        Firestore db = FirestoreClient.getFirestore();

        new Thread(() -> {
            try {
                // First archive the equipment data
                DocumentSnapshot doc = db.collection("equipment")
                        .document(equipmentId)
                        .get()
                        .get();

                if (doc.exists()) {
                    Map<String, Object> equipmentData = doc.getData();
                    equipmentData.put("deletedAt", com.google.cloud.Timestamp.now());
                    equipmentData.put("originalId", equipmentId);

                    // Add to archive collection
                    db.collection("equipment_archived").add(equipmentData);

                    // Then delete the original document
                    db.collection("equipment").document(equipmentId).delete();

                    uiInstance.access(() ->
                            Notification.show("Equipment deleted successfully",
                                    3000,
                                    Notification.Position.BOTTOM_CENTER));
                } else {
                    uiInstance.access(() ->
                            Notification.show("Equipment not found",
                                    3000,
                                    Notification.Position.BOTTOM_CENTER));
                }

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Error deleting equipment: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private String formatDate(Object dateObject) {
        if (dateObject instanceof com.google.cloud.Timestamp) {
            com.google.cloud.Timestamp timestamp = (com.google.cloud.Timestamp) dateObject;
            LocalDate date = timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            return date.format(formatter);
        }
        return "";
    }

    private String formatCurrency(Object valueObject) {
        if (valueObject instanceof Number) {
            double value = ((Number) valueObject).doubleValue();
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
            return currencyFormat.format(value);
        }
        return "$0.00";
    }
}