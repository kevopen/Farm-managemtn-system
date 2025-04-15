package com.farmingmgt.system.Views;

import com.farmingmgt.system.UserSession;
import com.google.firebase.database.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("checkrole")
public class CheckRoleView extends VerticalLayout {

    private final DatabaseReference testUsersReference = FirebaseDatabase.getInstance().getReference("testusers");

    public CheckRoleView() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Span loadingText = new Span("Checking role...");
        Div loadingAnimation = new Div();
        loadingAnimation.getStyle().set("border", "4px solid #f3f3f3")
                .set("border-top", "4px solid #3498db")
                .set("border-radius", "50%")
                .set("width", "40px")
                .set("height", "40px")
                .set("animation", "spin 1s linear infinite");

        add(loadingText, loadingAnimation);

        // Fetch role and redirect
        checkUserRole();
    }

    private void checkUserRole() {
        String uid = UserSession.getUserUid();
        if (uid == null || uid.isEmpty()) {
            System.out.println("UID is null or empty, redirecting to login.");
            UI.getCurrent().navigate("");
            return;
        }

        System.out.println("Checking role for UID: " + uid);

        // Capture UI instance before making Firebase call
        UI ui = UI.getCurrent();

        testUsersReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    System.out.println("Retrieved data for UID: " + uid);
                    System.out.println("Role: " + role);

                    if (role != null) {
                        System.out.println("This code should be running");

                        if (ui != null) {
                            ui.access(() -> {
                                redirectToDashboard(role);
                            });
                        } else {
                            System.out.println("UI instance is null, cannot navigate.");
                        }

                        UserSession.setUserRole(role);
                        redirectToDashboard(role);
                    } else {
                        System.out.println("Role not found, defaulting to dashboard.");
                        redirectToDashboard("dashboard");
                    }
                } else {
                    System.out.println("Snapshot does not exist for UID: " + uid + ", redirecting to dashboard.");
                    redirectToDashboard("dashboard");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.out.println("Error retrieving role: " + error.getMessage());
                if (ui != null) {
                    ui.access(() -> ui.navigate("dashboard"));
                }
            }
        });
    }

    private void redirectToDashboard(String role) {
        switch (role.toLowerCase()) {
            case "manager":
                UI.getCurrent().navigate("dashboard");
                break;
            case "vendor":
                UI.getCurrent().navigate("vendor-dashboard");
                break;
            case "customer":
                UI.getCurrent().navigate("customer-dashboard");
                break;
            default:
                UI.getCurrent().navigate("dashboard");
                break;
        }
    }
}
