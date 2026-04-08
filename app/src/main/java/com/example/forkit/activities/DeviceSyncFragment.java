package com.example.forkit.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.forkit.R;
import com.example.forkit.utils.HealthConnectBridge;
import com.example.forkit.utils.PrefsHelper;

import java.util.Set;


public class DeviceSyncFragment extends Fragment {

    private TextView tvStatus;
    private PrefsHelper prefs;

    private final ActivityResultLauncher<Set<String>> permissionLauncher =
            registerForActivityResult(
                    androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
                    granted -> syncNow()
            );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_sync, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new PrefsHelper(requireContext());
        tvStatus = view.findViewById(R.id.tv_sync_status);

        view.findViewById(R.id.btn_sync_back).setOnClickListener(v -> {
            if (getActivity() instanceof com.example.forkit.MainActivity) {
                ((com.example.forkit.MainActivity) getActivity()).loadFragment(new SettingsFragment());
                ((com.example.forkit.MainActivity) getActivity()).selectNavItem(R.id.nav_profile);
            }
        });

        view.findViewById(R.id.btn_request_permissions).setOnClickListener(v -> requestPermissions());
        view.findViewById(R.id.btn_sync_now).setOnClickListener(v -> syncNow());

        refreshStatus();
    }

    private void refreshStatus() {
        if (tvStatus == null || prefs == null) return;
        tvStatus.setText(
                "Steps today: " + prefs.getStepsToday() + "\n" +
                "Exercise today: " + prefs.getExerciseMins() + " min\n" +
                "Active calories today: " + prefs.getCaloriesBurned() + " kcal"
        );
    }

    private void requestPermissions() {
        if (!isHealthConnectAvailable()) return;
        permissionLauncher.launch(HealthConnectBridge.permissions());
    }

    private void syncNow() {
        Context ctx = getContext();
        if (ctx == null) return;
        if (!isHealthConnectAvailable()) return;
        if (tvStatus != null) tvStatus.setText("Syncing from Health Connect...");

        HealthConnectBridge.readTodayMetrics(ctx, new HealthConnectBridge.Callback() {
            @Override
            public void onSuccess(HealthConnectBridge.Metrics m) {
                if (!isAdded() || prefs == null) return;
                prefs.setStepsToday(m.getStepsToday());
                prefs.setExerciseMins(m.getExerciseMinutesToday());
                prefs.setCaloriesBurned(m.getActiveCaloriesToday());
                Toast.makeText(requireContext(), "Synced from Health Connect", Toast.LENGTH_SHORT).show();
                refreshStatus();
                // Refresh Home if currently visible
                if (getActivity() instanceof com.example.forkit.MainActivity) {
                    Fragment cur = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (cur instanceof HomeFragment) ((HomeFragment) cur).updateDashboard();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                if (tvStatus != null) tvStatus.setText("Sync failed: " + message + "\n\nTip: Install Health Connect and ensure your device app syncs into it.");
            }
        });
    }

    private boolean isHealthConnectAvailable() {
        try {
            int status = androidx.health.connect.client.HealthConnectClient.getSdkStatus(requireContext());
            if (status == androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE) {
                if (tvStatus != null) tvStatus.setText("Health Connect is not available on this device.");
                return false;
            }
            if (status == androidx.health.connect.client.HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                if (tvStatus != null) {
                    tvStatus.setText("Health Connect needs an update.\n\nInstall/update Health Connect, then try again.");
                }
                openHealthConnectStore();
                return false;
            }
            return true;
        } catch (Exception e) {
            if (tvStatus != null) tvStatus.setText("Health Connect error: " + e.getMessage());
            return false;
        }
    }

    private void openHealthConnectStore() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
            try {
                Intent i2 = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"));
                startActivity(i2);
            } catch (Exception ignored2) {}
        }
    }
}

