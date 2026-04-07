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
                // MAX/MIN HEAP for calories:
                // - peek: O(1)
                // - insert: O(log n)
                // - extract: O(log n)
                // - full sort via repeated extract: O(n log n)
                list = heapSortByCalories(list, sortBy);
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

    private static List<FoodEntry> heapSortByCalories(List<FoodEntry> entries, SortBy sortBy) {
        if (entries == null || entries.size() <= 1) return entries;
        if (sortBy == SortBy.CALORIES_HIGH) {
            MaxHeap heap = MaxHeap.build(entries);
            List<FoodEntry> out = new ArrayList<>(entries.size());
            while (!heap.isEmpty()) out.add(heap.extractMax());
            return out;
        } else {
            MinHeap heap = MinHeap.build(entries);
            List<FoodEntry> out = new ArrayList<>(entries.size());
            while (!heap.isEmpty()) out.add(heap.extractMin());
            return out;
        }
    }

    private static final class MaxHeap {
        private final ArrayList<FoodEntry> a;

        private MaxHeap(int capacity) { this.a = new ArrayList<>(capacity); }

        static MaxHeap build(List<FoodEntry> items) {
            MaxHeap h = new MaxHeap(items.size());
            h.a.addAll(items);
            // heapify O(n)
            for (int i = (h.a.size() / 2) - 1; i >= 0; i--) h.siftDown(i);
            return h;
        }

        boolean isEmpty() { return a.isEmpty(); }

        FoodEntry peek() { return a.isEmpty() ? null : a.get(0); } // O(1)

        void insert(FoodEntry e) { // O(log n)
            a.add(e);
            siftUp(a.size() - 1);
        }

        FoodEntry extractMax() { // O(log n)
            if (a.isEmpty()) return null;
            FoodEntry max = a.get(0);
            FoodEntry last = a.remove(a.size() - 1);
            if (!a.isEmpty()) {
                a.set(0, last);
                siftDown(0);
            }
            return max;
        }

        private void siftUp(int i) {
            while (i > 0) {
                int p = (i - 1) / 2;
                if (compareCaloriesDesc(a.get(i), a.get(p)) <= 0) break;
                swap(i, p);
                i = p;
            }
        }

        private void siftDown(int i) {
            int n = a.size();
            while (true) {
                int l = i * 2 + 1;
                int r = l + 1;
                int best = i;
                if (l < n && compareCaloriesDesc(a.get(l), a.get(best)) > 0) best = l;
                if (r < n && compareCaloriesDesc(a.get(r), a.get(best)) > 0) best = r;
                if (best == i) return;
                swap(i, best);
                i = best;
            }
        }

        private void swap(int i, int j) {
            FoodEntry t = a.get(i);
            a.set(i, a.get(j));
            a.set(j, t);
        }
    }

    private static final class MinHeap {
        private final ArrayList<FoodEntry> a;

        private MinHeap(int capacity) { this.a = new ArrayList<>(capacity); }

        static MinHeap build(List<FoodEntry> items) {
            MinHeap h = new MinHeap(items.size());
            h.a.addAll(items);
            // heapify O(n)
            for (int i = (h.a.size() / 2) - 1; i >= 0; i--) h.siftDown(i);
            return h;
        }

        boolean isEmpty() { return a.isEmpty(); }

        FoodEntry peek() { return a.isEmpty() ? null : a.get(0); } // O(1)

        void insert(FoodEntry e) { // O(log n)
            a.add(e);
            siftUp(a.size() - 1);
        }

        FoodEntry extractMin() { // O(log n)
            if (a.isEmpty()) return null;
            FoodEntry min = a.get(0);
            FoodEntry last = a.remove(a.size() - 1);
            if (!a.isEmpty()) {
                a.set(0, last);
                siftDown(0);
            }
            return min;
        }

        private void siftUp(int i) {
            while (i > 0) {
                int p = (i - 1) / 2;
                if (compareCaloriesAsc(a.get(i), a.get(p)) >= 0) break;
                swap(i, p);
                i = p;
            }
        }

        private void siftDown(int i) {
            int n = a.size();
            while (true) {
                int l = i * 2 + 1;
                int r = l + 1;
                int best = i;
                if (l < n && compareCaloriesAsc(a.get(l), a.get(best)) < 0) best = l;
                if (r < n && compareCaloriesAsc(a.get(r), a.get(best)) < 0) best = r;
                if (best == i) return;
                swap(i, best);
                i = best;
            }
        }

        private void swap(int i, int j) {
            FoodEntry t = a.get(i);
            a.set(i, a.get(j));
            a.set(j, t);
        }
    }

    /**
     * Descending calories comparator used by max-heap.
     * Ties broken by timestamp (newer first) for stable-ish behavior.
     */
    private static int compareCaloriesDesc(FoodEntry a, FoodEntry b) {
        int c = Integer.compare(a.getCalories(), b.getCalories());
        if (c != 0) return c;
        return Long.compare(a.getTimestamp(), b.getTimestamp());
    }

    /**
     * Ascending calories comparator used by min-heap.
     * Ties broken by timestamp (older first).
     */
    private static int compareCaloriesAsc(FoodEntry a, FoodEntry b) {
        int c = Integer.compare(a.getCalories(), b.getCalories());
        if (c != 0) return c;
        return Long.compare(a.getTimestamp(), b.getTimestamp());
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
        if (mealType == null) return 5;
        switch (mealType) {
            case "Breakfast": return 0;
            case "Lunch":     return 1;
            case "Dinner":    return 2;
            case "Tea":       return 3;
            case "Snack":     return 4;
            default:          return 5;
        }
    }
}