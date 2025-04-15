package com.farmingmgt.system.Views;

import com.farmingmgt.system.FirebaseAuthREST;
import com.farmingmgt.system.UserSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.*;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("") // Main page
public class MainLayout extends VerticalLayout {

    private FirebaseAuth mAuth;
    private LoginForm loginForm;
    private final DatabaseReference usersReference = FirebaseDatabase.getInstance().getReference("users");
    private UI uiInstance;
    public MainLayout() {
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        this.uiInstance = UI.getCurrent();
        mAuth = FirebaseAuth.getInstance();
        loginForm = new LoginForm();
        loginForm.getStyle().set("border-radius", "10px");
        loginForm.getStyle().set("box-shadow", "0 0 10px rgba(0, 0, 0, 0.2)");
        loginForm.setI18n(createCustomI18n());
        loginForm.addLoginListener(event -> authenticate(event.getUsername(), event.getPassword()));
        loginForm.addForgotPasswordListener(event -> Notification.show("Reset password option coming soon!"));
        Span signUpText = new Span("Don't have an account? Sign Up");
        signUpText.getStyle().set("cursor", "pointer");
        signUpText.getStyle().set("color", "blue");
        signUpText.addClickListener(new ComponentEventListener<ClickEvent<Span>>() {
            @Override
            public void onComponentEvent(ClickEvent<Span> event) {
                UI.getCurrent().navigate("register");
            }
        });
        add(loginForm, signUpText);

    }

    private void authenticate(String email, String password) {
        try {
            UserRecord userRecord = mAuth.getUserByEmail(email);
            String uid = userRecord.getUid();
            if (uid != null && !uid.isEmpty() && FirebaseAuthREST.verifyUser(email, password)) {
                UserSession.setUserUid(uid);
                checkUserRoleAndRedirect(uid);
            } else {
                loginForm.setError(true);
            }
        } catch (Exception e) {
            loginForm.setError(true);
            Notification notification = Notification.show("Login failed: " + e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void checkUserRoleAndRedirect(String uid) {
        usersReference.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                uiInstance.access(() -> {
                    if (snapshot.exists()) {
                        String role = snapshot.child("role").getValue(String.class);
                        UserSession.setUserRole(role);

                        // Redirect based on role
                        if (role != null) {
                            switch (role.toLowerCase()) {
                                case "farm-manager":
                                    Notification.show("Welcome, Farm Manager!");
                                    uiInstance.navigate("manager-dashboard");
                                    break;
                                case "vendor":
                                    Notification.show("Welcome, Vendor!");
                                    uiInstance.navigate("vendor-dashboard");
                                    break;
                                case "customer":
                                    Notification.show("Welcome, Customer!");
                                    uiInstance.navigate("customer-dashboard");
                                    break;
                                default:
                                    Notification.show("Login successful!");
                                    uiInstance.navigate("dashboard");
                                    break;
                            }
                        } else {
                            Notification.show("Login successful!");
                            uiInstance.navigate("dashboard");
                        }
                    } else {
                        Notification.show("Login successful!");
                        uiInstance.navigate("dashboard");
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                uiInstance.access(() -> {
                    Notification notification = Notification.show("Error checking user role: " + error.getMessage());
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    uiInstance.navigate("dashboard"); // Default fallback
                });
            }
        });
    }

    private LoginI18n createCustomI18n() {
        LoginI18n i18n = LoginI18n.createDefault();
        LoginI18n.Form form = i18n.getForm();
        form.setUsername("Email");
        form.setPassword("Password");
        form.setTitle("Login");
        form.setSubmit("Sign in");
        form.setForgotPassword("Forgot password?");
        i18n.setForm(form);
        return i18n;
    }
}

// Add UserSession class to store user role


