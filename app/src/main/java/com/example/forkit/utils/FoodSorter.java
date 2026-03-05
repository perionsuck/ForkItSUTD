package com.example.forkit.utils;

import com.example.forkit.models.FoodEntry;
import java.util.ArrayList;
import java.util.List;

public class FoodSorter {

    public enum SortBy {
        CALORIES_HIGH,
        CALORIES_LOW,
        NAME_AZ,
        NAME_ZA,
        TIME_NEWEST,
        TIME_OLDEST,
        MEAL_TYPE
    }

    // Main entry point
    public static List<FoodEntry> sort(List<FoodEntry> entries, SortBy sortBy) {
        if (entries == null || entries.size() <= 1) return entries;
        List<FoodEntry> list = new ArrayList<>(entries);

        switch (sortBy) {
            case CALORIES_HIGH:
            case CALORIES_LOW:
                // MERGE SORT for calories
                mergeSort(list, 0, list.size() - 1, sortBy);
                break;
            case NAME_AZ:
            case NAME_ZA:
                // QUICK SORT for alphabetical
                quickSort(list, 0, list.size() - 1, sortBy);
                break;
            case TIME_NEWEST:
            case TIME_OLDEST:
                // INSERTION SORT for time (nearly sorted data)
                insertionSort(list, sortBy);
                break;
            case MEAL_TYPE:
                mergeSort(list, 0, list.size() - 1, sortBy);
                break;
        }
        return list;
    }

    // ── MERGE SORT O(n log n) stable ──
    private static void mergeSort(List<FoodEntry> list, int left, int right, SortBy sortBy) {
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSort(list, left, mid, sortBy);
            mergeSort(list, mid + 1, right, sortBy);
            merge(list, left, mid, right, sortBy);
        }
    }

    private static void merge(List<FoodEntry> list, int left, int mid, int right, SortBy sortBy) {
        List<FoodEntry> leftArr = new ArrayList<>(list.subList(left, mid + 1));
        List<FoodEntry> rightArr = new ArrayList<>(list.subList(mid + 1, right + 1));

        int i = 0, j = 0, k = left;
        while (i < leftArr.size() && j < rightArr.size()) {
            if (compare(leftArr.get(i), rightArr.get(j), sortBy) <= 0) {
                list.set(k++, leftArr.get(i++));
            } else {
                list.set(k++, rightArr.get(j++));
            }
        }
        while (i < leftArr.size()) list.set(k++, leftArr.get(i++));
        while (j < rightArr.size()) list.set(k++, rightArr.get(j++));
    }

    // ── QUICK SORT O(n log n) avg ──
    private static void quickSort(List<FoodEntry> list, int low, int high, SortBy sortBy) {
        if (low < high) {
            int pivot = partition(list, low, high, sortBy);
            quickSort(list, low, pivot - 1, sortBy);
            quickSort(list, pivot + 1, high, sortBy);
        }
    }

    private static int partition(List<FoodEntry> list, int low, int high, SortBy sortBy) {
        FoodEntry pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (compare(list.get(j), pivot, sortBy) <= 0) {
                i++;
                FoodEntry temp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, temp);
            }
        }
        FoodEntry temp = list.get(i + 1);
        list.set(i + 1, list.get(high));
        list.set(high, temp);
        return i + 1;
    }

    // ── INSERTION SORT O(n²) great for small/nearly-sorted ──
    private static void insertionSort(List<FoodEntry> list, SortBy sortBy) {
        for (int i = 1; i < list.size(); i++) {
            FoodEntry key = list.get(i);
            int j = i - 1;
            while (j >= 0 && compare(list.get(j), key, sortBy) > 0) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }

    // ── COMPARATOR ──
    private static int compare(FoodEntry a, FoodEntry b, SortBy sortBy) {
        switch (sortBy) {
            case CALORIES_HIGH:  return Integer.compare(b.getCalories(), a.getCalories());
            case CALORIES_LOW:   return Integer.compare(a.getCalories(), b.getCalories());
            case NAME_AZ:        return a.getFoodName().compareToIgnoreCase(b.getFoodName());
            case NAME_ZA:        return b.getFoodName().compareToIgnoreCase(a.getFoodName());
            case TIME_NEWEST:    return Long.compare(b.getTimestamp(), a.getTimestamp());
            case TIME_OLDEST:    return Long.compare(a.getTimestamp(), b.getTimestamp());
            case MEAL_TYPE:
                int order = mealOrder(a.getMealType()) - mealOrder(b.getMealType());
                return order != 0 ? order : Long.compare(a.getTimestamp(), b.getTimestamp());
            default: return 0;
        }
    }

    private static int mealOrder(String mealType) {
        if (mealType == null) return 4;
        switch (mealType) {
            case "Breakfast": return 0;
            case "Lunch":     return 1;
            case "Dinner":    return 2;
            case "Snack":     return 3;
            default:          return 4;
        }
    }
}