package com.farmingmgt.system.Views;

public class InventoryItem {
    private String id;
    private String name;
    private String category;
    private int quantity;
    private String unit;
    private int reorderLevel;
    private String supplier;
    private String lastRestocked;
    
    public InventoryItem() {
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public int getReorderLevel() {
        return reorderLevel;
    }
    
    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
    
    public String getSupplier() {
        return supplier;
    }
    
    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }
    
    public String getLastRestocked() {
        return lastRestocked;
    }
    
    public void setLastRestocked(String lastRestocked) {
        this.lastRestocked = lastRestocked;
    }
}