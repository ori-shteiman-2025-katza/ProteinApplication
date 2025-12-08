package com.ori.proteinapplication;


public class Meal {
    private String imageUrl;
    private String date;

    public Meal() {} // חובה בשביל Firebase

    public Meal(String imageUrl, String date) {
        this.imageUrl = imageUrl;
        this.date = date;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDate() {
        return date;
    }
}
