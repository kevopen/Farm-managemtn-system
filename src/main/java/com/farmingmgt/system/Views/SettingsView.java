package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Route(value = "settings", layout = HomeLayout.class)
@PageTitle("Settings")
public class SettingsView extends VerticalLayout {

    private final UI uiInstance;
    private final Firestore firestore;
    String userId = UserSession.getUserUid();
    // Farm details fields
    private TextField farmNameField;
    private TextArea farmAddressField;
    private TextField ownerNameField;
    private EmailField emailField;
    private TextField phoneField;
    private ComboBox<String> farmTypeField;
    private NumberField farmSizeField;
    private ComboBox<String> sizeUnitField;

    // System settings fields
    private ComboBox<String> languageField;
    private ComboBox<String> dateFormatField;
    private ComboBox<String> currencyField;

    // Logo upload components
    private Image logoPreview;
    private String currentLogoData;
    private MemoryBuffer buffer;
    private Upload logoUpload;

    // Main tabs
    private Tabs tabs;
    private Div pages;
    private Tab farmDetailsTab;
    private Tab systemSettingsTab;

    // Binder for form validation
    private Binder<FarmSettings> binder;

    public SettingsView() {
        this.uiInstance = UI.getCurrent();
        this.firestore = FirestoreClient.getFirestore();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H3 title = new H3("System Settings");
        add(title);

        initializeTabs();
        initializeFormFields();

        // Create the farm details form by default when page loads
        createFarmDetailsForm();

        // Load settings from Firestore
        loadSettingsFromFirestore();
    }

    private void initializeTabs() {
        tabs = new Tabs();
        farmDetailsTab = new Tab("Farm Details");
        systemSettingsTab = new Tab("System Settings");

        tabs.add(farmDetailsTab, systemSettingsTab);

        pages = new Div();
        pages.setSizeFull();

        add(tabs, pages);

        tabs.addSelectedChangeListener(event -> {
            pages.removeAll();
            if (event.getSelectedTab().equals(farmDetailsTab)) {
                createFarmDetailsForm();
            } else {
                createSystemSettingsForm();
            }
        });
    }

    private void initializeFormFields() {
        // Initialize farm details fields
        farmNameField = new TextField("Farm Name");
        farmNameField.setRequired(true);

        farmAddressField = new TextArea("Farm Address");
        farmAddressField.setMinHeight("100px");

        ownerNameField = new TextField("Owner Name");

        emailField = new EmailField("Email Address");
        emailField.setRequiredIndicatorVisible(true);

        phoneField = new TextField("Phone Number");

        farmTypeField = new ComboBox<>("Farm Type");
        farmTypeField.setItems("Crop Farm", "Livestock Farm", "Dairy Farm", "Poultry Farm", "Mixed Farm", "Other");

        farmSizeField = new NumberField("Farm Size");
        farmSizeField.setMin(0);

        sizeUnitField = new ComboBox<>("Unit");
        sizeUnitField.setItems("Acres", "Hectares", "Square Meters", "Square Feet");
        sizeUnitField.setValue("Acres");

        // Initialize logo upload components
        buffer = new MemoryBuffer();
        logoUpload = new Upload(buffer);
        logoUpload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        logoUpload.setMaxFiles(1);
        logoUpload.setMaxFileSize(5 * 1024 * 1024); // 5MB limit

        logoPreview = new Image();
        logoPreview.setWidth("200px");
        logoPreview.setHeight("auto");
        logoPreview.setVisible(false); // Hidden by default until logo is loaded

        logoUpload.addSucceededListener(this::processLogoUpload);

        // Initialize system settings fields
        languageField = new ComboBox<>("Language");
        languageField.setItems("English", "Spanish", "French", "German", "Portuguese", "Other");
        languageField.setValue("English");

        dateFormatField = new ComboBox<>("Date Format");
        dateFormatField.setItems("MM/DD/YYYY", "DD/MM/YYYY", "YYYY-MM-DD");
        dateFormatField.setValue("MM/DD/YYYY");

        currencyField = new ComboBox<>("Currency");
        currencyField.setItems("USD ($)", "EUR (€)", "GBP (£)", "JPY (¥)", "CAD ($)", "AUD ($)", "Other");
        currencyField.setValue("USD ($)");
    }

    private void createFarmDetailsForm() {
        pages.removeAll();

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        // Add fields to form layout
        formLayout.add(farmNameField, 2);
        formLayout.add(farmAddressField, 2);
        formLayout.add(ownerNameField, emailField);
        formLayout.add(phoneField);

        HorizontalLayout farmTypeLayout = new HorizontalLayout();
        farmTypeLayout.add(farmTypeField);
        farmTypeLayout.add(farmSizeField);
        farmTypeLayout.add(sizeUnitField);
        farmTypeLayout.setAlignItems(Alignment.BASELINE);

        formLayout.add(farmTypeLayout, 2);

        VerticalLayout logoLayout = new VerticalLayout();
        logoLayout.add(new H3("Farm Logo"));
        logoLayout.add(logoPreview);
        logoLayout.add(logoUpload);
        logoLayout.setAlignItems(Alignment.START);

        Button saveButton = new Button("Save Farm Details", e -> saveFarmDetails());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        pages.add(formLayout, logoLayout, saveButton);
    }

    private void createSystemSettingsForm() {
        pages.removeAll();

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        formLayout.add(languageField, dateFormatField, currencyField);

        Button saveSystemSettingsButton = new Button("Save System Settings", e -> saveSystemSettings());
        saveSystemSettingsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        pages.add(formLayout, saveSystemSettingsButton);
    }

    private void processLogoUpload(SucceededEvent event) {
        try {
            InputStream inputStream = buffer.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = outputStream.toByteArray();
            currentLogoData = Base64.getEncoder().encodeToString(imageBytes);

            // Update preview
            StreamResource resource = new StreamResource("logo",
                    () -> new ByteArrayInputStream(imageBytes));
            logoPreview.setSrc(resource);
            logoPreview.setVisible(true);

            Notification.show("Logo uploaded successfully", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (IOException e) {
            Notification.show("Error uploading logo: " + e.getMessage(),
                            3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void loadSettingsFromFirestore() {
        try {
             // Replace with your auth method
            DocumentReference docRef = firestore.collection("farm_settings").document(userId);

            DocumentSnapshot document = docRef.get().get();
            if (document.exists()) {
                Map<String, Object> data = document.getData();

                // Update fields with data from Firestore
                uiInstance.access(() -> {
                    // Farm details fields
                    if (data.containsKey("farmName")) farmNameField.setValue((String) data.get("farmName"));
                    if (data.containsKey("farmAddress")) farmAddressField.setValue((String) data.get("farmAddress"));
                    if (data.containsKey("ownerName")) ownerNameField.setValue((String) data.get("ownerName"));
                    if (data.containsKey("email")) emailField.setValue((String) data.get("email"));
                    if (data.containsKey("phone")) phoneField.setValue((String) data.get("phone"));
                    if (data.containsKey("farmType")) farmTypeField.setValue((String) data.get("farmType"));

                    // Handle numeric farm size field
                    if (data.containsKey("farmSize")) {
                        Object farmSizeObj = data.get("farmSize");
                        if (farmSizeObj instanceof Number) {
                            farmSizeField.setValue(((Number) farmSizeObj).doubleValue());
                        } else if (farmSizeObj instanceof String) {
                            try {
                                farmSizeField.setValue(Double.parseDouble((String) farmSizeObj));
                            } catch (NumberFormatException e) {
                                // Keep default value if parsing fails
                            }
                        }
                    }

                    if (data.containsKey("sizeUnit")) sizeUnitField.setValue((String) data.get("sizeUnit"));

                    // System settings fields
                    if (data.containsKey("language")) languageField.setValue((String) data.get("language"));
                    if (data.containsKey("dateFormat")) dateFormatField.setValue((String) data.get("dateFormat"));
                    if (data.containsKey("currency")) currencyField.setValue((String) data.get("currency"));

                    // Update logo if exists
                    if (data.containsKey("logoData")) {
                        currentLogoData = (String) data.get("logoData");
                        if (currentLogoData != null && !currentLogoData.isEmpty()) {
                            byte[] imageBytes = Base64.getDecoder().decode(currentLogoData);
                            StreamResource resource = new StreamResource("logo",
                                    () -> new ByteArrayInputStream(imageBytes));
                            logoPreview.setSrc(resource);
                            logoPreview.setVisible(true);
                        }
                    }
                });
            }
        } catch (InterruptedException | ExecutionException e) {
            uiInstance.access(() -> {
                Notification.show("Error loading settings: " + e.getMessage(),
                                3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            });
        }
    }

    private void saveFarmDetails() {
        String userId = getCurrentUserId(); // Replace with your auth method
        DocumentReference docRef = firestore.collection("farm_settings").document(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("farmName", farmNameField.getValue());
        data.put("farmAddress", farmAddressField.getValue());
        data.put("ownerName", ownerNameField.getValue());
        data.put("email", emailField.getValue());
        data.put("phone", phoneField.getValue());
        data.put("farmType", farmTypeField.getValue());
        data.put("farmSize", farmSizeField.getValue());
        data.put("sizeUnit", sizeUnitField.getValue());

        if (currentLogoData != null && !currentLogoData.isEmpty()) {
            data.put("logoData", currentLogoData);
        }

        // Get existing data to preserve system settings
        docRef.get().addListener(() -> {
            try {
                DocumentSnapshot document = docRef.get().get();
                Map<String, Object> existingData = document.exists() ? document.getData() : new HashMap<>();

                // Keep system settings intact
                if (existingData.containsKey("language")) data.put("language", existingData.get("language"));
                if (existingData.containsKey("dateFormat")) data.put("dateFormat", existingData.get("dateFormat"));
                if (existingData.containsKey("currency")) data.put("currency", existingData.get("currency"));

                // Save updated data
                docRef.set(data)
                        .addListener(() -> {
                            uiInstance.access(() -> {
                                Notification.show("Farm details saved successfully",
                                                3000, Notification.Position.BOTTOM_CENTER)
                                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            });
                        }, Runnable::run);

            } catch (InterruptedException | ExecutionException e) {
                uiInstance.access(() -> {
                    Notification.show("Error saving settings: " + e.getMessage(),
                                    3000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
            }
        }, Runnable::run);
    }

    private void saveSystemSettings() {
        String userId = getCurrentUserId(); // Replace with your auth method
        DocumentReference docRef = firestore.collection("farm_settings").document(userId);

        docRef.get().addListener(() -> {
            try {
                DocumentSnapshot document = docRef.get().get();
                Map<String, Object> data = document.exists() ? document.getData() : new HashMap<>();

                // Update only system settings
                data.put("language", languageField.getValue());
                data.put("dateFormat", dateFormatField.getValue());
                data.put("currency", currencyField.getValue());

                docRef.set(data)
                        .addListener(() -> {
                            uiInstance.access(() -> {
                                Notification.show("System settings saved successfully",
                                                3000, Notification.Position.BOTTOM_CENTER)
                                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            });
                        }, Runnable::run);

            } catch (InterruptedException | ExecutionException e) {
                uiInstance.access(() -> {
                    Notification.show("Error saving settings: " + e.getMessage(),
                                    3000, Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
            }
        }, Runnable::run);
    }

    // Replace this with your actual authentication method
    private String getCurrentUserId() {
        // This should return the current user's ID from your authentication system
        return userId;
    }

    // Data class for form binding
    private static class FarmSettings {
        private String farmName;
        private String farmAddress;
        private String ownerName;
        private String email;
        private String phone;
        private String farmType;
        private Double farmSize;
        private String sizeUnit;
        private String language;
        private String dateFormat;
        private String currency;
        private String logoData;

        // Getters and setters
        public String getFarmName() { return farmName; }
        public void setFarmName(String farmName) { this.farmName = farmName; }

        public String getFarmAddress() { return farmAddress; }
        public void setFarmAddress(String farmAddress) { this.farmAddress = farmAddress; }

        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getFarmType() { return farmType; }
        public void setFarmType(String farmType) { this.farmType = farmType; }

        public Double getFarmSize() { return farmSize; }
        public void setFarmSize(Double farmSize) { this.farmSize = farmSize; }

        public String getSizeUnit() { return sizeUnit; }
        public void setSizeUnit(String sizeUnit) { this.sizeUnit = sizeUnit; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getLogoData() { return logoData; }
        public void setLogoData(String logoData) { this.logoData = logoData; }
    }
}