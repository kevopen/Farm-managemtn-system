package com.farmingmgt.system;

public class UserNew {
    private String name;
    private String email;
    private String role;
    private String uid;

    public UserNew() {
    }

    public UserNew(String name, String email, String role, String uid) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
