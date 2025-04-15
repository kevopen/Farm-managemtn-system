package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.database.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route("manager-home")
@PageTitle("Home")
public class ManagerLayout extends AppLayout {
    private Text userDetails;
    private final UI uiInstance;
    private String uid;

    public ManagerLayout() {
        uid = UserSession.getUserUid();
        this.uiInstance = UI.getCurrent();

        HorizontalLayout header = createHeader();

        // Sidebar (Navigation Drawer)
        SideNav sideNav = createSidebar();
        // Add to layout
        addToNavbar(new DrawerToggle(), header);
        addToDrawer(sideNav);
    }

    private HorizontalLayout createHeader() {
        userDetails = new Text("Loading farm details...");
        String currentUid = UserSession.getUserUid();
        Firestore db = FirestoreClient.getFirestore();

        // Run Firestore logic in a background thread
        new Thread(() -> {
            try {
                // Step 1: Get farm_manager where uid == currentUid
                ApiFuture<QuerySnapshot> future = db.collection("farm_managers")
                        .whereEqualTo("uid", currentUid)
                        .get();

                QuerySnapshot managerSnapshot = future.get();
                if (!managerSnapshot.isEmpty()) {
                    DocumentSnapshot managerDoc = managerSnapshot.getDocuments().get(0);
                    String managerName = managerDoc.getString("name");
                    String farmOwnerId = managerDoc.getString("farmOwnerId");

                    // Step 2: Get farm from farm_settings using farmOwnerId
                    ApiFuture<DocumentSnapshot> farmFuture = db.collection("farm_settings")
                            .document(farmOwnerId)
                            .get();

                    DocumentSnapshot farmDoc = farmFuture.get();

                    String farmName = farmDoc.getString("farmName");
                    String farmAddress = farmDoc.getString("farmAddress");

                    String finalText = managerName;

                    uiInstance.access(() -> {
                        userDetails.setText(finalText);
                    });

                } else {
                    uiInstance.access(() -> {
                        userDetails.setText("Manager not found");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                uiInstance.access(() -> {
                    userDetails.setText("Error loading farm details");
                });
            }
        }).start();

        // Logout Button
        Button logout = new Button("LOGOUT", event -> {
            UserSession.clearSession();
            UI.getCurrent().navigate("");
        });

        // Header Layout
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.add(userDetails, logout);

        return header;
    }
    private SideNav createSidebar() {
        SideNav nav = new SideNav();
        nav.setHeightFull();
        nav.getStyle()
                .set("background-color", "#DCEEFF")
                .set("color", "black")
                .set("transition", "0.3s ease-in-out");

        // Sidebar Items with Icons
        SideNavItem dashboardLink = createNavItem("Dashboard", ManagerDashboard.class, VaadinIcon.DASHBOARD);
        SideNavItem equipment = createNavItem("Equipment Management", Equipment.class, VaadinIcon.MONEY);
        SideNavItem supply = createNavItem("Inventory Management", InventoryMgtNew.class, VaadinIcon.STORAGE);
        SideNavItem cropManagement = createNavItem("Crop Management", CropMgt.class, VaadinIcon.CROP);
        SideNavItem analytics = createNavItem("Reporting & Analytics", AnalyticsMgt.class, VaadinIcon.RECORDS);
        SideNavItem logout = createNavItem("Logout", LogoutManager.class, VaadinIcon.SIGN_OUT);


        // Add items to SideNav
        nav.addItem(dashboardLink, equipment, supply, analytics, cropManagement, logout);

        return nav;
    }

    // Helper method to create SideNavItem with consistent styling
    private SideNavItem createNavItem(String title, Class<? extends Component> view, VaadinIcon icon) {
        SideNavItem item = new SideNavItem(title, view, icon.create());
        item.getStyle()
                .set("font-size", "16px")
                .set("padding", "5px 10px")
                .set("border-radius", "20px")
                .set("transition", "0.2s");

        // Add CSS class for hover effect
        item.addClassName("nav-item");

        return item;
    }


}
