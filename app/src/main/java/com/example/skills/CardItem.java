package com.example.skills;
// CardItem.java
public class CardItem {
    private int imageResId;
    private String statement;
    private String buttonText;

    public CardItem(int imageResId, String statement, String buttonText) {
        this.imageResId = imageResId;
        this.statement = statement;
        this.buttonText = buttonText;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getStatement() {
        return statement;
    }

    public String getButtonText() {
        return buttonText;
    }
}
