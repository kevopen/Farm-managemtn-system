package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
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

@Route("vendor")
@PageTitle("Vendor")
public class VendorLayout extends AppLayout {
    private Text userDetails;
    private String uid;

    public VendorLayout() {
        uid = UserSession.getUserUid();
        //notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        // Create header with logo and logout button
        HorizontalLayout header = createHeader();

        // Sidebar (Navigation Drawer)
        SideNav sideNav = createSidebar();
        // Add to layout
        addToNavbar(new DrawerToggle(), header);
        addToDrawer(sideNav);
    }

    private HorizontalLayout createHeader() {
        userDetails = new Text("Vendor");
        String newuid = uid = UserSession.getUserUid();
        // Retrieve user details from Firebase
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(newuid);
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String txtName = dataSnapshot.child("fullname").getValue(String.class);
                getUI().ifPresent(ui -> ui.access(() ->
                        userDetails.setText(txtName != null ? txtName : "Vendor")
                ));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                getUI().ifPresent(ui -> ui.access(() ->
                        userDetails.setText("Error loading data")
                ));
            }
        });

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
        header.add(userDetails);

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
        SideNavItem dashboardLink = createNavItem("Dashboard", VendorDashboard.class, VaadinIcon.DASHBOARD);
        SideNavItem Listings = createNavItem("Listings", ListingsView.class, VaadinIcon.STOCK);
        SideNavItem LOGOUT = createNavItem("Logout", LogoutVendor.class, VaadinIcon.SIGN_OUT);
        // Add items to SideNav
        nav.addItem(dashboardLink,Listings, LOGOUT);

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
