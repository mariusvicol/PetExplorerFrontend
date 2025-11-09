package domain;

import java.io.Serializable;

public class PetSittingOffer implements Serializable {
    private Integer id;
    private Integer userId;
    private String name;
    private String description;
    private String location;
    private String phoneNumber;
    private String experience;
    private String availability;

    public PetSittingOffer() {}

    public PetSittingOffer(Integer id, Integer userId, String name, String description, String location, String phoneNumber, String experience, String availability) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.location = location;
        this.phoneNumber = phoneNumber;
        this.experience = experience;
        this.availability = availability;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }
}


