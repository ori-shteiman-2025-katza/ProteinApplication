package com.ori.proteinapplication;


public class UserInfo {
    private double weight, height, dailyCalories, dailyProtein;
    private int age, workoutsPerWeek;
    private String gender, intensity;
    private double currentCalories, currentProtein; // הוספנו את השדות האלה

    public UserInfo() { }

    public UserInfo(double weight, double height, int age, String gender, int workoutsPerWeek,
                    String intensity, double dailyCalories, double dailyProtein,
                    double currentCalories, double currentProtein) {
        this.weight = weight;
        this.height = height;
        this.age = age;
        this.gender = gender;
        this.workoutsPerWeek = workoutsPerWeek;
        this.intensity = intensity;
        this.dailyCalories = dailyCalories;
        this.dailyProtein = dailyProtein;
        this.currentCalories = currentCalories;
        this.currentProtein = currentProtein;
    }

    // גטרים וסטרים לכל השדות
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public int getWorkoutsPerWeek() { return workoutsPerWeek; }
    public void setWorkoutsPerWeek(int workoutsPerWeek) { this.workoutsPerWeek = workoutsPerWeek; }
    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; }
    public double getDailyCalories() { return dailyCalories; }
    public void setDailyCalories(double dailyCalories) { this.dailyCalories = dailyCalories; }
    public double getDailyProtein() { return dailyProtein; }
    public void setDailyProtein(double dailyProtein) { this.dailyProtein = dailyProtein; }
    public double getCurrentCalories() { return currentCalories; }
    public void setCurrentCalories(double currentCalories) { this.currentCalories = currentCalories; }
    public double getCurrentProtein() { return currentProtein; }
    public void setCurrentProtein(double currentProtein) { this.currentProtein = currentProtein; }
}
