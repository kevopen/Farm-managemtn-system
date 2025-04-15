package com.farmingmgt.system;

import com.farmingmgt.system.Views.HomeLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.database.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;

@Route(value = "users", layout = HomeLayout.class)
@PageTitle("Users")
public class UsersView extends HorizontalLayout {

    private final FirebaseAuth auth;
    private final DatabaseReference databaseReference;
    private final Grid<UserNew> userGrid;

    public UsersView() {
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("testusers");

        // Form Fields
        TextField nameField = new TextField("Full Name");
        EmailField emailField = new EmailField("Email");
        PasswordField passwordField = new PasswordField("Password");

        ComboBox<String> roleField = new ComboBox<>("Role");
        roleField.setItems("Admin", "User", "Editor");

        Button addButton = new Button("Add User", event -> addUser(nameField, emailField, passwordField, roleField));

        VerticalLayout formLayout = new VerticalLayout(nameField, emailField, passwordField, roleField, addButton);

        // User Grid
        userGrid = new Grid<>(UserNew.class);
        userGrid.setColumns("name", "email", "role");
        userGrid.setSizeFull();

        loadUsers(); // Fetch users from Firebase

        add(formLayout, userGrid);
        setSizeFull();
    }

    private void addUser(TextField nameField, EmailField emailField, PasswordField passwordField, ComboBox<String> roleField) {
        String name = nameField.getValue();
        String email = emailField.getValue();
        String password = passwordField.getValue();
        String role = roleField.getValue();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || role == null) {
            Notification.show("All fields are required!", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(name);

            UserRecord userRecord = auth.createUser(request);

            UserNew user = new UserNew(name, email, role, userRecord.getUid());
            databaseReference.child(userRecord.getUid()).setValueAsync(user);

            Notification.show("User added successfully!", 3000, Notification.Position.MIDDLE);

            // Clear fields
            nameField.clear();
            emailField.clear();
            passwordField.clear();
            roleField.clear();

            loadUsers(); // Refresh grid after adding a user

        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void loadUsers() {
        UI ui = UI.getCurrent(); // Get UI reference

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<UserNew> users = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    UserNew user = child.getValue(UserNew.class);
                    if (user != null) {
                        users.add(user);
                    }
                }

                if (ui != null) {
                    ui.access(() -> {
                        userGrid.setItems(users); // Update Grid items
                        ui.push(); // Push UI update
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (ui != null) {
                    ui.access(() -> Notification.show("Error fetching users: " + error.getMessage(), 5000, Notification.Position.MIDDLE));
                }
            }
        });
    }
}
