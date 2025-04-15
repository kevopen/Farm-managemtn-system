package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Route(value = "manager-dashboard", layout = ManagerLayout.class)
@PageTitle("Manager Dashboard")
public class ManagerDashboard extends VerticalLayout {
    private Grid<Map<String, Object>> vendorGrid = new Grid<>();
    private Text userDetails = new Text("Loading farm details...");
    private String farmId; // This will be used to assign vendors
    private UI uiInstance;
    private Registration vendorListenerRegistration;

    public ManagerDashboard() {
        this.uiInstance = UI.getCurrent();
        setSizeFull();
        getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        getStyle().set("max-width", "1200px");
        getStyle().set("margin", "0 auto");

        add(createHeader()); // Load header with farm info
        add(createWelcomePanel());
        add(createCardRow());
        add(createVendorSection());
    }
    
    private Component createWelcomePanel() {
        VerticalLayout welcomePanel = new VerticalLayout();
        welcomePanel.setWidthFull();
        welcomePanel.setPadding(true);
        welcomePanel.setSpacing(false);
        welcomePanel.getStyle()
                .set("background-image", "linear-gradient(to right, #4facfe 0%, #00f2fe 100%)")
                .set("border-radius", "12px")
                .set("color", "white")
                .set("margin-bottom", "20px");

        H4 welcomeTitle = new H4("Welcome to Farm Management Dashboard");
        welcomeTitle.getStyle().set("margin", "0");
        
        Span welcomeText = new Span("Manage your farm operations efficiently with our comprehensive dashboard");
        welcomeText.getStyle().set("margin-top", "8px");
        
        welcomePanel.add(welcomeTitle, welcomeText);
        return welcomePanel;
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
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle()
                .set("background-color", "white")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.08)")
                .set("margin-bottom", "20px");

        Icon userIcon = VaadinIcon.USER.create();
        userIcon.setSize("24px");
        userIcon.getStyle()
                .set("background-color", "var(--lumo-primary-color)")
                .set("padding", "8px")
                .set("border-radius", "50%")
                .set("color", "white");

        VerticalLayout userInfoLayout = new VerticalLayout();
        userInfoLayout.setPadding(false);
        userInfoLayout.setSpacing(false);
        userInfoLayout.add(userDetails);
        
        Span lastLoginSpan = new Span("Last login: Today at " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        lastLoginSpan.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "small");
        userInfoLayout.add(lastLoginSpan);

        HorizontalLayout userSection = new HorizontalLayout(userIcon, userInfoLayout);
        userSection.setAlignItems(FlexComponent.Alignment.CENTER);
        userSection.setSpacing(true);
        
        header.add(userSection);
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
        cardsRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        cardsRow.setSpacing(true);
        cardsRow.getStyle().set("margin-bottom", "20px");

        cardsRow.add(
                createDashboardCard("Equipment Management", VaadinIcon.TOOLS, "#4CAF50", "12 items", Equipment.class),
                createDashboardCard("Inventory Management", VaadinIcon.STORAGE, "#2196F3", "24 items", InventoryMgtNew.class),
                createDashboardCard("Crop Management", VaadinIcon.CROP, "#FF9800", "5 crops", CropMgt.class)
        );

        return cardsRow;
    }

    private VerticalLayout createDashboardCard(String title, VaadinIcon icon, String iconColor, String statsText, Class<? extends Component> navigationTarget) {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(false);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.getStyle()
                .set("background-color", "#ffffff")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("border-radius", "12px")
                .set("cursor", "pointer")
                .set("transition", "transform 0.2s, box-shadow 0.2s")
                .set("overflow", "hidden");

        // Add hover effect
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                    .set("transform", "translateY(-5px)")
                    .set("box-shadow", "0 8px 16px rgba(0,0,0,0.1)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                    .set("transform", "translateY(0)")
                    .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)");
        });

        // Create icon with colored background
        HorizontalLayout iconContainer = new HorizontalLayout();
        iconContainer.setWidth("48px");
        iconContainer.setHeight("48px");
        iconContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        iconContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        iconContainer.getStyle()
                .set("background-color", iconColor)
                .set("border-radius", "12px")
                .set("margin-bottom", "8px");

        Icon cardIcon = icon.create();
        cardIcon.setSize("24px");
        cardIcon.setColor("white");
        iconContainer.add(cardIcon);

        H4 titleH4 = new H4(title);
        titleH4.getStyle().set("margin", "8px 0");

        // Add stats text
        Span stats = new Span(statsText);
        stats.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        // Add a button for navigation
        Button manageButton = new Button("Manage");
        manageButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        manageButton.getStyle().set("margin-top", "8px");
        manageButton.addClickListener(e -> UI.getCurrent().navigate(navigationTarget));

        card.add(iconContainer, titleH4, stats, manageButton);
        
        // Make the entire card clickable
        card.addClickListener(e -> UI.getCurrent().navigate(navigationTarget));
        
        return card;
    }

    // =================== VENDOR SECTION ===================
    private HorizontalLayout vendorHeader;
    private Span vendorCountLabel;

    private VerticalLayout createVendorSection() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.getStyle()
                .set("background-color", "white")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.08)");

        // Create section title with icon
        Icon vendorIcon = VaadinIcon.USERS.create();
        vendorIcon.setSize("20px");
        vendorIcon.getStyle().set("color", "var(--lumo-primary-color)");
        
        H4 sectionTitle = new H4("Vendors");
        sectionTitle.getStyle().set("margin", "0");
        
        HorizontalLayout titleLayout = new HorizontalLayout(vendorIcon, sectionTitle);
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);

        vendorCountLabel = new Span("0 vendors");
        vendorCountLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "16px")
                .set("padding", "4px 12px");

        Button createVendorBtn = new Button("Create Vendor", VaadinIcon.PLUS.create());
        createVendorBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createVendorBtn.addClickListener(e -> openCreateVendorDialog());

        vendorHeader = new HorizontalLayout(titleLayout, vendorCountLabel);
        vendorHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        vendorHeader.setSpacing(true);

        HorizontalLayout actionBar = new HorizontalLayout(vendorHeader, createVendorBtn);
        actionBar.setWidthFull();
        actionBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        actionBar.setAlignItems(FlexComponent.Alignment.CENTER);
        actionBar.getStyle().set("margin-bottom", "16px");

        // Add a description text
        Span description = new Span("Manage your vendors and their access to your farm data");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "16px");

        layout.add(actionBar, description, configureVendorGrid());

        return layout;
    }

    private void updateVendorCountLabel(int count) {
        vendorCountLabel.setText(count + (count == 1 ? " vendor" : " vendors"));
    }

    private Component configureVendorGrid() {
        vendorGrid.setWidthFull();
        vendorGrid.setHeight("400px");
        vendorGrid.getStyle()
                .set("border-radius", "8px")
                .set("overflow", "hidden");

        // Add some nicer styling
        vendorGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT, GridVariant.LUMO_ROW_STRIPES);

        // Configure columns with proper headers and formatting
        Grid.Column<Map<String, Object>> nameColumn = vendorGrid
                .addComponentColumn(vendor -> {
                    HorizontalLayout layout = new HorizontalLayout();
                    layout.setAlignItems(FlexComponent.Alignment.CENTER);
                    layout.setSpacing(true);
                    
                    // Create avatar with first letter of name
                    String name = vendor.getOrDefault("name", "").toString();
                    String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
                    
                    Span avatar = new Span(initial);
                    avatar.getStyle()
                            .set("background-color", "var(--lumo-primary-color)")
                            .set("color", "white")
                            .set("border-radius", "50%")
                            .set("width", "32px")
                            .set("height", "32px")
                            .set("display", "flex")
                            .set("align-items", "center")
                            .set("justify-content", "center")
                            .set("font-weight", "bold");
                    
                    Span nameSpan = new Span(name);
                    nameSpan.getStyle().set("font-weight", "500");
                    
                    layout.add(avatar, nameSpan);
                    return layout;
                })
                .setHeader("Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        Grid.Column<Map<String, Object>> emailColumn = vendorGrid
                .addComponentColumn(vendor -> {
                    String email = vendor.getOrDefault("email", "").toString();
                    Span emailSpan = new Span(email);
                    emailSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    return emailSpan;
                })
                .setHeader("Email")
                .setAutoWidth(true)
                .setFlexGrow(1);

        Grid.Column<Map<String, Object>> phoneColumn = vendorGrid
                .addComponentColumn(vendor -> {
                    String phone = vendor.getOrDefault("phone", "").toString();
                    if (phone.isEmpty()) {
                        Span noPhone = new Span("No phone");
                        noPhone.getStyle()
                                .set("color", "var(--lumo-tertiary-text-color)")
                                .set("font-style", "italic");
                        return noPhone;
                    }
                    return new Span(phone);
                })
                .setHeader("Phone")
                .setAutoWidth(true);

        // Add status column
        vendorGrid.addComponentColumn(vendor -> {
            Span statusBadge = new Span("Active");
            statusBadge.getStyle()
                    .set("background-color", "var(--lumo-success-color-10pct)")
                    .set("color", "var(--lumo-success-text-color)")
                    .set("border-radius", "16px")
                    .set("padding", "4px 8px")
                    .set("font-size", "12px")
                    .set("font-weight", "500");
            return statusBadge;
        }).setHeader("Status").setAutoWidth(true);

        // Add action column for editing and deleting vendors
        vendorGrid.addComponentColumn(vendor -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editButton.getElement().setAttribute("title", "Edit vendor");
            editButton.addClickListener(e -> openEditVendorDialog(vendor));

            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            deleteButton.getElement().setAttribute("title", "Delete vendor");
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
        dialog.setWidth("500px");
        dialog.getElement().getThemeList().add("dialog-rounded");
        
        // Add a description
        Span description = new Span("Create a new vendor account to give access to your farm data");
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("margin-bottom", "16px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        form.getStyle().set("margin-top", "16px");

        // Create fields with icons
        TextField nameField = new TextField();
        nameField.setLabel("Full Name");
        nameField.setPrefixComponent(VaadinIcon.USER.create());
        nameField.setRequired(true);
        nameField.setPlaceholder("Enter vendor's full name");
        nameField.setWidthFull();

        TextField phoneField = new TextField();
        phoneField.setLabel("Phone Number");
        phoneField.setPrefixComponent(VaadinIcon.PHONE.create());
        phoneField.setPlaceholder("Enter vendor's phone number");
        phoneField.setWidthFull();

        EmailField emailField = new EmailField();
        emailField.setLabel("Email Address");
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setRequired(true);
        emailField.setPlaceholder("Enter vendor's email address");
        emailField.setWidthFull();
        emailField.setErrorMessage("Please enter a valid email address");

        PasswordField passwordField = new PasswordField();
        passwordField.setLabel("Password");
        passwordField.setPrefixComponent(VaadinIcon.LOCK.create());
        passwordField.setRequired(true);
        passwordField.setPlaceholder("Create a password for the vendor");
        passwordField.setHelperText("Minimum 6 characters");
        passwordField.setWidthFull();

        form.add(nameField, phoneField, emailField, passwordField);

        // Create styled buttons
        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelBtn.getStyle().set("margin-right", "auto");

        Button saveBtn = new Button("Create Vendor", event -> {
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
        saveBtn.setIcon(VaadinIcon.CHECK.create());

        dialog.add(description, form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        //dialog.getFooter().getStyle().set("padding", "16px");
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
        dialog.setWidth("500px");
        dialog.getElement().getThemeList().add("dialog-rounded");
        
        // Add vendor name to header
        String vendorName = vendor.getOrDefault("name", "").toString();
        Span description = new Span("Update information for " + vendorName);
        description.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block")
                .set("margin-bottom", "16px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        form.getStyle().set("margin-top", "16px");

        // Create fields with icons
        TextField nameField = new TextField();
        nameField.setLabel("Full Name");
        nameField.setPrefixComponent(VaadinIcon.USER.create());
        nameField.setValue(vendorName);
        nameField.setRequired(true);
        nameField.setWidthFull();

        TextField phoneField = new TextField();
        phoneField.setLabel("Phone Number");
        phoneField.setPrefixComponent(VaadinIcon.PHONE.create());
        phoneField.setValue(vendor.getOrDefault("phone", "").toString());
        phoneField.setWidthFull();

        EmailField emailField = new EmailField();
        emailField.setLabel("Email Address");
        emailField.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        emailField.setValue(vendor.getOrDefault("email", "").toString());
        emailField.setReadOnly(true); // Email can't be changed as it's used for authentication
        emailField.setHelperText("Email cannot be changed as it's used for authentication");
        emailField.getStyle().set("opacity", "0.7");
        emailField.setWidthFull();

        form.add(nameField, phoneField, emailField);

        // Create styled buttons
        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelBtn.getStyle().set("margin-right", "auto");

        Button saveBtn = new Button("Save Changes", event -> {
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
        saveBtn.setIcon(VaadinIcon.CHECK.create());

        dialog.add(description, form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        //dialog.getFooter().getStyle().set("padding", "16px");
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
        DatabaseReference realtimeDb = FirebaseDatabase.getInstance().getReference();

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

                // Save to Firestore
                db.collection("vendors").document(uid).set(vendorData);

                // Save to Realtime Database with role
                Map<String, Object> realtimeData = new HashMap<>(vendorData);
                realtimeData.put("role", "vendor");
                realtimeDb.child("users").child(uid).setValueAsync(realtimeData);

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
