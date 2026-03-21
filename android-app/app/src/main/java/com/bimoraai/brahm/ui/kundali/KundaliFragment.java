package com.bimoraai.brahm.ui.kundali;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.bimoraai.brahm.R;
import com.bimoraai.brahm.databinding.FragmentKundaliBinding;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.Arrays;
import java.util.List;

public class KundaliFragment extends Fragment {

    private FragmentKundaliBinding b;

    private static final List<String> TAB_TITLES = Arrays.asList(
        "Chart", "Planets", "Dashas", "Yogas", "Alerts", "Shadbala", "D-9"
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        b = FragmentKundaliBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViewPager();
        setupActionButtons();
    }

    private void setupViewPager() {
        KundaliPagerAdapter adapter = new KundaliPagerAdapter(this);
        b.viewPager.setAdapter(adapter);
        b.viewPager.setOffscreenPageLimit(3);

        new TabLayoutMediator(b.tabLayout, b.viewPager,
            (tab, position) -> tab.setText(TAB_TITLES.get(position))
        ).attach();
    }

    private void setupActionButtons() {
        b.btnExportPdf.setOnClickListener(v -> exportPdf());
        b.btnShare.setOnClickListener(v -> shareKundali());
    }

    private void exportPdf() {
        // TODO: Generate PDF with kundali data
        android.widget.Toast.makeText(requireContext(), "Generating PDF…", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void shareKundali() {
        // TODO: Share deep link with birth params
        android.widget.Toast.makeText(requireContext(), "Generating share link…", android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    // ─── Pager adapter ───────────────────────────────────────────────────────
    private static class KundaliPagerAdapter extends FragmentStateAdapter {
        KundaliPagerAdapter(Fragment fragment) { super(fragment); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 0 -> new KundaliChartTabFragment();
                case 1 -> new KundaliPlanetsTabFragment();
                case 2 -> new KundaliDashasTabFragment();
                case 3 -> new KundaliYogasTabFragment();
                case 4 -> new KundaliAlertsTabFragment();
                case 5 -> new KundaliShadbalaTabFragment();
                case 6 -> new KundaliNavamshaTabFragment();
                default -> new KundaliChartTabFragment();
            };
        }

        @Override public int getItemCount() { return 7; }
    }
}
