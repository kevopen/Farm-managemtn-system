package com.farmingmgt.system.Views;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Route(value = "dashboard", layout = HomeLayout.class)
@PageTitle("Dashboard")
public class DashboardView extends VerticalLayout {
    private final UI uiInstance;
    private final Firestore firestore;
    private Div upcomingTasksContent, productionCalendarContent;

    public DashboardView() {
        this.uiInstance = UI.getCurrent();
        this.firestore = FirestoreClient.getFirestore();

        // Set up the main layout
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Add header with date
        add(createHeader());

        createAdditionalSection();
        fetchTasks();
        Button productionCalendar = new Button("New Production", VaadinIcon.PLUS_CIRCLE.create());
        Button supplyChain = new Button("Supply Chain", VaadinIcon.PLUS_CIRCLE.create());
        productionCalendar.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            @Override
            public void onComponentEvent(ClickEvent<Button> event) {
                UI.getCurrent().navigate("production");
            }
        });
        supplyChain.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            @Override
            public void onComponentEvent(ClickEvent<Button> event) {
                UI.getCurrent().navigate("supply");
            }
        });
        productionCalendar.getStyle()
                .set("margin-left", "30px")
                .set("color", "var(--lumo-primary-color)")
                .set("font-weight", "bold");
        supplyChain.getStyle()
                .set("margin-left", "30px")
                .set("color", "white")
                .set("font-weight", "bold");
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(productionCalendar, supplyChain);

        add(createMetricsSection(), horizontalLayout, createAdditionalSection());
    }

    private HorizontalLayout createAdditionalSection() {
        // Create cards
        Div recentActivitiesCard = createCard("Recent Activities", "View All", VaadinIcon.CLIPBOARD_TEXT, "#3498db");
        Div upcomingTasksCard = createCard("Upcoming Tasks", "Add Task", VaadinIcon.PLUS_CIRCLE, "#2ecc71");

        // Initialize upcoming tasks content
        upcomingTasksContent = new Div();
        upcomingTasksContent.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("height", "300px")  // Fixed height
                .set("overflow-y", "auto")  // Enable scrolling
                .set("padding", "10px");


        // Set width of the cards
        recentActivitiesCard.setWidth("100%");
        upcomingTasksCard.setWidth("100%");


        // Set the content of the Upcoming Tasks card
        upcomingTasksCard.add(upcomingTasksContent);

        // Layout for the cards
        HorizontalLayout sectionLayout = new HorizontalLayout(
                recentActivitiesCard,
                upcomingTasksCard
        );

        sectionLayout.setPadding(true);
        sectionLayout.setSpacing(true);
        sectionLayout.setWidth("100%");
        sectionLayout.setFlexGrow(1, recentActivitiesCard, upcomingTasksCard);

        return sectionLayout;
    }

    private Div createCard(String title, String buttonText, VaadinIcon icon, String iconColor) {
        // Card container
        Div card = new Div();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "var(--lumo-space-m)")
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "0 4px 10px rgba(0, 0, 0, 0.05)")
                .set("width", "100%")
                .set("height", "300px")
                .set("transition", "transform 0.2s, box-shadow 0.2s")
                .set("overflow", "hidden");

        // Add hover effect
        card.getElement().addEventListener("mouseover", e ->
                card.getStyle()
                        .set("transform", "translateY(-3px)")
                        .set("box-shadow", "0 6px 15px rgba(0, 0, 0, 0.1)")
        );
        card.getElement().addEventListener("mouseout", e ->
                card.getStyle()
                        .set("transform", "translateY(0)")
                        .set("box-shadow", "0 4px 10px rgba(0, 0, 0, 0.05)")
        );

        // Title
        Span titleElement = new Span(title);
        titleElement.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-l)");

        // Button/Icon
        Icon iconElement = icon.create();
        iconElement.getStyle()
                .set("cursor", "pointer")
                .set("color", iconColor)
                .set("width", "24px")
                .set("height", "24px");

        // Add click listener for adding tasks
        if (icon == VaadinIcon.PLUS_CIRCLE) {
            iconElement.addClickListener(event -> showAddTaskPopup());
        }

        // Title & Button Layout
        HorizontalLayout headerLayout = new HorizontalLayout(titleElement, iconElement);
        headerLayout.setWidth("100%");
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setPadding(false);
        headerLayout.setMargin(false);

        // Add a subtle divider
        Div divider = new Div();
        divider.getStyle()
                .set("height", "1px")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("margin", "10px 0")
                .set("width", "100%");

        // Content area (placeholder)
        Div content = new Div();
        content.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Add everything to card
        card.add(headerLayout, divider, content);
        return card;
    }

    private void showAddTaskPopup() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add New Task");

        // Form Fields
        TextField taskTitle = new TextField("Task Title");
        DatePicker dueDate = new DatePicker("Due Date");
        taskTitle.setWidthFull();
        dueDate.setWidthFull();

        // Buttons
        Button saveButton = new Button("Save", event -> {
            if (taskTitle.getValue().isEmpty() || dueDate.getValue() == null) {
                Notification.show("Please fill in all fields.").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            saveTask(taskTitle.getValue(), dueDate.getValue().toString());
            dialog.close();
        });

        Button cancelButton = new Button("Cancel", event -> dialog.close());

        // Layout
        FormLayout formLayout = new FormLayout(taskTitle, dueDate);
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        // Add components to dialog
        dialog.add(formLayout, buttonLayout);
        dialog.open();
    }

    private void saveTask(String title, String dueDate) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("title", title);
        taskData.put("dueDate", dueDate);

        // Add document with auto-generated ID to "tasks" collection
        ApiFuture<DocumentReference> future = firestore.collection("tasks").add(taskData);
        try {
            future.get(); // Wait for operation to complete
            uiInstance.access(() -> {
                Notification.show("Task added successfully!");
                fetchTasks();
            });
        } catch (InterruptedException | ExecutionException e) {
            uiInstance.access(() -> Notification.show("Error adding task: " + e.getMessage()));
        }
    }

    private void fetchTasks() {
        // Clear current content
        upcomingTasksContent.removeAll();

        // Get all documents from "tasks" collection
        ApiFuture<QuerySnapshot> future = firestore.collection("tasks").get();
        try {
            QuerySnapshot snapshot = future.get();
            uiInstance.access(() -> {
                for (DocumentSnapshot document : snapshot.getDocuments()) {
                    String taskTitle = document.getString("title");
                    String dueDate = document.getString("dueDate");

                    // Create a div for each task
                    Div taskItem = new Div();

                    // Add task icon
                    Icon taskIcon = VaadinIcon.TASKS.create();
                    taskIcon.getStyle()
                            .set("color", "#3498db")
                            .set("margin-right", "8px");

                    // Task details
                    Div details = new Div();
                    details.setText(taskTitle + " - " + dueDate);

                    // Create a horizontal layout for the icon and text
                    HorizontalLayout taskContent = new HorizontalLayout(taskIcon, details);
                    taskContent.setAlignItems(Alignment.CENTER);
                    taskContent.setPadding(false);
                    taskContent.setSpacing(true);

                    taskItem.add(taskContent);
                    taskItem.getStyle()
                            .set("padding", "12px")
                            .set("margin-bottom", "10px")
                            .set("border-radius", "8px")
                            .set("box-shadow", "0 2px 5px rgba(0,0,0,0.08)")
                            .set("background-color", "var(--lumo-base-color)")
                            .set("border-left", "4px solid #3498db")
                            .set("transition", "transform 0.2s");

                    // Add hover effect
                    taskItem.getElement().addEventListener("mouseover", e ->
                            taskItem.getStyle().set("transform", "translateX(5px)"));
                    taskItem.getElement().addEventListener("mouseout", e ->
                            taskItem.getStyle().set("transform", "translateX(0)"));

                    // Add the task item to the upcoming tasks section
                    upcomingTasksContent.add(taskItem);
                }
            });
        } catch (InterruptedException | ExecutionException e) {
            uiInstance.access(() -> Notification.show("Error retrieving tasks: " + e.getMessage()));
        }
    }

    private VerticalLayout createHeader() {
        // Get current date and format it
        LocalDate today = LocalDate.now();
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
        String formattedDate = today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        H1 header = new H1("Dashboard");
        header.getStyle()
                .set("margin", "0")
                .set("font-size", "2.2em")
                .set("font-weight", "600")
                .set("color", "var(--lumo-primary-text-color)");

        Span date = new Span(dayOfWeek + ", " + formattedDate);
        date.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "1.1em");

        VerticalLayout headerLayout = new VerticalLayout(header, date);
        headerLayout.setPadding(true);
        headerLayout.setSpacing(false);
        return headerLayout;
    }

    private HorizontalLayout createMetricsSection() {
        // Create cards for each metric with improved design
        Div revenueCard = createMetricCard(
                "Total Revenue",
                "KES 128,450",
                "12.5% from last month",
                VaadinIcon.DOLLAR,
                "linear-gradient(135deg, #3498db, #2c3e50)", // Blue gradient
                "#ffffff" // White text
        );

        Div productionCard = createMetricCard(
                "Current Production",
                "14,280 kg",
                "8.3% from last season",
                VaadinIcon.PACKAGE,
                "linear-gradient(135deg, #2ecc71, #27ae60)", // Green gradient
                "#ffffff" // White text
        );

        Div supplyChainCard = createMetricCard(
                "Supply Chain Status",
                "Optimal",
                "All systems operational",
                VaadinIcon.CHECK_CIRCLE,
                "linear-gradient(135deg, #9b59b6, #8e44ad)", // Purple gradient
                "#ffffff" // White text
        );

        Div forecastCard = createMetricCard(
                "Yield Forecast",
                "+18.2%",
                "Exceeding projections",
                VaadinIcon.TRENDING_UP,
                "linear-gradient(135deg, #e74c3c, #c0392b)", // Red gradient
                "#ffffff" // White text
        );

        // Ensure each card takes up equal width
        revenueCard.setWidth("100%");
        productionCard.setWidth("100%");
        supplyChainCard.setWidth("100%");
        forecastCard.setWidth("100%");

        // Layout for the cards
        HorizontalLayout metricsLayout = new HorizontalLayout(
                revenueCard,
                productionCard,
                supplyChainCard,
                forecastCard
        );

        metricsLayout.setPadding(true);
        metricsLayout.setSpacing(true);
        metricsLayout.setAlignItems(Alignment.STRETCH);
        metricsLayout.setWidth("100%");

        // Make sure each card gets equal width
        metricsLayout.setFlexGrow(1, revenueCard, productionCard, supplyChainCard, forecastCard);

        return metricsLayout;
    }

    private Div createMetricCard(String title, String value, String description, VaadinIcon icon,
                                 String backgroundColor, String textColor) {
        // Card container
        Div card = new Div();
        card.addClassName("metric-card");
        card.getStyle()
                .set("border-radius", "16px")
                .set("padding", "var(--lumo-space-m)")
                .set("background", backgroundColor)
                .set("box-shadow", "0 8px 16px rgba(0, 0, 0, 0.12)")
                .set("color", textColor)
                .set("overflow", "hidden")
                .set("position", "relative")
                .set("transition", "transform 0.3s, box-shadow 0.3s");

        // Add hover effect
        card.getElement().addEventListener("mouseover", e ->
                card.getStyle()
                        .set("transform", "translateY(-5px)")
                        .set("box-shadow", "0 12px 20px rgba(0, 0, 0, 0.18)")
        );
        card.getElement().addEventListener("mouseout", e ->
                card.getStyle()
                        .set("transform", "translateY(0)")
                        .set("box-shadow", "0 8px 16px rgba(0, 0, 0, 0.12)")
        );

        // Decorative element (large translucent icon in background)
        Icon bgIcon = icon.create();
        bgIcon.getStyle()
                .set("position", "absolute")
                .set("right", "-15px")
                .set("bottom", "-15px")
                .set("font-size", "5em")
                .set("opacity", "0.15")
                .set("color", textColor);

        // Title
        H2 titleElement = new H2(title);
        titleElement.getStyle()
                .set("margin", "0 0 10px 0")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("font-weight", "500")
                .set("opacity", "0.9");

        // Value with icon
        Div valueContainer = new Div();
        valueContainer.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-s)")
                .set("margin-bottom", "10px");

        Icon iconElement = icon.create();
        iconElement.getStyle()
                .set("color", textColor)
                .set("width", "24px")
                .set("height", "24px");

        Span valueElement = new Span(value);
        valueElement.getStyle()
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold");

        valueContainer.add(iconElement, valueElement);

        // Description
        Span descriptionElement = new Span(description);
        descriptionElement.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("opacity", "0.8");

        // Add all elements to card
        card.add(bgIcon, titleElement, valueContainer, descriptionElement);

        return card;
    }
}