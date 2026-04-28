package com.example.smartmedicinereminder.models;

public class Medicine {
    private String id;
    private String name;
    private String quantity;
    private String timeSlot;
    private String timeRange;
    private String imageUrl; // This will now store the local file path
    private String userId;

    public Medicine() {
    }

    public Medicine(String id, String name, String quantity, String timeSlot, String timeRange, String imageUrl, String userId) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.timeSlot = timeSlot;
        this.timeRange = timeRange;
        this.imageUrl = imageUrl;
        this.userId = userId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}