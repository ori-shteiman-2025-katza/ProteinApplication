package com.ori.proteinapplication;

import java.util.Map;

 class Meal {
    private String base64Image;
    private int protein;
    private int calories;
    private Map<String, Double> components;
    private String date;
    private boolean favorite;
    private String id; // מזהה המסמך ב-Firestore

    public Meal(String base64Image, int protein, int calories, Map<String, Double> components,
                String date, boolean favorite, String id) {
        this.base64Image = base64Image;
        this.protein = protein;
        this.calories = calories;
        this.components = components;
        this.date = date;
        this.favorite = favorite;
        this.id = id;
    }

    // getter ו-setter ל-id
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }


    public String getBase64Image() {
        return base64Image;
    }

    public int getProtein() {
        return protein;
    }

    public int getCalories() {
        return calories;
    }

    public Map<String, Double> getComponents() {
        return components;
    }

    public String getDate() {
        return date;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
