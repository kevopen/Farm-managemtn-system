package com.farmingmgt.system.Views;

public class Vendor {
    private String id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String itemsSupplied;
    private String lastOrderDate;
    
    public Vendor() {
        // Default constructor for Firestore
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getContactPerson() {
        return contactPerson;
    }
    
    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getItemsSupplied() {
        return itemsSupplied;
    }
    
    public void setItemsSupplied(String itemsSupplied) {
        this.itemsSupplied = itemsSupplied;
    }
    
    public String getLastOrderDate() {
        return lastOrderDate;
    }
    
    public void setLastOrderDate(String lastOrderDate) {
        this.lastOrderDate = lastOrderDate;
    }
}