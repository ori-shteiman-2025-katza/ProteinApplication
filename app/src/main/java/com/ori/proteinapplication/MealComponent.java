package com.ori.proteinapplication;


public class MealComponent {
    public String name;
    public float weight; // בגרם
    public float protein; // חלבון כולל ברכיב
    public float calories; // קלוריות כולל ברכיב

    public MealComponent(String name, float weight, float protein, float calories) {
        this.name = name;
        this.weight = weight;
        this.protein = protein;
        this.calories = calories;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getProtein() {
        return protein;
    }

    public void setProtein(float protein) {
        this.protein = protein;
    }

    public float getCalories() {
        return calories;
    }

    public void setCalories(float calories) {
        this.calories = calories;
    }
}

