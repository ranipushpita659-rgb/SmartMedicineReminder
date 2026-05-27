package com.example.smartmedicinereminder.models;

public class UserProfile {
    private String fullName;
    private String age;
    private String gender;
    private String bloodGroup;
    private String diseases;
    private String allergies;
    private String regularDoctorName;
    
    // Emergency Contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    public UserProfile() {
        // Required for Firebase
    }

    public UserProfile(String fullName, String age, String gender, String bloodGroup, String diseases, String allergies, String regularDoctorName, String emergencyContactName, String emergencyContactPhone, String emergencyContactRelationship) {
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.diseases = diseases;
        this.allergies = allergies;
        this.regularDoctorName = regularDoctorName;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.emergencyContactRelationship = emergencyContactRelationship;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getDiseases() { return diseases; }
    public void setDiseases(String diseases) { this.diseases = diseases; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getRegularDoctorName() { return regularDoctorName; }
    public void setRegularDoctorName(String regularDoctorName) { this.regularDoctorName = regularDoctorName; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getEmergencyContactRelationship() { return emergencyContactRelationship; }
    public void setEmergencyContactRelationship(String emergencyContactRelationship) { this.emergencyContactRelationship = emergencyContactRelationship; }
}
