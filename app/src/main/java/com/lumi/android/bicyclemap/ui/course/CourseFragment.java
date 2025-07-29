package com.lumi.android.bicyclemap.ui.course;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.lumi.android.bicyclemap.MainActivity;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourseFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private MainViewModel viewModel;
    private CourseAdapter adapter;
    private List<Route> allCourses = new ArrayList<>();
    private EditText searchInput;
    private ChipGroup tagGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.course_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CourseAdapter(viewModel);
        adapter.setOnCourseClickListener(this);
        recyclerView.setAdapter(adapter);

        searchInput = view.findViewById(R.id.search_input);
        tagGroup = view.findViewById(R.id.tag_chip_group);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCourses();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        tagGroup.setOnCheckedChangeListener((group, checkedId) -> filterCourses());

        viewModel.getAllRoutes().observe(getViewLifecycleOwner(), routes -> {
            allCourses = routes;
            filterCourses();
        });

        return view;
    }

    private void filterCourses() {
        String keyword = searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        int checkedId = tagGroup.getCheckedChipId();
        String tag = null;
        if (checkedId != View.NO_ID) {
            Chip chip = tagGroup.findViewById(checkedId);
            if (chip != null) tag = chip.getText().toString();
        }

        List<Route> filtered = new ArrayList<>();
        for (Route r : allCourses) {
            boolean matchesKeyword = r.title.toLowerCase(Locale.ROOT).contains(keyword);
            boolean matchesTag = (tag == null);
            if (tag != null && r.category != null) {
                for (String c : r.category) {
                    if (c.contains(tag)) {
                        matchesTag = true;
                        break;
                    }
                }
            }
            if (matchesKeyword && matchesTag) {
                filtered.add(r);
            }
        }

        adapter.submitList(filtered);
    }

    @Override
    public void onCourseClick(Route route) {
        CourseDetailFragment detailFragment = new CourseDetailFragment();

        // route 정보를 넘기고 싶다면 Bundle로 설정
        Bundle bundle = new Bundle();
        bundle.putSerializable("route", route);
        detailFragment.setArguments(bundle);

        // MainActivity의 replaceFragment를 통해 디테일 프래그먼트로 이동
        ((MainActivity) requireActivity()).replaceFragment(detailFragment, true);
    }
}
