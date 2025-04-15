package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Route(value = "manager-dashboardbk", layout = ManagerLayout.class)
@PageTitle("Manager Dashboard")
public class ManagerDashboardbk extends VerticalLayout {
    private Grid<Map<String, Object>> vendorGrid = new Grid<>();
    private Text userDetails = new Text("Loading farm details...");
    private String farmId; // This will be used to assign vendors
    private UI uiInstance;
    private Registration vendorListenerRegistration;

    public ManagerDashboardbk() {
        this.uiInstance = UI.getCurrent();

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        add(createHeader()); // Load header with farm info
        add(createCardRow());
        add(createVendorSection());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Load farm details first, which will then trigger vendor loading
        loadManagerDetails();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Clean up listener when component is detached
        if (vendorListenerRegistration != null) {
            vendorListenerRegistration.remove();
        }
    }

    private void loadVendors() {
        if (farmId == null) return;

        Firestore db = FirestoreClient.getFirestore();

        // First, load initial data
        new Thread(() -> {
            try {
                ApiFuture<QuerySnapshot> future = db.collection("vendors")
                        .whereEqualTo("farmId", farmId)
                        .get();

                QuerySnapshot snapshot = future.get();
                updateVendorGrid(snapshot);

                // Then set up real-time listener
                setupVendorListener(db);

            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Failed to load vendors: " + e.getMessage(), 3000, Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void setupVendorListener(Firestore db) {
        // Set up real-time listener for vendors collection
        ListenerRegistration registration = db.collection("vendors")
                .whereEqualTo("farmId", farmId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        System.err.println("Vendor listener error: " + error);
                        return;
                    }

                    updateVendorGrid(snapshots);
                });

        // Store registration for cleanup
        this.vendorListenerRegistration = () -> registration.remove();
    }

    private void updateVendorGrid(QuerySnapshot snapshot) {
        java.util.List<Map<String, Object>> vendorList = new java.util.ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Map<String, Object> data = doc.getData();
            // Add document ID to the data for reference
            data.put("id", doc.getId());
            vendorList.add(data);
        }

        uiInstance.access(() -> {
            vendorGrid.setItems(vendorList);
            // Update items count in header
            updateVendorCountLabel(vendorList.size());
        });
    }

    // =================== HEADER ===================
    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout(userDetails);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        return header;
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
                    String managerName = managerDoc.getString("name");
                    String farmOwnerId = managerDoc.getString("farmOwnerId");

                    // Set farmId and then load vendors
                    this.farmId = farmOwnerId;

                    DocumentSnapshot farmDoc = db.collection("farm_settings")
                            .document(farmOwnerId)
                            .get()
                            .get();

                    String farmName = farmDoc.getString("farmName");
                    String farmAddress = farmDoc.getString("farmAddress");

                    String finalText = managerName + " - " + (farmName != null ? farmName : "Unnamed Farm")
                            + (farmAddress != null ? " (" + farmAddress + ")" : "");

                    uiInstance.access(() -> userDetails.setText(finalText));

                    // Load vendors after we have the farmId
                    loadVendors();
                } else {
                    uiInstance.access(() -> userDetails.setText("Manager not found"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() -> {
                    userDetails.setText("Error loading farm details");
                    Notification.show("Error: " + e.getMessage(), 3000, Notification.Position.BOTTOM_CENTER);
                });
            }
        }).start();
    }

    // =================== DASHBOARD CARDS ===================
    private HorizontalLayout createCardRow() {
        HorizontalLayout cardsRow = new HorizontalLayout();
        cardsRow.setWidthFull();
        cardsRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        cardsRow.setSpacing(true);

        cardsRow.add(
                createDashboardCard("Equipment Management", VaadinIcon.TOOLS),
                createDashboardCard("Inventory Management", VaadinIcon.STORAGE),
                createDashboardCard("Crop Management", VaadinIcon.CROP)
        );

        return cardsRow;
    }

    private VerticalLayout createDashboardCard(String title, VaadinIcon icon) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setAlignItems(Alignment.CENTER);
        card.getStyle()
                .set("background-color", "#ffffff")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("border-radius", "12px")
                .set("cursor", "pointer");

        Icon cardIcon = icon.create();
        cardIcon.setSize("32px");

        card.add(cardIcon, new H4(title));
        return card;
    }

    // =================== VENDOR SECTION ===================
    private HorizontalLayout vendorHeader;
    private Span vendorCountLabel;

    private VerticalLayout createVendorSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        vendorCountLabel = new Span("0 vendors");
        vendorCountLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button createVendorBtn = new Button("Create Vendor", VaadinIcon.PLUS.create());
        createVendorBtn.addClickListener(e -> openCreateVendorDialog());

        vendorHeader = new HorizontalLayout(new H4("Vendors"), vendorCountLabel);
        vendorHeader.setAlignItems(Alignment.BASELINE);
        vendorHeader.setSpacing(true);

        HorizontalLayout actionBar = new HorizontalLayout(vendorHeader, createVendorBtn);
        actionBar.setWidthFull();
        actionBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        layout.add(actionBar, configureVendorGrid());

        return layout;
    }

    private void updateVendorCountLabel(int count) {
        vendorCountLabel.setText(count + (count == 1 ? " vendor" : " vendors"));
    }

    private Component configureVendorGrid() {
        vendorGrid.setWidthFull();
        vendorGrid.setHeight("300px");

        // Add some nicer styling
        vendorGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);

        // Configure columns with proper headers and formatting
        Grid.Column<Map<String, Object>> nameColumn = vendorGrid
                .addColumn(v -> v.getOrDefault("name", ""))
                .setHeader("Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        Grid.Column<Map<String, Object>> emailColumn = vendorGrid
                .addColumn(v -> v.getOrDefault("email", ""))
                .setHeader("Email")
                .setAutoWidth(true)
                .setFlexGrow(1);

        Grid.Column<Map<String, Object>> phoneColumn = vendorGrid
                .addColumn(v -> v.getOrDefault("phone", ""))
                .setHeader("Phone")
                .setAutoWidth(true);

        // Add action column for editing and deleting vendors
        vendorGrid.addComponentColumn(vendor -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editButton.addClickListener(e -> openEditVendorDialog(vendor));

            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteButton.addClickListener(e -> confirmDeleteVendor(vendor));

            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        // Add filtering capability
        HeaderRow filterRow = vendorGrid.appendHeaderRow();

        TextField nameFilter = new TextField();
        nameFilter.setPlaceholder("Filter by name...");
        nameFilter.setClearButtonVisible(true);
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.addValueChangeListener(e -> applyFilters());
        filterRow.getCell(nameColumn).setComponent(nameFilter);

        TextField emailFilter = new TextField();
        emailFilter.setPlaceholder("Filter by email...");
        emailFilter.setClearButtonVisible(true);
        emailFilter.setValueChangeMode(ValueChangeMode.EAGER);
        emailFilter.addValueChangeListener(e -> applyFilters());
        filterRow.getCell(emailColumn).setComponent(emailFilter);

        return vendorGrid;
    }

    private void applyFilters() {
        // This would be implemented for client-side filtering
        // For Firestore, we'd need to adjust our queries which is more complex
    }

    private void openCreateVendorDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Vendor");
        dialog.setWidth("400px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );

        TextField nameField = new TextField("Name");
        nameField.setRequired(true);

        TextField phoneField = new TextField("Phone");

        EmailField emailField = new EmailField("Email");
        emailField.setRequired(true);

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setRequired(true);
        passwordField.setHelperText("Minimum 6 characters");

        form.add(nameField, phoneField, emailField, passwordField);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button saveBtn = new Button("Save", event -> {
            if (validateForm(nameField, emailField, passwordField)) {
                String name = nameField.getValue();
                String phone = phoneField.getValue();
                String email = emailField.getValue();
                String password = passwordField.getValue();

                createVendorAccount(name, phone, email, password);
                dialog.close();
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private boolean validateForm(TextField nameField, EmailField emailField, PasswordField passwordField) {
        boolean valid = true;

        if (nameField.isEmpty()) {
            nameField.setInvalid(true);
            valid = false;
        }

        if (emailField.isEmpty() || !emailField.getValue().contains("@")) {
            emailField.setInvalid(true);
            valid = false;
        }

        if (passwordField.isEmpty() || passwordField.getValue().length() < 6) {
            passwordField.setInvalid(true);
            valid = false;
        }

        return valid;
    }

    private void openEditVendorDialog(Map<String, Object> vendor) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Vendor");
        dialog.setWidth("400px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );

        TextField nameField = new TextField("Name");
        nameField.setValue(vendor.getOrDefault("name", "").toString());
        nameField.setRequired(true);

        TextField phoneField = new TextField("Phone");
        phoneField.setValue(vendor.getOrDefault("phone", "").toString());

        EmailField emailField = new EmailField("Email");
        emailField.setValue(vendor.getOrDefault("email", "").toString());
        emailField.setReadOnly(true); // Email can't be changed as it's used for authentication

        form.add(nameField, phoneField, emailField);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button saveBtn = new Button("Save", event -> {
            if (!nameField.isEmpty()) {
                updateVendor(
                        vendor.get("id").toString(),
                        nameField.getValue(),
                        phoneField.getValue()
                );
                dialog.close();
            } else {
                nameField.setInvalid(true);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void updateVendor(String vendorId, String name, String phone) {
        Firestore db = FirestoreClient.getFirestore();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);

        new Thread(() -> {
            try {
                db.collection("vendors").document(vendorId)
                        .update(updates);

                // No need to refresh grid as the listener will handle it
                uiInstance.access(() ->
                        Notification.show("Vendor updated successfully",
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Failed to update vendor: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void confirmDeleteVendor(Map<String, Object> vendor) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm Deletion");
        dialog.setText("Are you sure you want to delete the vendor " +
                vendor.getOrDefault("name", "") + "?");

        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(event ->
                deleteVendor(vendor.get("id").toString()));

        dialog.open();
    }

    private void deleteVendor(String vendorId) {
        Firestore db = FirestoreClient.getFirestore();

        new Thread(() -> {
            try {
                // Delete from vendors collection
                db.collection("vendors").document(vendorId).delete();

                // Attempt to delete the user account as well
                com.google.firebase.auth.FirebaseAuth.getInstance().deleteUser(vendorId);

                // Grid will be updated by the listener
                uiInstance.access(() ->
                        Notification.show("Vendor deleted successfully",
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Failed to delete vendor: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    private void createVendorAccount(String name, String phone, String email, String password) {
        Firestore db = FirestoreClient.getFirestore();

        new Thread(() -> {
            try {
                com.google.firebase.auth.UserRecord.CreateRequest request =
                        new com.google.firebase.auth.UserRecord.CreateRequest()
                                .setEmail(email)
                                .setPassword(password);

                com.google.firebase.auth.UserRecord userRecord =
                        com.google.firebase.auth.FirebaseAuth.getInstance().createUser(request);

                String uid = userRecord.getUid();

                Map<String, Object> vendorData = new HashMap<>();
                vendorData.put("name", name);
                vendorData.put("phone", phone);
                vendorData.put("email", email);
                vendorData.put("uid", uid);
                vendorData.put("farmId", farmId);
                vendorData.put("createdAt", new Date());

                db.collection("vendors").document(uid).set(vendorData);

                // The listener will update the grid automatically
                uiInstance.access(() ->
                        Notification.show("Vendor created successfully",
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() ->
                        Notification.show("Failed to create vendor: " + e.getMessage(),
                                3000,
                                Notification.Position.BOTTOM_CENTER));
            }
        }).start();
    }

    // Interface for handling listener registration cleanup
    private interface Registration {
        void remove();
    }
}
