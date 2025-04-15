package com.farmingmgt.system.Views;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Route(value = "production", layout = HomeLayout.class)
@PageTitle("Production")
public class Production extends VerticalLayout {
    private final UI uiInstance;
    private final Firestore firestore;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
    private LocalDate currentMonth = LocalDate.now();
    private Div calendarContainer;
    private HorizontalLayout calendarHeader;
    private Span monthYearLabel;
    private Tabs viewTabs;
    
    // Overview cards
    private Div upcomingHarvestsCard;
    private Div activeFieldsCard;
    private Div livestockStatusCard;
    private Div weatherForecastCard;
    
    public Production() {
        this.uiInstance = UI.getCurrent();
        this.firestore = FirestoreClient.getFirestore();
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        
        // Create header with title and date
        add(createHeader());
        
        // Create overview section with cards
        add(createOverviewSection());
        
        // Create calendar section
        add(createCalendarSection());
        
        // Load data
        loadProductionData();
    }
    
    private HorizontalLayout createHeader() {
        H1 title = new H1("Production Planning");
        title.getStyle().set("margin", "0");
        
        // Current date display
        LocalDate today = LocalDate.now();
        Span dateSpan = new Span("Thursday, " + today.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        dateSpan.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        
        // Search field
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search activities...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.getStyle().set("margin-left", "auto");
        
        // Notification icon
        Button notificationButton = new Button(VaadinIcon.BELL.create());
        notificationButton.getStyle().set("margin-left", "var(--lumo-space-m)");
        
        // Settings icon
        Button settingsButton = new Button(VaadinIcon.COG.create());
        settingsButton.getStyle().set("margin-left", "var(--lumo-space-s)");
        
        // Add activity button
        Button addActivityButton = new Button("Add Activity", VaadinIcon.PLUS.create());
        addActivityButton.getStyle()
                .set("margin-left", "var(--lumo-space-m)")
                .set("background-color", "var(--lumo-primary-color)")
                .set("color", "var(--lumo-base-color)");
        addActivityButton.addClickListener(e -> showAddActivityDialog());
        
        // Layout for title and date
        VerticalLayout titleLayout = new VerticalLayout(title, dateSpan);
        titleLayout.setSpacing(false);
        titleLayout.setPadding(false);
        
        // Header layout
        HorizontalLayout header = new HorizontalLayout(titleLayout, searchField, notificationButton, settingsButton, addActivityButton);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setPadding(true);
        header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        
        return header;
    }
    
    private HorizontalLayout createOverviewSection() {
        // Create overview cards
        upcomingHarvestsCard = createOverviewCard(
                "Upcoming Harvests",
                "12",
                "Next: Corn (North Field)",
                "in 5 days",
                VaadinIcon.CALENDAR,
                "#4caf50"
        );
        
        activeFieldsCard = createOverviewCard(
                "Active Crop Fields",
                "8/12",
                "2 fields",
                "vs last season",
                VaadinIcon.GRID_SMALL,
                "#2196f3"
        );
        
        livestockStatusCard = createOverviewCard(
                "Livestock Status",
                "245 head",
                "98% Healthy",
                "5 under treatment",
                VaadinIcon.USERS,
                "#9c27b0"
        );
        
        weatherForecastCard = createOverviewCard(
                "Weather Forecast",
                "72°F",
                "Rain expected",
                "in 3 days",
                VaadinIcon.SUN_O,
                "#ff9800"
        );
        
        // Layout for cards
        HorizontalLayout overviewLayout = new HorizontalLayout(
                upcomingHarvestsCard,
                activeFieldsCard,
                livestockStatusCard,
                weatherForecastCard
        );
        overviewLayout.setWidthFull();
        overviewLayout.setPadding(true);
        overviewLayout.setSpacing(true);
        overviewLayout.setAlignItems(Alignment.STRETCH);
        overviewLayout.setFlexGrow(1, upcomingHarvestsCard, activeFieldsCard, livestockStatusCard, weatherForecastCard);
        
        return overviewLayout;
    }
    
    private Div createOverviewCard(String title, String value, String detail1, String detail2, VaadinIcon icon, String iconColor) {
        Div card = new Div();
        card.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("padding", "var(--lumo-space-m)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("height", "100%");
        
        // Icon with background
        Div iconContainer = new Div();
        iconContainer.getStyle()
                .set("background-color", iconColor + "10") // 10% opacity
                .set("border-radius", "50%")
                .set("width", "40px")
                .set("height", "40px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("margin-bottom", "var(--lumo-space-s)");
        
        Icon iconElement = icon.create();
        iconElement.getStyle().set("color", iconColor);
        iconContainer.add(iconElement);
        
        // Title
        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "var(--lumo-space-xs)");
        
        // Value
        H2 valueH2 = new H2(value);
        valueH2.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "bold")
                .set("margin-bottom", "var(--lumo-space-s)");
        
        // Details
        HorizontalLayout detailsLayout = new HorizontalLayout();
        detailsLayout.setSpacing(false);
        detailsLayout.setPadding(false);
        detailsLayout.setWidthFull();
        
        Span detail1Span = new Span(detail1);
        detail1Span.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
        
        Span detail2Span = new Span(detail2);
        detail2Span.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-left", "auto");
        
        detailsLayout.add(detail1Span, detail2Span);
        
        // Add all elements to card
        card.add(iconContainer, titleSpan, valueH2, detailsLayout);
        
        return card;
    }
    
    private VerticalLayout createCalendarSection() {
        // Calendar title and view selector
        H2 calendarTitle = new H2("Production Calendar");
        calendarTitle.getStyle().set("margin-top", "var(--lumo-space-m)");
        
        // View tabs (Day, Week, Month)
        Tab dayTab = new Tab("Day");
        Tab weekTab = new Tab("Week");
        Tab monthTab = new Tab("Month");
        viewTabs = new Tabs(dayTab, weekTab, monthTab);
        viewTabs.setSelectedTab(monthTab); // Default to month view
        
        // Month navigation
        Button prevMonthButton = new Button(VaadinIcon.ANGLE_LEFT.create());
        prevMonthButton.addClickListener(e -> navigateMonth(-1));
        
        monthYearLabel = new Span(currentMonth.format(dateFormatter));
        monthYearLabel.getStyle()
                .set("font-weight", "bold")
                .set("margin", "0 var(--lumo-space-m)");
        
        Button nextMonthButton = new Button(VaadinIcon.ANGLE_RIGHT.create());
        nextMonthButton.addClickListener(e -> navigateMonth(1));
        
        // Export button
        Button exportButton = new Button("Export", VaadinIcon.DOWNLOAD.create());
        exportButton.getStyle().set("margin-left", "auto");
        
        // Calendar header layout
        calendarHeader = new HorizontalLayout(prevMonthButton, monthYearLabel, nextMonthButton, exportButton);
        calendarHeader.setWidthFull();
        calendarHeader.setAlignItems(Alignment.CENTER);
        
        // Calendar container
        calendarContainer = new Div();
        calendarContainer.setWidthFull();
        calendarContainer.getStyle().set("margin-top", "var(--lumo-space-m)");
        
        // Create the calendar view
        updateCalendarView();
        
        // Calendar section layout
        VerticalLayout calendarSection = new VerticalLayout(calendarTitle, viewTabs, calendarHeader, calendarContainer);
        calendarSection.setPadding(true);
        calendarSection.setSpacing(false);
        
        return calendarSection;
    }
    
    private void updateCalendarView() {
        calendarContainer.removeAll();
        
        // Create day headers (Sun, Mon, etc.)
        HorizontalLayout dayHeaders = new HorizontalLayout();
        dayHeaders.setWidthFull();
        dayHeaders.setSpacing(false);
        dayHeaders.setPadding(false);
        
        for (DayOfWeek day : DayOfWeek.values()) {
            Div dayHeader = new Div(new Span(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())));
            dayHeader.setWidthFull();
            dayHeader.getStyle()
                    .set("text-align", "center")
                    .set("font-weight", "bold")
                    .set("padding", "var(--lumo-space-s)")
                    .set("color", day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY ? 
                            "var(--lumo-error-text-color)" : "var(--lumo-secondary-text-color)");
            dayHeaders.add(dayHeader);
            dayHeaders.setFlexGrow(1, dayHeader);
        }
        
        // Create calendar grid
        Div calendarGrid = new Div();
        calendarGrid.setWidthFull();
        calendarGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(7, 1fr)")
                .set("grid-gap", "1px")
                .set("background-color", "var(--lumo-contrast-10pct)");
        
        // Get the first day of the month and the last day
        YearMonth yearMonth = YearMonth.from(currentMonth);
        LocalDate firstOfMonth = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();
        
        // Calculate the day of week for the first day (0 = Sunday, 6 = Saturday)
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
        
        // Add days from previous month to fill the first row
        LocalDate prevMonth = currentMonth.minusMonths(1);
        YearMonth prevYearMonth = YearMonth.from(prevMonth);
        int daysInPrevMonth = prevYearMonth.lengthOfMonth();
        
        for (int i = 0; i < firstDayOfWeek; i++) {
            int day = daysInPrevMonth - firstDayOfWeek + i + 1;
            Div dayCell = createDayCell(day, true);
            calendarGrid.add(dayCell);
        }
        
        // Add days of current month
        LocalDate today = LocalDate.now();
        
        // Create a map to store activities by date
        Map<String, List<Map<String, Object>>> activitiesByDate = new HashMap<>();
        
        // Fetch activities from Firestore
        try {
            QuerySnapshot activitiesSnapshot = firestore.collection("activities").get().get();
            
            // Group activities by date
            for (QueryDocumentSnapshot doc : activitiesSnapshot.getDocuments()) {
                String dateStr = doc.getString("date");
                if (dateStr != null) {
                    LocalDate activityDate = LocalDate.parse(dateStr);
                    
                    // Only include activities for the current month
                    if (activityDate.getMonth() == currentMonth.getMonth() && 
                        activityDate.getYear() == currentMonth.getYear()) {
                        
                        String key = activityDate.toString();
                        if (!activitiesByDate.containsKey(key)) {
                            activitiesByDate.put(key, new ArrayList<>());
                        }
                        
                        Map<String, Object> activityData = new HashMap<>();
                        activityData.put("title", doc.getString("title"));
                        activityData.put("type", doc.getString("type"));
                        activityData.put("location", doc.getString("location"));
                        
                        activitiesByDate.get(key).add(activityData);
                    }
                }
            }
            
            // Add days of current month with activities
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), day);
                boolean isToday = date.equals(today);
                
                Div dayCell = createDayCell(day, false);
                if (isToday) {
                    dayCell.getStyle()
                            .set("background-color", "var(--lumo-primary-color-10pct)")
                            .set("font-weight", "bold");
                }
                
                // Add activities for this day from Firestore data
                String dateKey = date.toString();
                if (activitiesByDate.containsKey(dateKey)) {
                    for (Map<String, Object> activity : activitiesByDate.get(dateKey)) {
                        String title = (String) activity.get("title");
                        String type = (String) activity.get("type");
                        
                        // Determine color based on activity type
                        String color = "#9e9e9e"; // Default gray
                        if ("Planting".equals(type)) {
                            color = "#2196f3"; // Blue
                        } else if ("Harvesting".equals(type)) {
                            color = "#4caf50"; // Green
                        } else if ("Maintenance".equals(type)) {
                            color = "#ff9800"; // Orange
                        } else if ("Feeding".equals(type)) {
                            color = "#9c27b0"; // Purple
                        }
                        
                        addActivityToCell(dayCell, title, color);
                    }
                }
                
                calendarGrid.add(dayCell);
            }
            
        } catch (InterruptedException | ExecutionException e) {
            Notification.show("Error loading activities: " + e.getMessage())
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            
            // If there's an error, still show the calendar without activities
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = LocalDate.of(currentMonth.getYear(), currentMonth.getMonth(), day);
                boolean isToday = date.equals(today);
                
                Div dayCell = createDayCell(day, false);
                if (isToday) {
                    dayCell.getStyle()
                            .set("background-color", "var(--lumo-primary-color-10pct)")
                            .set("font-weight", "bold");
                }
                
                calendarGrid.add(dayCell);
            }
        }
        
        // Add days from next month to complete the grid
        int remainingCells = 7 - ((firstDayOfWeek + daysInMonth) % 7);
        if (remainingCells < 7) {
            for (int day = 1; day <= remainingCells; day++) {
                Div dayCell = createDayCell(day, true);
                calendarGrid.add(dayCell);
            }
        }
        
        // Add legend
        HorizontalLayout legend = createLegend();
        
        // Add everything to the calendar container
        calendarContainer.add(dayHeaders, calendarGrid, legend);
    }
    
    private Div createDayCell(int day, boolean isOtherMonth) {
        Div dayCell = new Div();
        dayCell.getStyle()
                .set("background-color", "var(--lumo-base-color)")
                .set("min-height", "100px")
                .set("padding", "var(--lumo-space-xs)")
                .set("display", "flex")
                .set("flex-direction", "column");
        
        Span dayNumber = new Span(String.valueOf(day));
        dayNumber.getStyle()
                .set("color", isOtherMonth ? "var(--lumo-tertiary-text-color)" : "var(--lumo-body-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("margin-bottom", "var(--lumo-space-xs)");
        
        dayCell.add(dayNumber);
        return dayCell;
    }
    
    private void addActivityToCell(Div dayCell, String activityName, String color) {
        Div activity = new Div(new Span(activityName));
        activity.getStyle()
                .set("background-color", color + "20") // 20% opacity
                .set("color", color)
                .set("border-radius", "4px")
                .set("padding", "2px 4px")
                .set("margin-bottom", "2px")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
        
        dayCell.add(activity);
    }
    
    private HorizontalLayout createLegend() {
        HorizontalLayout legend = new HorizontalLayout();
        legend.setWidthFull();
        legend.setJustifyContentMode(JustifyContentMode.CENTER);
        legend.setSpacing(true);
        legend.getStyle().set("margin-top", "var(--lumo-space-m)");
        
        legend.add(createLegendItem("Today", "var(--lumo-primary-color-10pct)"));
        legend.add(createLegendItem("Planting", "#2196f3"));
        legend.add(createLegendItem("Harvesting", "#4caf50"));
        legend.add(createLegendItem("Maintenance", "#ff9800"));
        legend.add(createLegendItem("Feeding", "#9c27b0"));
        
        return legend;
    }
    
    private Div createLegendItem(String label, String color) {
        Div item = new Div();
        item.getStyle().set("display", "flex").set("align-items", "center");
        
        Div colorBox = new Div();
        colorBox.getStyle()
                .set("width", "12px")
                .set("height", "12px")
                .set("background-color", color)
                .set("border-radius", "2px")
                .set("margin-right", "4px");
        
        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");
        
        item.add(colorBox, labelSpan);
        return item;
    }
    
    private void navigateMonth(int monthsToAdd) {
        currentMonth = currentMonth.plusMonths(monthsToAdd);
        monthYearLabel.setText(currentMonth.format(dateFormatter));
        updateCalendarView();
    }
    
    private void showAddActivityDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Activity");
        
        // Form fields
        TextField titleField = new TextField("Activity Title");
        titleField.setWidthFull();
        
        ComboBox<String> typeField = new ComboBox<>("Activity Type");
        typeField.setItems("Planting", "Harvesting", "Maintenance", "Feeding", "Other");
        typeField.setWidthFull();
        
        TextField locationField = new TextField("Location");
        locationField.setWidthFull();
        
        DatePicker dateField = new DatePicker("Date");
        dateField.setValue(LocalDate.now());
        dateField.setWidthFull();
        
        // Buttons
        Button saveButton = new Button("Save", e -> {
            if (titleField.isEmpty() || typeField.isEmpty() || locationField.isEmpty() || dateField.isEmpty()) {
                Notification.show("Please fill in all fields").addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            saveActivity(titleField.getValue(), typeField.getValue(), locationField.getValue(), dateField.getValue());
            dialog.close();
        });
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        buttonLayout.setWidthFull();
        
        VerticalLayout dialogLayout = new VerticalLayout(titleField, typeField, locationField, dateField, buttonLayout);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialogLayout.setSizeFull();
        
        dialog.add(dialogLayout);
        dialog.setWidth("400px");
        dialog.open();
    }
    
    private void saveActivity(String title, String type, String location, LocalDate date) {
        Map<String, Object> activity = new HashMap<>();
        activity.put("title", title);
        activity.put("type", type);
        activity.put("location", location);
        activity.put("date", date.toString());
        activity.put("createdAt", LocalDate.now().toString());
        
        try {
            // Add activity to Firestore
            DocumentReference result = firestore.collection("activities").add(activity).get();
            
            uiInstance.access(() -> {
                Notification.show("Activity added successfully");
                // Calendar will be updated automatically via the snapshot listener
            });
        } catch (InterruptedException | ExecutionException e) {
            uiInstance.access(() -> {
                Notification.show("Error adding activity: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            });
        }
    }
    
    private void loadProductionData() {
        try {
            // Load upcoming harvests
            firestore.collection("harvests").get().get().getDocuments().forEach(doc -> {
                // Update UI with harvest data
                uiInstance.access(() -> {
                    String cropName = doc.getString("cropName");
                    String fieldName = doc.getString("fieldName");
                    String harvestDate = doc.getString("harvestDate");
                    
                    if (cropName != null && fieldName != null && harvestDate != null) {
                        upcomingHarvestsCard.removeAll();
                        
                        // Recalculate days until harvest
                        LocalDate today = LocalDate.now();
                        LocalDate harvest = LocalDate.parse(harvestDate);
                        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, harvest);
                        
                        // Create new card content
                        Div iconContainer = (Div) upcomingHarvestsCard.getChildren().findFirst().orElse(null);
                        if (iconContainer != null) {
                            upcomingHarvestsCard.add(iconContainer);
                            
                            // Add updated title
                            Span titleSpan = new Span("Upcoming Harvests");
                            titleSpan.getStyle()
                                    .set("font-size", "var(--lumo-font-size-s)")
                                    .set("color", "var(--lumo-secondary-text-color)")
                                    .set("margin-bottom", "var(--lumo-space-xs)");
                            upcomingHarvestsCard.add(titleSpan);
                            
                            // Add count
                            H2 valueH2 = new H2(String.valueOf(doc.getReference().getParent()));
                            valueH2.getStyle()
                                    .set("margin", "0")
                                    .set("font-size", "var(--lumo-font-size-xxl)")
                                    .set("font-weight", "bold")
                                    .set("margin-bottom", "var(--lumo-space-s)");
                            upcomingHarvestsCard.add(valueH2);
                            
                            // Add details
                            HorizontalLayout detailsLayout = new HorizontalLayout();
                            detailsLayout.setSpacing(false);
                            detailsLayout.setPadding(false);
                            detailsLayout.setWidthFull();
                            
                            Span detail1Span = new Span("Next: " + cropName + " (" + fieldName + ")");
                            detail1Span.getStyle()
                                    .set("font-size", "var(--lumo-font-size-s)")
                                    .set("color", "var(--lumo-secondary-text-color)");
                            
                            Span detail2Span = new Span("in " + daysUntil + " days");
                            detail2Span.getStyle()
                                    .set("font-size", "var(--lumo-font-size-xs)")
                                    .set("color", "var(--lumo-tertiary-text-color)")
                                    .set("margin-left", "auto");
                            
                            detailsLayout.add(detail1Span, detail2Span);
                            upcomingHarvestsCard.add(detailsLayout);
                        }
                    }
                });
            });
            
            // Load active fields
            firestore.collection("fields").get().get().getDocuments().forEach(doc -> {
                // Update UI with field data
                uiInstance.access(() -> {
                    // Update active fields card with real data
                    // Similar implementation as above
                });
            });
            
            // Load livestock data
            firestore.collection("livestock").get().get().getDocuments().forEach(doc -> {
                // Update UI with livestock data
                uiInstance.access(() -> {
                    // Update livestock card with real data
                    // Similar implementation as above
                });
            });
            
            // Set up real-time listener for activities collection
            setupActivityListener();
            
        } catch (InterruptedException | ExecutionException e) {
            uiInstance.access(() -> {
                Notification.show("Error loading data: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            });
        }
    }
    
    private void setupActivityListener() {
        // Listen for real-time updates to activities collection
        firestore.collection("activities").addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                uiInstance.access(() -> {
                    Notification.show("Error loading activities: " + error.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                });
                return;
            }
            
            if (snapshots != null) {
                uiInstance.access(() -> {
                    // Refresh calendar with latest data
                    updateCalendarView();
                });
            }
        });
    }
}