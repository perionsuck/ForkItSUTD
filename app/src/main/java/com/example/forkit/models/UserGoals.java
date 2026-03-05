package com.example.forkit.models;

public class UserGoals {

    private String userName;
    private int dailyCalorieGoal;
    private int dailyBurnGoal;
    private float proteinGoal;
    private float carbsGoal;
    private float fatGoal;

    public UserGoals() {
        this.dailyCalorieGoal = 2000;
        this.dailyBurnGoal = 500;
        this.proteinGoal = 150;
        this.carbsGoal = 250;
        this.fatGoal = 65;
    }

    // Getters
    public String getUserName() { return userName; }
    public int getDailyCalorieGoal() { return dailyCalorieGoal; }
    public int getDailyBurnGoal() { return dailyBurnGoal; }
    public float getProteinGoal() { return proteinGoal; }
    public float getCarbsGoal() { return carbsGoal; }
    public float getFatGoal() { return fatGoal; }

    // Setters
    public void setUserName(String userName) { this.userName = userName; }
    public void setDailyCalorieGoal(int dailyCalorieGoal) { this.dailyCalorieGoal = dailyCalorieGoal; }
    public void setDailyBurnGoal(int dailyBurnGoal) { this.dailyBurnGoal = dailyBurnGoal; }
    public void setProteinGoal(float proteinGoal) { this.proteinGoal = proteinGoal; }
    public void setCarbsGoal(float carbsGoal) { this.carbsGoal = carbsGoal; }
    public void setFatGoal(float fatGoal) { this.fatGoal = fatGoal; }
}