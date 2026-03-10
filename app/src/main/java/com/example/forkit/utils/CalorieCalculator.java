package com.example.forkit.utils;

/**
 * Calculates daily calorie goal using Mifflin-St Jeor BMR and activity multipliers.
 * Activity levels: 0=Sedentary, 1=Light, 2=Moderate, 3=Very active, 4=Extra active.
 * For weight loss: subtract ~500 kcal for ~0.5kg/week.
 */
public class CalorieCalculator {

    private static final float[] ACTIVITY_MULTIPLIERS = { 1.2f, 1.375f, 1.55f, 1.725f, 1.9f };
    private static final int WEIGHT_LOSS_DEFICIT = 500;

    public static int calculateDailyCalories(boolean isMale, float weightKg, float heightCm, int age, int activityLevel) {
        float bmr = isMale
                ? (10 * weightKg + 6.25f * heightCm - 5 * age + 5)
                : (10 * weightKg + 6.25f * heightCm - 5 * age - 161);
        int idx = Math.max(0, Math.min(activityLevel, ACTIVITY_MULTIPLIERS.length - 1));
        int tdee = Math.round(bmr * ACTIVITY_MULTIPLIERS[idx]);
        return Math.max(1200, tdee - WEIGHT_LOSS_DEFICIT);
    }
}
