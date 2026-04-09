package com.example.forkit.utils;
public class CalorieCalculator {

    //activity levels: 0=sedentary, 1=light, 2=moderate, 3=active, 4=very active.
    private static final float[] ACTIVITY_MULTIPLIERS = { 1.2f, 1.375f, 1.55f, 1.725f, 1.9f };
    private static final int WEIGHT_LOSS_DEFICIT = 500;

    public static int calculateDailyCalories(boolean isMale, float weightKg, float heightCm, int age, int activityLevel) {
        // Mifflin-St Jeor BMF formula
        float bmr = isMale
                ? (10 * weightKg + 6.25f * heightCm - 5 * age + 5)
                : (10 * weightKg + 6.25f * heightCm - 5 * age - 161);
        int idx = Math.max(0, Math.min(activityLevel, ACTIVITY_MULTIPLIERS.length - 1));
        int tdee = Math.round(bmr * ACTIVITY_MULTIPLIERS[idx]);
        // TODO: weight loss deficit should not always be subtracted. (currently always assumes weight loss)
        return Math.max(1200, tdee - WEIGHT_LOSS_DEFICIT);
    }
}
