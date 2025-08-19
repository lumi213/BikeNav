package com.lumi.android.bicyclemap.ui.tour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.POIAdapter;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.ui.surrounding.SurroundingFragment;

public class TourFragment extends Fragment {
    private TourViewModel viewModel;
    private MainViewModel mainViewModel;
    private RecyclerView recyclerView;
    private POIAdapter adapter;

    private enum FilterCategory {
        ALL, BIZ, UTIL, TOURIST
    }
    private FilterCategory currentFilter = FilterCategory.ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tour, container, false);
    }
}
