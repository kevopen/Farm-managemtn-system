package com.farmingmgt.system.Views;

import java.time.LocalDate;

public class Crop {
    private String id;
    private String name;
    private String variety;
    private String fieldLocation;
    private String plantingDate;
    private String expectedHarvestDate;
    private String status; // Planted, Growing, Ready for Harvest, Harvested
    private String notes;
    
    public Crop() {
        // Default constructor needed for Firestore
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
    
    public String getVariety() {
        return variety;
    }
    
    public void setVariety(String variety) {
        this.variety = variety;
    }
    
    public String getFieldLocation() {
        return fieldLocation;
    }
    
    public void setFieldLocation(String fieldLocation) {
        this.fieldLocation = fieldLocation;
    }
    
    public String getPlantingDate() {
        return plantingDate;
    }
    
    public void setPlantingDate(String plantingDate) {
        this.plantingDate = plantingDate;
    }
    
    public String getExpectedHarvestDate() {
        return expectedHarvestDate;
    }
    
    public void setExpectedHarvestDate(String expectedHarvestDate) {
        this.expectedHarvestDate = expectedHarvestDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}