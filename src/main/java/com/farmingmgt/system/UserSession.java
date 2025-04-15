package com.farmingmgt.system;

import com.vaadin.flow.server.VaadinSession;

public class UserSession {

    private static final String UID_ATTRIBUTE = "userUid";
    private static final String ROLE_ATTRIBUTE = "userRole";

    public UserSession() {
    }

    // Store the UID in session
    public static void setUserUid(String uid) {
        VaadinSession.getCurrent().setAttribute(UID_ATTRIBUTE, uid);
    }

    // Retrieve the UID from session
    public static String getUserUid() {
        return (String) VaadinSession.getCurrent().getAttribute(UID_ATTRIBUTE);
    }

    // Store the role in session
    public static void setUserRole(String role) {
        VaadinSession.getCurrent().setAttribute(ROLE_ATTRIBUTE, role);
    }

    // Retrieve the role from session
    public static String getUserRole() {
        return (String) VaadinSession.getCurrent().getAttribute(ROLE_ATTRIBUTE);
    }

    // Role check helper methods
    public static boolean isAdmin() {
        String role = getUserRole();
        return role != null && role.equalsIgnoreCase("admin");
    }

    public static boolean isTeacher() {
        String role = getUserRole();
        return role != null && role.equalsIgnoreCase("teacher");
    }

    public static boolean isStudent() {
        String role = getUserRole();
        return role != null && role.equalsIgnoreCase("student");
    }

    // Clear the session (for logout)
    public static void clearSession() {
        VaadinSession.getCurrent().setAttribute(UID_ATTRIBUTE, null);
        VaadinSession.getCurrent().setAttribute(ROLE_ATTRIBUTE, null);
    }
}
