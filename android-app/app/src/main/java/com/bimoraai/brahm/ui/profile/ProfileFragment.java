package com.bimoraai.brahm.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.bimoraai.brahm.databinding.FragmentProfileBinding;
import com.bimoraai.brahm.ui.auth.LoginActivity;
import com.bimoraai.brahm.utils.DateUtils;
import com.bimoraai.brahm.utils.PrefsHelper;

/**
 * User Profile screen.
 *
 * Displays:
 *   - Name, phone, date of birth, time of birth, birth place.
 *   - Current plan chip (Free / Jyotishi / Acharya).
 *   - Upgrade card click → SubscriptionActivity (stub: Toast "Coming soon").
 *   - Edit birth details link → shows informational dialog about re-onboarding.
 *   - Logout button → confirm dialog → clear prefs → LoginActivity.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding b;
    private PrefsHelper prefs;

    // ── Fragment lifecycle ────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new PrefsHelper(requireContext());
        loadProfile();
        setupClickListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh in case the user just updated birth details via onboarding
        if (prefs != null) loadProfile();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    // ── Profile loading ───────────────────────────────────────────────────────

    /**
     * Reads all user data from PrefsHelper and populates the views.
     * Shows "Not set" for empty values so the UI never looks broken.
     */
    private void loadProfile() {
        if (b == null || prefs == null) return;

        // Name
        String name = prefs.getName();
        b.tvName.setText(name.isEmpty() ? "Not set" : name);

        // Phone
        String phone = prefs.getPhone();
        b.tvPhone.setText(phone.isEmpty() ? "Not set" : phone);

        // Date of birth — format for display
        String dob = prefs.getBirthDate();
        if (!dob.isEmpty()) {
            b.tvDob.setText(DateUtils.formatDisplayDate(dob));
        } else {
            b.tvDob.setText("Not set");
        }

        // Time of birth
        String tob = prefs.getBirthTime();
        b.tvTob.setText(tob.isEmpty() ? "Not set" : tob);

        // Birth place
        String place = prefs.getBirthPlace();
        b.tvBirthPlace.setText(place.isEmpty() ? "Not set" : place);

        // Plan chip — capitalise first letter for display
        String plan = prefs.getPlan();   // "free", "jyotishi", "acharya"
        String planDisplay = plan.isEmpty() ? "Free"
            : plan.substring(0, 1).toUpperCase() + plan.substring(1);
        b.chipPlan.setText(planDisplay);

        // Tint the chip based on plan level
        switch (plan.toLowerCase()) {
            case "jyotishi":
                b.chipPlan.setChipBackgroundColorResource(
                    com.google.android.material.R.color.material_dynamic_primary40);
                break;
            case "acharya":
                b.chipPlan.setChipBackgroundColorResource(
                    com.google.android.material.R.color.material_dynamic_tertiary40);
                break;
            default: // free — default chip colour
                break;
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        if (b == null) return;

        // Upgrade card
        b.cardUpgrade.setOnClickListener(v -> {
            // TODO: Start SubscriptionActivity when implemented
            // startActivity(new Intent(requireContext(), SubscriptionActivity.class));
            Toast.makeText(requireContext(), "Coming soon", Toast.LENGTH_SHORT).show();
        });

        // Edit birth details (tap birth place row)
        b.rowEditBirth.setOnClickListener(v -> showEditBirthDialog());

        // Logout
        b.btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    /**
     * Informs the user that birth details can be changed by going through the
     * onboarding flow again (re-login or a dedicated edit screen in future).
     */
    private void showEditBirthDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Edit Birth Details")
            .setMessage("To update your birth details, please log out and go through the " +
                        "onboarding flow again. A dedicated edit screen will be available " +
                        "in an upcoming update.")
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Asks for confirmation before clearing all data and returning to LoginActivity.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out? Your birth details and local " +
                        "data will be cleared from this device.")
            .setPositiveButton("Log out", (dialog, which) -> performLogout())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void performLogout() {
        if (prefs != null) prefs.clear();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) getActivity().finish();
    }
}
