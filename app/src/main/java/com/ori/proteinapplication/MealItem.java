package com.ori.proteinapplication;

public class MealItem {

    private String id;          // מזהה Firestore
    private String imageUrl;
    private int protein;
    private int calories;
    private String date;        // yyyy-MM-dd
    private boolean favorite;   // האם מועדף

    public MealItem() {
        // דרוש ל-Firebase
    }

    public MealItem(String id, String imageUrl, int protein, int calories, String date, boolean favorite) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.protein = protein;
        this.calories = calories;
        this.date = date;
        this.favorite = favorite;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getProtein() { return protein; }
    public void setProtein(int protein) { this.protein = protein; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
}
