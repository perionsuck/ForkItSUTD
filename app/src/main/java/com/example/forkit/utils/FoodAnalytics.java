package com.example.forkit.utils;

import com.example.forkit.models.FoodEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Analytics helpers designed around:
 * - peek: O(1)
 * - insert/extract: O(log n)
 *
 * (We build heaps from the current entries when needed.)
 */
public final class FoodAnalytics {

    private FoodAnalytics() {}

    public static final class DayTotal {
        public final int dayIndexMon0; // 0=Mon ... 6=Sun
        public final int calories;

        public DayTotal(int dayIndexMon0, int calories) {
            this.dayIndexMon0 = dayIndexMon0;
            this.calories = calories;
        }
    }

    public static DayTotal peakDayLast7Days(List<FoodEntry> entries) {
        int[] totals = totalsLast7DaysByDow(entries);
        MaxHeapDay heap = MaxHeapDay.build(totals);
        return heap.peek(); // O(1)
    }

    public static FoodEntry highestCalorieMeal(List<FoodEntry> entries) {
        if (entries == null || entries.isEmpty()) return null;
        MaxHeapMeal heap = MaxHeapMeal.build(entries);
        return heap.peek(); // O(1)
    }

    /** Returns calories totals by day-of-week for the last 7 days, keyed 0=Mon..6=Sun. */
    private static int[] totalsLast7DaysByDow(List<FoodEntry> entries) {
        int[] totals = new int[7];
        if (entries == null || entries.isEmpty()) return totals;

        // Match the app's existing "Asia/Singapore" assumption.
        TimeZone tz = TimeZone.getTimeZone("Asia/Singapore");
        Calendar cal = Calendar.getInstance(tz);
        long now = System.currentTimeMillis();
        cal.setTimeInMillis(now);
        setStartOfDay(cal);
        long todayStart = cal.getTimeInMillis();
        long windowStart = todayStart - (6L * 24L * 60L * 60L * 1000L); // inclusive

        for (FoodEntry e : entries) {
            if (e == null) continue;
            long ts = e.getTimestamp();
            if (ts < windowStart || ts > now) continue;

            cal.setTimeInMillis(ts);
            int dow = cal.get(Calendar.DAY_OF_WEEK); // 1=Sun,2=Mon..7=Sat
            int idxMon0 = (dow + 5) % 7;
            totals[idxMon0] += e.getCalories();
        }
        return totals;
    }

    private static void setStartOfDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    // ---- Heaps ----

    /** Max-heap over meals by calories; ties -> newer first. */
    private static final class MaxHeapMeal {
        private final ArrayList<FoodEntry> a;

        private MaxHeapMeal(int capacity) { this.a = new ArrayList<>(capacity); }

        static MaxHeapMeal build(List<FoodEntry> items) {
            MaxHeapMeal h = new MaxHeapMeal(items.size());
            h.a.addAll(items);
            for (int i = (h.a.size() / 2) - 1; i >= 0; i--) h.siftDown(i);
            return h;
        }

        FoodEntry peek() { return a.isEmpty() ? null : a.get(0); } // O(1)

        boolean isEmpty() { return a.isEmpty(); }

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

        void insert(FoodEntry e) { // O(log n)
            a.add(e);
            siftUp(a.size() - 1);
        }

        private void siftUp(int i) {
            while (i > 0) {
                int p = (i - 1) / 2;
                if (compareMeal(a.get(i), a.get(p)) <= 0) break;
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
                if (l < n && compareMeal(a.get(l), a.get(best)) > 0) best = l;
                if (r < n && compareMeal(a.get(r), a.get(best)) > 0) best = r;
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

        private static int compareMeal(FoodEntry x, FoodEntry y) {
            if (x == null && y == null) return 0;
            if (x == null) return -1;
            if (y == null) return 1;
            int c = Integer.compare(x.getCalories(), y.getCalories());
            if (c != 0) return c;
            return Long.compare(x.getTimestamp(), y.getTimestamp());
        }
    }

    /** Max-heap over days by calories; ties -> earlier in week (Mon..Sun). */
    private static final class MaxHeapDay {
        private final ArrayList<DayTotal> a;

        private MaxHeapDay(int capacity) { this.a = new ArrayList<>(capacity); }

        static MaxHeapDay build(int[] totalsMon0) {
            MaxHeapDay h = new MaxHeapDay(7);
            for (int i = 0; i < 7; i++) h.a.add(new DayTotal(i, totalsMon0 != null ? totalsMon0[i] : 0));
            for (int i = (h.a.size() / 2) - 1; i >= 0; i--) h.siftDown(i);
            return h;
        }

        DayTotal peek() { return a.isEmpty() ? null : a.get(0); } // O(1)

        private void siftDown(int i) {
            int n = a.size();
            while (true) {
                int l = i * 2 + 1;
                int r = l + 1;
                int best = i;
                if (l < n && compareDay(a.get(l), a.get(best)) > 0) best = l;
                if (r < n && compareDay(a.get(r), a.get(best)) > 0) best = r;
                if (best == i) return;
                swap(i, best);
                i = best;
            }
        }

        private void swap(int i, int j) {
            DayTotal t = a.get(i);
            a.set(i, a.get(j));
            a.set(j, t);
        }

        private static int compareDay(DayTotal x, DayTotal y) {
            int c = Integer.compare(x.calories, y.calories);
            if (c != 0) return c;
            // Prefer earlier day when tied
            return Integer.compare(y.dayIndexMon0, x.dayIndexMon0);
        }
    }

    public static String dayLabelMon0(int idx) {
        switch (idx) {
            case 0: return "Mon";
            case 1: return "Tue";
            case 2: return "Wed";
            case 3: return "Thu";
            case 4: return "Fri";
            case 5: return "Sat";
            case 6: return "Sun";
            default: return "—";
        }
    }
}

