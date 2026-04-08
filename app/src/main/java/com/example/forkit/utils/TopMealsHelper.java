package com.example.forkit.utils;

import com.example.forkit.models.FoodEntry;
import java.util.ArrayList;
import java.util.List;

public class TopMealsHelper {

    // simple max heap using array
    private List<FoodEntry> heap = new ArrayList<>();

    private void swap(int i, int j) {
        FoodEntry temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (heap.get(i).getCalories() > heap.get(parent).getCalories()) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }

    private void siftDown(int i, int size) {
        while (2 * i + 1 < size) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int largest = i;

            if (left < size && heap.get(left).getCalories() > heap.get(largest).getCalories())
                largest = left;
            if (right < size && heap.get(right).getCalories() > heap.get(largest).getCalories())
                largest = right;

            if (largest != i) {
                swap(i, largest);
                i = largest;
            } else {
                break;
            }
        }
    }

    public void insert(FoodEntry entry) {
        heap.add(entry);
        siftUp(heap.size() - 1);
    }

    // removes and returns the highest calorie entry
    public FoodEntry extractMax() {
        if (heap.isEmpty()) return null;
        FoodEntry max = heap.get(0);
        heap.set(0, heap.get(heap.size() - 1));
        heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) siftDown(0, heap.size());
        return max;
    }

    // get top k highest calorie meals
    public static List<FoodEntry> getTopMeals(List<FoodEntry> entries, int k) {
        if (entries == null || entries.isEmpty()) return new ArrayList<>();

        TopMealsHelper maxHeap = new TopMealsHelper();
        for (FoodEntry e : entries) {
            maxHeap.insert(e);
        }

        List<FoodEntry> top = new ArrayList<>();
        for (int i = 0; i < k && !maxHeap.heap.isEmpty(); i++) {
            top.add(maxHeap.extractMax());
        }
        return top;
    }
}