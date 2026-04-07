package com.example.forkit.utils;

import com.example.forkit.models.FoodEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * Central in-memory store for logged foods + heap-backed analytics.
 *
 * Goals:
 * - Insert: O(log n) update to top-meal heap + daily totals + peak-day heap
 * - Peek: O(1) for top meal and peak day
 *
 * Notes:
 * - We rebuild on full cloud refresh or deletes (O(n)) which keeps code simple and reliable.
 */
public final class FoodStore {

    private FoodStore() {}

    private static final Object LOCK = new Object();

    private static final ArrayList<FoodEntry> entries = new ArrayList<>();

    // Meal heap: max calories (ties -> newer)
    private static MaxMealHeap mealHeap = new MaxMealHeap(0);

    // Peak day heap for last 7 days window (0=Mon..6=Sun)
    private static int[] last7DowTotals = new int[7];
    private static MaxDayHeap dayHeap = MaxDayHeap.build(last7DowTotals);
    private static long lastWindowTodayStartMs = 0L;

    public static List<FoodEntry> getEntriesView() {
        synchronized (LOCK) {
            return Collections.unmodifiableList(new ArrayList<>(entries));
        }
    }

    public static void setAll(List<FoodEntry> newEntries) {
        synchronized (LOCK) {
            entries.clear();
            if (newEntries != null) entries.addAll(newEntries);
            rebuildIndexesLocked();
        }
    }

    public static void add(FoodEntry e) {
        if (e == null) return;
        synchronized (LOCK) {
            entries.add(e);
            ensureWindowUpToDateLocked();
            mealHeap.insert(e); // O(log n)
            if (isInLast7DaysWindowLocked(e.getTimestamp())) {
                int idx = dowMon0(e.getTimestamp());
                last7DowTotals[idx] += e.getCalories();
                dayHeap = MaxDayHeap.build(last7DowTotals); // 7 items -> tiny constant; keeps correctness
            }
        }
    }

    public static void remove(FoodEntry e) {
        if (e == null) return;
        synchronized (LOCK) {
            entries.remove(e);
            // Removal from heap efficiently needs an index map; we rebuild to keep logic simple.
            rebuildIndexesLocked();
        }
    }

    public static FoodEntry peekTopMeal() {
        synchronized (LOCK) {
            ensureWindowUpToDateLocked();
            return mealHeap.peek(); // O(1)
        }
    }

    public static FoodAnalytics.DayTotal peekPeakDay() {
        synchronized (LOCK) {
            ensureWindowUpToDateLocked();
            return dayHeap.peek(); // O(1)
        }
    }

    private static void rebuildIndexesLocked() {
        ensureWindowUpToDateLocked();
        mealHeap = MaxMealHeap.build(entries);
        last7DowTotals = totalsLast7DaysByDowLocked(entries);
        dayHeap = MaxDayHeap.build(last7DowTotals);
    }

    private static void ensureWindowUpToDateLocked() {
        long todayStart = startOfTodaySingapore();
        if (todayStart != lastWindowTodayStartMs) {
            lastWindowTodayStartMs = todayStart;
            last7DowTotals = totalsLast7DaysByDowLocked(entries);
            dayHeap = MaxDayHeap.build(last7DowTotals);
        }
    }

    private static long startOfTodaySingapore() {
        TimeZone tz = TimeZone.getTimeZone("Asia/Singapore");
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static boolean isInLast7DaysWindowLocked(long ts) {
        long now = System.currentTimeMillis();
        long windowStart = lastWindowTodayStartMs - (6L * 24L * 60L * 60L * 1000L);
        return ts >= windowStart && ts <= now;
    }

    private static int[] totalsLast7DaysByDowLocked(List<FoodEntry> list) {
        int[] totals = new int[7];
        if (list == null || list.isEmpty()) return totals;
        long now = System.currentTimeMillis();
        long windowStart = lastWindowTodayStartMs - (6L * 24L * 60L * 60L * 1000L);
        for (FoodEntry e : list) {
            if (e == null) continue;
            long ts = e.getTimestamp();
            if (ts < windowStart || ts > now) continue;
            totals[dowMon0(ts)] += e.getCalories();
        }
        return totals;
    }

    private static int dowMon0(long ts) {
        TimeZone tz = TimeZone.getTimeZone("Asia/Singapore");
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(ts);
        int dow = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun,2=Mon..7=Sat
        return (dow + 5) % 7;
    }

    // ---- Heaps ----

    private static final class MaxMealHeap {
        private final ArrayList<FoodEntry> a;

        private MaxMealHeap(int capacity) { this.a = new ArrayList<>(capacity); }

        static MaxMealHeap build(List<FoodEntry> items) {
            MaxMealHeap h = new MaxMealHeap(items != null ? items.size() : 0);
            if (items != null) h.a.addAll(items);
            for (int i = (h.a.size() / 2) - 1; i >= 0; i--) h.siftDown(i);
            return h;
        }

        FoodEntry peek() { return a.isEmpty() ? null : a.get(0); } // O(1)

        void insert(FoodEntry e) { // O(log n)
            a.add(e);
            siftUp(a.size() - 1);
        }

        private void siftUp(int i) {
            while (i > 0) {
                int p = (i - 1) / 2;
                if (compare(a.get(i), a.get(p)) <= 0) break;
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
                if (l < n && compare(a.get(l), a.get(best)) > 0) best = l;
                if (r < n && compare(a.get(r), a.get(best)) > 0) best = r;
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

        private static int compare(FoodEntry x, FoodEntry y) {
            if (x == null && y == null) return 0;
            if (x == null) return -1;
            if (y == null) return 1;
            int c = Integer.compare(x.getCalories(), y.getCalories());
            if (c != 0) return c;
            return Long.compare(x.getTimestamp(), y.getTimestamp());
        }
    }

    private static final class MaxDayHeap {
        private final ArrayList<FoodAnalytics.DayTotal> a;

        private MaxDayHeap(int capacity) { this.a = new ArrayList<>(capacity); }

        static MaxDayHeap build(int[] totalsMon0) {
            MaxDayHeap h = new MaxDayHeap(7);
            for (int i = 0; i < 7; i++) {
                int cals = totalsMon0 != null ? totalsMon0[i] : 0;
                h.a.add(new FoodAnalytics.DayTotal(i, cals));
            }
            for (int i = (h.a.size() / 2) - 1; i >= 0; i--) h.siftDown(i);
            return h;
        }

        FoodAnalytics.DayTotal peek() { return a.isEmpty() ? null : a.get(0); } // O(1)

        private void siftDown(int i) {
            int n = a.size();
            while (true) {
                int l = i * 2 + 1;
                int r = l + 1;
                int best = i;
                if (l < n && compare(a.get(l), a.get(best)) > 0) best = l;
                if (r < n && compare(a.get(r), a.get(best)) > 0) best = r;
                if (best == i) return;
                swap(i, best);
                i = best;
            }
        }

        private void swap(int i, int j) {
            FoodAnalytics.DayTotal t = a.get(i);
            a.set(i, a.get(j));
            a.set(j, t);
        }

        private static int compare(FoodAnalytics.DayTotal x, FoodAnalytics.DayTotal y) {
            int c = Integer.compare(x.calories, y.calories);
            if (c != 0) return c;
            // Prefer earlier day when tied
            return Integer.compare(y.dayIndexMon0, x.dayIndexMon0);
        }
    }
}

