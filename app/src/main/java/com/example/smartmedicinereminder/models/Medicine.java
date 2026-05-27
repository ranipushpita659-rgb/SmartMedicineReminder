package com.example.smartmedicinereminder.models;

public class Medicine {
    private String id;
    private String medicineName;
    private String dosage;
    private String period; // Morning, Afternoon, Night
    private String medicineTime;
    private String imageUrl;
    private String userId;
    private boolean takenStatus;
    private boolean alarmEnabled;
    private int stockQuantity;

    public Medicine() {
        // Required for Firebase
    }

    public Medicine(String id, String medicineName, String dosage, String period, String medicineTime, String imageUrl, String userId) {
        this.id = id;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.period = period;
        this.medicineTime = medicineTime;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.takenStatus = false;
        this.alarmEnabled = true;
        this.stockQuantity = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    
    public String getMedicineTime() { return medicineTime; }
    public void setMedicineTime(String medicineTime) { this.medicineTime = medicineTime; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public boolean isTakenStatus() { return takenStatus; }
    public void setTakenStatus(boolean takenStatus) { this.takenStatus = takenStatus; }

    public boolean isAlarmEnabled() { return alarmEnabled; }
    public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
}