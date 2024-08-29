package com.example.skills;

public class User {
    private String name;
    private String year;
    private String username;
    private String image;

    public User() {
        // Needed for Firestore
    }

    public User(String name, String year, String image) {
        this.name = name;
        this.year = year;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
