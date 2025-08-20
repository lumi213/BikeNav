package com.lumi.android.bicyclemap.ui.course;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.lumi.android.bicyclemap.MainActivity;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.dto.CourseDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CourseFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private MainViewModel viewModel;
    private CourseAdapter adapter;
    private List<CourseDto> allCourses = new ArrayList<>();
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

        // 🔎 키워드 변경 시 필터
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterCourses(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 🏷️ 다중 칩 선택 대응
        tagGroup.setOnCheckedStateChangeListener((group, checkedIds) -> filterCourses());

        // 데이터 observe
        viewModel.getAllRoutes().observe(getViewLifecycleOwner(), routes -> {
            allCourses = (routes != null) ? routes : new ArrayList<>();
            filterCourses();
        });

        return view;
    }

    private void filterCourses() {
        // 1) 키워드
        final String keyword = safeLower(searchInput.getText() != null ? searchInput.getText().toString() : "");

        // 2) 선택된 칩들 텍스트 수집 (다중 선택)
        final List<Integer> checkedIds = tagGroup.getCheckedChipIds();
        final Set<String> selectedTags = new HashSet<>();
        if (checkedIds != null) {
            for (int id : checkedIds) {
                Chip chip = tagGroup.findViewById(id);
                if (chip != null && chip.isChecked()) {
                    selectedTags.add(chip.getText().toString());
                }
            }
        }

        // 3) 필터링
        List<CourseDto> filtered = new ArrayList<>();
        for (CourseDto r : allCourses) {
            final String title = safeLower(r.getTitle());

            // 3-1) 키워드 매칭: 제목 또는 카테고리(태그) 문자열에 포함
            boolean matchesKeyword = keyword.isEmpty();
            if (!matchesKeyword) {
                if (title.contains(keyword)) {
                    matchesKeyword = true;
                } else if (r.getTags() != null && !r.getTags().isEmpty()) {
                    for (String c : r.getTags()) {
                        if (safeLower(c).contains(keyword)) { matchesKeyword = true; break; }
                    }
                }
            }
            if (!matchesKeyword) continue;

            // 3-2) 태그 매칭
            // OR 조건: 선택된 칩 중 하나라도 route.category에 포함되면 통과
            boolean matchesTags = selectedTags.isEmpty();
            if (!matchesTags) {
                if (r.getTags() != null && !r.getTags().isEmpty()) {
                    for (String sel : selectedTags) {
                        for (String c : r.getTags()) {
                            if (containsIgnoreCase(c, sel)) { matchesTags = true; break; }
                        }
                        if (matchesTags) break;
                    }
                } else {
                    matchesTags = false;
                }
            }
            if (!matchesTags) continue;

            filtered.add(r);
        }

        // 4) 적용
        adapter.submitList(filtered);
    }

    private static String safeLower(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsIgnoreCase(String src, String needle) {
        if (src == null || needle == null) return false;
        return src.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    @Override
    public void onCourseClick(CourseDto route) {
        CourseDetailFragment detailFragment = new CourseDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("CourseDto", route);
        detailFragment.setArguments(bundle);
        ((MainActivity) requireActivity()).replaceFragment(detailFragment, true);
    }
}
