package com.ori.proteinapplication;

public class MealItem {
    private String imageUrl;
    private int protein;
    private int calories;

    public MealItem() {
        // דרוש ל-Firebase
    }

    public MealItem(String imageUrl, int protein, int calories) {
        this.imageUrl = imageUrl;
        this.protein = protein;
        this.calories = calories;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getProtein() {
        return protein;
    }

    public void setProtein(int protein) {
        this.protein = protein;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }
}
