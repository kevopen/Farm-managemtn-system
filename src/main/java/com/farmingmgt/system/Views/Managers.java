package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Route(value = "managers", layout = HomeLayout.class)
@PageTitle("Farm Managers")
public class Managers extends VerticalLayout {
    private final UI uiInstance;
    private final Firestore firestore;
    private final FirebaseAuth firebaseAuth;
    String currentUserId = UserSession.getUserUid();
    private Grid<FarmManager> managersGrid;
    private Dialog managerFormDialog;
    private TextField nameField;
    private TextField emailField;
    private PasswordField passwordField;
    private TextField phoneField;
    private ComboBox<String> statusComboBox;

    public Managers() {
        this.uiInstance = UI.getCurrent();
        this.firestore = FirestoreClient.getFirestore();
        this.firebaseAuth = FirebaseAuth.getInstance();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 pageTitle = new H2("Farm Managers");
        pageTitle.getStyle().set("margin-top", "0");

        createManagersGrid();

        Button addManagerButton = new Button("Add Manager", VaadinIcon.PLUS.create());
        addManagerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addManagerButton.addClickListener(e -> showManagerForm(null));

        add(pageTitle, addManagerButton, managersGrid);

        // Load managers data
        loadManagers();
    }

    private void createManagersGrid() {
        managersGrid = new Grid<>();
        managersGrid.setSizeFull();
        managersGrid.addColumn(FarmManager::getName).setHeader("Name").setSortable(true);
        managersGrid.addColumn(FarmManager::getEmail).setHeader("Email").setSortable(true);
        managersGrid.addColumn(FarmManager::getPhone).setHeader("Phone").setSortable(true);
        managersGrid.addColumn(FarmManager::getStatus).setHeader("Status").setSortable(true);

        // Add action column
        managersGrid.addComponentColumn(manager -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            editButton.addClickListener(e -> showManagerForm(manager));

            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            deleteButton.addClickListener(e -> confirmDelete(manager));

            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions").setFlexGrow(0);
    }

    private void showManagerForm(FarmManager manager) {
        managerFormDialog = new Dialog();
        managerFormDialog.setWidth("400px");

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(true);
        formLayout.setSpacing(true);

        H3 dialogTitle = new H3(manager == null ? "Add Farm Manager" : "Edit Farm Manager");

        nameField = new TextField("Name");
        nameField.setWidthFull();
        nameField.setRequired(true);

        emailField = new TextField("Email");
        emailField.setWidthFull();
        emailField.setRequired(true);

        passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        passwordField.setRequired(manager == null); // Required only for new managers

        phoneField = new TextField("Phone");
        phoneField.setWidthFull();

        statusComboBox = new ComboBox<>("Status");
        statusComboBox.setWidthFull();
        statusComboBox.setItems("Active", "Inactive");
        statusComboBox.setValue("Active");

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);

        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(e -> managerFormDialog.close());

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            if (validateForm()) {
                saveManager(manager);
            }
        });

        buttonLayout.add(cancelButton, saveButton);

        formLayout.add(dialogTitle, nameField, emailField, passwordField, phoneField, statusComboBox, buttonLayout);

        // Fill form if editing
        if (manager != null) {
            nameField.setValue(manager.getName());
            emailField.setValue(manager.getEmail());
            phoneField.setValue(manager.getPhone());
            statusComboBox.setValue(manager.getStatus());

            // Disable email if editing (cannot change email in Firebase Auth easily)
            emailField.setReadOnly(true);
        }

        managerFormDialog.add(formLayout);
        managerFormDialog.open();
    }

    private boolean validateForm() {
        boolean valid = true;

        if (nameField.getValue().trim().isEmpty()) {
            nameField.setInvalid(true);
            valid = false;
        }

        if (emailField.getValue().trim().isEmpty() || !emailField.getValue().contains("@")) {
            emailField.setInvalid(true);
            valid = false;
        }

        // Password validation only for new users
        if (emailField.isReadOnly() == false && passwordField.getValue().length() < 6) {
            passwordField.setInvalid(true);
            passwordField.setErrorMessage("Password must be at least 6 characters");
            valid = false;
        }

        return valid;
    }

    private void saveManager(FarmManager existingManager) {
        // Show loading indicator
        managerFormDialog.close();

        UI.getCurrent().access(() -> {
            ProgressBar progressBar = new ProgressBar();
            progressBar.setIndeterminate(true);
            add(progressBar);

            CompletableFuture.runAsync(() -> {
                try {
                    if (currentUserId == null) {
                        throw new Exception("Not authenticated");
                    }

                    String name = nameField.getValue();
                    String email = emailField.getValue();
                    String password = passwordField.getValue();
                    String phone = phoneField.getValue();
                    String status = statusComboBox.getValue();

                    if (existingManager == null) {
                        // Create user in Firebase Auth
                        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                                .setEmail(email)
                                .setPassword(password)
                                .setDisplayName(name);

                        UserRecord userRecord = firebaseAuth.createUser(request);

                        // Set custom claims
                        Map<String, Object> claims = new HashMap<>();
                        claims.put("role", "farm-manager");
                        claims.put("farmOwnerId", currentUserId);

                        firebaseAuth.setCustomUserClaims(userRecord.getUid(), claims);

                        // Save to Firestore
                        Map<String, Object> managerData = new HashMap<>();
                        managerData.put("uid", userRecord.getUid());
                        managerData.put("name", name);
                        managerData.put("email", email);
                        managerData.put("phone", phone);
                        managerData.put("status", status);
                        managerData.put("farmOwnerId", currentUserId);
                        managerData.put("createdAt", FieldValue.serverTimestamp());

                        firestore.collection("farm_managers")
                                .document(userRecord.getUid())
                                .set(managerData);

                        // Save to Realtime Database
                        Map<String, Object> rtdbUser = new HashMap<>();
                        rtdbUser.put("uid", userRecord.getUid());
                        rtdbUser.put("name", name);
                        rtdbUser.put("email", email);
                        rtdbUser.put("phone", phone);
                        rtdbUser.put("role", "farm-manager");
                        rtdbUser.put("farmOwnerId", currentUserId);

                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                        usersRef.child(userRecord.getUid()).setValueAsync(rtdbUser);
                    } else {
                        // Update Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", name);
                        updates.put("phone", phone);
                        updates.put("status", status);
                        updates.put("updatedAt", FieldValue.serverTimestamp());

                        firestore.collection("farm_managers")
                                .document(existingManager.getUid())
                                .update(updates);

                        // Optional: Update Realtime DB as well
                        Map<String, Object> rtdbUpdates = new HashMap<>();
                        rtdbUpdates.put("name", name);
                        rtdbUpdates.put("phone", phone);

                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(existingManager.getUid());

                        userRef.updateChildrenAsync(rtdbUpdates);
                    }

                    // UI update
                    uiInstance.access(() -> {
                        remove(progressBar);
                        loadManagers();
                        Notification.show(
                                existingManager == null ? "Manager added successfully" : "Manager updated successfully",
                                3000, Notification.Position.TOP_CENTER
                        );
                    });

                } catch (Exception e) {
                    uiInstance.access(() -> {
                        remove(progressBar);
                        Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                    });
                }
            });
        });
    }


    private void confirmDelete(FarmManager manager) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirm Delete");
        dialog.setText("Are you sure you want to delete this manager? This action cannot be undone.");

        dialog.setCancelable(true);
        dialog.addCancelListener(event -> dialog.close());

        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> deleteManager(manager));

        dialog.open();
    }

    private void deleteManager(FarmManager manager) {
        UI.getCurrent().access(() -> {
            ProgressBar progressBar = new ProgressBar();
            progressBar.setIndeterminate(true);
            add(progressBar);

            CompletableFuture.runAsync(() -> {
                try {
                    // Delete from Firestore
                    firestore.collection("farm_managers").document(manager.getUid()).delete();

                    // Delete from Firebase Auth
                    firebaseAuth.deleteUser(manager.getUid());

                    // Refresh UI
                    uiInstance.access(() -> {
                        remove(progressBar);
                        loadManagers();
                        Notification.show("Manager deleted successfully",
                                3000, Notification.Position.TOP_CENTER);
                    });

                } catch (Exception e) {
                    uiInstance.access(() -> {
                        remove(progressBar);
                        Notification.show("Error: " + e.getMessage(),
                                5000, Notification.Position.TOP_CENTER);
                    });
                }
            });
        });
    }

    private void loadManagers() {
        // Show loading indicator
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        add(progressBar);

        CompletableFuture.runAsync(() -> {
            try {

                if (currentUserId == null) {
                    throw new Exception("Not authenticated");
                }

                // Query managers for this farm owner
                ApiFuture<QuerySnapshot> future = firestore.collection("farm_managers")
                        .whereEqualTo("farmOwnerId", currentUserId)
                        .get();

                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                List<FarmManager> managers = new ArrayList<>();

                for (QueryDocumentSnapshot document : documents) {
                    FarmManager manager = new FarmManager(
                            document.getId(),
                            document.getString("name"),
                            document.getString("email"),
                            document.getString("phone"),
                            document.getString("status")
                    );
                    managers.add(manager);
                }

                // Update UI
                uiInstance.access(() -> {
                    remove(progressBar);
                    managersGrid.setItems(managers);
                });

            } catch (Exception e) {
                uiInstance.access(() -> {
                    remove(progressBar);
                    Notification.show("Error loading managers: " + e.getMessage(),
                            5000, Notification.Position.TOP_CENTER);
                });
            }
        });
    }

    private String getCurrentUserId() {
        // Get current user ID from session
        // This depends on how you're storing the authenticated user in your application
        // Example implementation:
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null && session.getAttribute("currentUser") != null) {
            return session.getAttribute("currentUser").toString();
        }
        return null;
    }

    // Farm Manager model class
    public static class FarmManager {
        private String uid;
        private String name;
        private String email;
        private String phone;
        private String status;

        public FarmManager(String uid, String name, String email, String phone, String status) {
            this.uid = uid;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.status = status;
        }

        public String getUid() {
            return uid;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }

        public String getStatus() {
            return status;
        }
    }
}