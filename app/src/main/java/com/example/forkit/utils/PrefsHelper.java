package com.example.forkit.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

public class PrefsHelper {

    private static final String PREFS = "forkit_prefs";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_LAST_LOG_DATE = "last_log_date";
    private static final String KEY_WATER_PREFIX = "water_";
    private static final String KEY_WATER_DATE_PREFIX = "waterdate_";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_HANDLE = "user_handle";
    private static final String KEY_WEIGHT = "weight_kg";
    private static final String KEY_AGE = "age";
    private static final String KEY_HEIGHT_CM = "height_cm";
    private static final String KEY_GOAL_WEIGHT = "goal_weight_kg";
    private static final String KEY_ACTIVITY_LEVEL = "activity_level";
    private static final String KEY_IS_MALE = "is_male";
    private static final String KEY_STEPS = "steps_today";
    private static final String KEY_EXERCISE_MINS = "exercise_mins";
    private static final String KEY_WATER_GOAL_ML = "water_goal_ml";
    private static final String KEY_DIETARY_PREFS = "dietary_prefs";
    private static final String KEY_PROFILE_PIC = "profile_pic_uri";
    private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
    private static final String KEY_REMINDER_TIMES = "reminder_times";
    private static final String KEY_CALORIE_GOAL = "calorie_goal";
    private static final String KEY_PROTEIN_GOAL = "protein_goal";
    private static final String KEY_CARBS_GOAL = "carbs_goal";
    private static final String KEY_FAT_GOAL = "fat_goal";
    private static final String KEY_CALORIES_BURNED = "calories_burned";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final int DEFAULT_WATER_GOAL = 2000;

    private final SharedPreferences prefs;

    public PrefsHelper(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getStreak() {
        return prefs.getInt(KEY_STREAK, 0);
    }

    public void onFoodLogged() {
        String today = getTodayKey();
        String last = prefs.getString(KEY_LAST_LOG_DATE, "");
        int streak = prefs.getInt(KEY_STREAK, 0);
        if (last.isEmpty()) {
            prefs.edit().putString(KEY_LAST_LOG_DATE, today).putInt(KEY_STREAK, 1).apply();
        } else if (last.equals(today)) {
            return;
        } else if (isYesterday(last)) {
            prefs.edit().putString(KEY_LAST_LOG_DATE, today).putInt(KEY_STREAK, streak + 1).apply();
        } else {
            prefs.edit().putString(KEY_LAST_LOG_DATE, today).putInt(KEY_STREAK, 1).apply();
        }
    }

    private String getTodayKey() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isYesterday(String key) {
        try {
            String[] p = key.split("-");
            int y = Integer.parseInt(p[0]);
            int d = Integer.parseInt(p[1]);
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -1);
            return c.get(Calendar.YEAR) == y && c.get(Calendar.DAY_OF_YEAR) == d;
        } catch (Exception e) {
            return false;
        }
    }

    public int getWaterForDay(int dayIndex) {
        return prefs.getInt(KEY_WATER_PREFIX + dayIndex, 0);
    }

    public void setWaterForDay(int dayIndex, int ml) {
        prefs.edit().putInt(KEY_WATER_PREFIX + dayIndex, Math.max(0, ml)).apply();
    }

    public void addWaterForDay(int dayIndex, int ml) {
        setWaterForDay(dayIndex, getWaterForDay(dayIndex) + ml);
    }

    // ---- Date-keyed water history (for History screen) ----
    public int getWaterForDateKey(String key) {
        if (key == null) return 0;
        return prefs.getInt(KEY_WATER_DATE_PREFIX + key, 0);
    }

    public void setWaterForDateKey(String key, int ml) {
        if (key == null) return;
        prefs.edit().putInt(KEY_WATER_DATE_PREFIX + key, Math.max(0, ml)).apply();
    }

    public void addWaterForToday(int ml) {
        String key = getTodayKeySG();
        setWaterForDateKey(key, getWaterForDateKey(key) + Math.max(0, ml));
    }

    public String getTodayKeySG() {
        Calendar c = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Singapore"));
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
    }

    public int getWaterGoalMl() {
        return prefs.getInt(KEY_WATER_GOAL_ML, DEFAULT_WATER_GOAL);
    }

    public void setWaterGoalMl(int ml) {
        prefs.edit().putInt(KEY_WATER_GOAL_ML, ml).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void setUserName(String s) {
        prefs.edit().putString(KEY_USER_NAME, s).apply();
    }

    public String getUserHandle() {
        return prefs.getString(KEY_USER_HANDLE, "");
    }

    public void setUserHandle(String s) {
        prefs.edit().putString(KEY_USER_HANDLE, s).apply();
    }

    public float getWeight() {
        return prefs.getFloat(KEY_WEIGHT, 70f);
    }

    public void setWeight(float f) {
        prefs.edit().putFloat(KEY_WEIGHT, f).apply();
    }

    public int getAge() {
        return prefs.getInt(KEY_AGE, 25);
    }

    public void setAge(int n) {
        prefs.edit().putInt(KEY_AGE, n).apply();
    }

    public float getHeightCm() {
        return prefs.getFloat(KEY_HEIGHT_CM, 170f);
    }

    public void setHeightCm(float f) {
        prefs.edit().putFloat(KEY_HEIGHT_CM, f).apply();
    }

    public float getGoalWeight() {
        return prefs.getFloat(KEY_GOAL_WEIGHT, 70f);
    }

    public void setGoalWeight(float f) {
        prefs.edit().putFloat(KEY_GOAL_WEIGHT, f).apply();
    }

    public boolean hasAge() {
        return prefs.contains(KEY_AGE);
    }

    public boolean hasHeightCm() {
        return prefs.contains(KEY_HEIGHT_CM);
    }

    public boolean hasWeight() {
        return prefs.contains(KEY_WEIGHT);
    }

    public boolean hasGoalWeight() {
        return prefs.contains(KEY_GOAL_WEIGHT);
    }

    public boolean hasGender() {
        return prefs.contains(KEY_IS_MALE);
    }

    public boolean hasActivityLevel() {
        return prefs.contains(KEY_ACTIVITY_LEVEL);
    }

    public int getActivityLevel() {
        return prefs.getInt(KEY_ACTIVITY_LEVEL, 1);
    }

    public void setActivityLevel(int level) {
        prefs.edit().putInt(KEY_ACTIVITY_LEVEL, level).apply();
    }

    public boolean isMale() {
        return prefs.getBoolean(KEY_IS_MALE, true);
    }

    public void setIsMale(boolean b) {
        prefs.edit().putBoolean(KEY_IS_MALE, b).apply();
    }

    public int getStepsToday() {
        return prefs.getInt(KEY_STEPS, 0);
    }

    public void setStepsToday(int n) {
        prefs.edit().putInt(KEY_STEPS, n).apply();
    }

    public int getExerciseMins() {
        return prefs.getInt(KEY_EXERCISE_MINS, 0);
    }

    public void setExerciseMins(int n) {
        prefs.edit().putInt(KEY_EXERCISE_MINS, n).apply();
    }

    public String getDietaryPrefs() {
        return prefs.getString(KEY_DIETARY_PREFS, "");
    }

    public void setDietaryPrefs(String s) {
        prefs.edit().putString(KEY_DIETARY_PREFS, s).apply();
    }

    public String getProfilePicUri() {
        return prefs.getString(KEY_PROFILE_PIC, "");
    }

    public void setProfilePicUri(String s) {
        prefs.edit().putString(KEY_PROFILE_PIC, s).apply();
    }

    public boolean getRemindersEnabled() {
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, false);
    }

    public void setRemindersEnabled(boolean b) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, b).apply();
    }

    public String getReminderTimes() {
        return prefs.getString(KEY_REMINDER_TIMES, "8:00,12:00,18:00");
    }

    public void setReminderTimes(String s) {
        prefs.edit().putString(KEY_REMINDER_TIMES, s).apply();
    }

    public int getCalorieGoal() {
        return prefs.getInt(KEY_CALORIE_GOAL, 2000);
    }

    public void setCalorieGoal(int n) {
        prefs.edit().putInt(KEY_CALORIE_GOAL, n).apply();
    }

    public float getProteinGoal() {
        return prefs.getFloat(KEY_PROTEIN_GOAL, 150f);
    }

    public void setProteinGoal(float f) {
        prefs.edit().putFloat(KEY_PROTEIN_GOAL, f).apply();
    }

    public float getCarbsGoal() {
        return prefs.getFloat(KEY_CARBS_GOAL, 250f);
    }

    public void setCarbsGoal(float f) {
        prefs.edit().putFloat(KEY_CARBS_GOAL, f).apply();
    }

    public float getFatGoal() {
        return prefs.getFloat(KEY_FAT_GOAL, 65f);
    }

    public void setFatGoal(float f) {
        prefs.edit().putFloat(KEY_FAT_GOAL, f).apply();
    }

    /**
     * Calories burned from activity (steps/watch). 0 until connected.
     */
    public int getCaloriesBurned() {
        return prefs.getInt(KEY_CALORIES_BURNED, 0);
    }

    public void setCaloriesBurned(int n) {
        prefs.edit().putInt(KEY_CALORIES_BURNED, Math.max(0, n)).apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public void setAccessToken(String token) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public void setUserId(String id) {
        prefs.edit().putString(KEY_USER_ID, id).apply();
    }

}
