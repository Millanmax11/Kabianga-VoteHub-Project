/* THIS IS A JAVA CLASS FOR SENIOR FRAGMENT */
package com.example.skills;

public class Senior {
    private String name;
    private String year;
    private String position;
    private String username;
    private String image;

    public Senior() {
        // Needed for Firestore
    }

    public Senior(String name, String year, String position, String image, String username) {
        this.name = name;
        this.year = year;
        this.position = position;
        this.image = image;
        this.username = username;
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

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
