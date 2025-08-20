package com.lumi.android.bicyclemap.ui.tour;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.lumi.android.bicyclemap.*;
import com.lumi.android.bicyclemap.api.ApiClient;
import com.lumi.android.bicyclemap.api.ApiService;
import com.lumi.android.bicyclemap.api.dto.*;
import com.lumi.android.bicyclemap.ui.surrounding.SurroundingFragment;
import com.lumi.android.bicyclemap.ui.tour.TourAdapter;

import java.util.*;
import java.util.concurrent.*;
import retrofit2.Response;

public class TourFragment extends Fragment {

    private MainViewModel mainViewModel;

    private RecyclerView recyclerView;
    private TourAdapter adapter;
    private EditText searchInput;
    private MaterialButtonToggleGroup toggleGroup;

    private enum FilterCategory { ALL, FOOD, TOURISM, TRADITION }
    private FilterCategory currentFilter = FilterCategory.ALL;
    private String currentQuery = "";

    private final List<VillagesDto> source = new ArrayList<>();
    private final List<VillagesDto> shown  = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tour, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        recyclerView = v.findViewById(R.id.tour_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TourAdapter(requireContext(), (item, pos) -> {
            // TODO: 상세 화면 진입(이미 상세 필드가 채워져 있음: tags/content 등 사용 가능)
        });
        recyclerView.setAdapter(adapter);

        searchInput = v.findViewById(R.id.tour_search_input);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s == null ? "" : s.toString().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        toggleGroup = v.findViewById(R.id.tour_category_toggle_group);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_tour_filter_food) {
                    currentFilter = FilterCategory.FOOD;
                } else if (checkedId == R.id.btn_tour_filter_tourism) {
                    currentFilter = FilterCategory.TOURISM;
                } else if (checkedId == R.id.btn_tour_filter_tradition) {
                    currentFilter = FilterCategory.TRADITION;
                } else {
                    currentFilter = FilterCategory.ALL;
                }
                applyFilters();
            } else {
                if (group.getCheckedButtonId() == View.NO_ID) {
                    currentFilter = FilterCategory.ALL;
                    applyFilters();
                }
            }
        });

        // Observe MainViewModel
        mainViewModel.getVillages().observe(getViewLifecycleOwner(), list -> {
            source.clear();
            if (list != null) source.addAll(list);
            applyFilters();
        });
        mainViewModel.getVillagesLoading().observe(getViewLifecycleOwner(), loading -> {
            // TODO: ProgressBar 표시/숨김
        });
        mainViewModel.getVillagesError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty()) {
                // TODO: Toast 로 안내
            }
        });
    }

    /** 탭에 들어올 때 데이터 없으면 목록+상세를 모두 병렬 로딩하여 MainViewModel에 저장 */
    @Override
    public void onResume() {
        super.onResume();
        List<VillagesDto> cached = mainViewModel.getVillages().getValue();
        if (cached == null || cached.isEmpty()) {
            loadVillagesWithDetailsIntoMainVM();
        }
    }

    private void loadVillagesWithDetailsIntoMainVM() {
        mainViewModel.setVillagesLoading(true);
        mainViewModel.setVillagesError(null);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<VillagesDto> result = new ArrayList<>();
            try {
                ApiService api = ApiClient.getInstance(requireContext()).getApiService();

                // 1) 4.7 목록
                Response<VillagesListResponse> listResp = api.getVillages().execute();
                if (!(listResp.isSuccessful()
                        && listResp.body() != null
                        && listResp.body().isSuccess()
                        && listResp.body().getData() != null
                        && listResp.body().getData().getSpecialties() != null)) {
                    mainViewModel.setVillagesLoading(false);
                    mainViewModel.setVillagesError("마을 특화상품 목록 조회 실패");
                    return;
                }
                List<VillagesDto> base = listResp.body().getData().getSpecialties();
                if (base.isEmpty()) {
                    mainViewModel.setVillages(new ArrayList<>());
                    mainViewModel.setVillagesLoading(false);
                    return;
                }

                // 2) 4.8 상세 병렬 호출 (쓰레드풀)
                int workers = Math.min(1, Math.max(2, Runtime.getRuntime().availableProcessors()));
                ExecutorService pool = Executors.newFixedThreadPool(workers);
                List<Callable<VillagesDto>> tasks = new ArrayList<>();

                for (VillagesDto item : base) {
                    tasks.add(() -> {
                        try {
                            Response<ApiResponse<VillagesDto>> det =
                                    api.getVillagesDetail(
                                            item.getVillageId(),
                                            item.getType(),      // "food" / "tourism" / "tradition"
                                            item.getId()
                                    ).execute();

                            if (det.isSuccessful()
                                    && det.body() != null
                                    && det.body().isSuccess()
                                    && det.body().getData() != null) {

                                VillagesDto d = det.body().getData();
                                // 목록 객체에 상세 필드 병합
                                item.setVillageAddr(d.getVillageAddr());
                                item.setTags(d.getTags());
                                item.setContent(d.getContent());
                                if (d.getRecommended() != null) item.setRecommended(d.getRecommended());
                                if (d.getImageUrl() != null && !d.getImageUrl().isEmpty())
                                    item.setImageUrl(d.getImageUrl());
                            }
                        } catch (Exception ignore) {}
                        return item; // 상세 실패해도 목록 그대로 반환
                    });
                }

                List<Future<VillagesDto>> futures = pool.invokeAll(tasks);
                for (Future<VillagesDto> f : futures) {
                    try { result.add(f.get()); } catch (Exception ignore) {}
                }
                pool.shutdown();

                // 3) MainVM에 저장
                requireActivity().runOnUiThread(() -> {
                    mainViewModel.setVillages(result);
                    mainViewModel.setVillagesLoading(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    mainViewModel.setVillagesLoading(false);
                    mainViewModel.setVillagesError("네트워크 오류: " + e.getMessage());
                });
            }
        });
    }

    private void applyFilters() {
        shown.clear();
        for (VillagesDto v : source) {
            // 타입 필터
            if (currentFilter != FilterCategory.ALL) {
                String t = v.getType() == null ? "" : v.getType();
                if (currentFilter == FilterCategory.FOOD && !t.equalsIgnoreCase("food")) continue;
                if (currentFilter == FilterCategory.TOURISM && !t.equalsIgnoreCase("tourism")) continue;
                if (currentFilter == FilterCategory.TRADITION && !t.equalsIgnoreCase("tradition")) continue;
            }
            // 검색 (이름/마을명/태그)
            if (!currentQuery.isEmpty()) {
                String q = currentQuery.toLowerCase(Locale.ROOT);
                String name = safe(v.getName());
                String villageName = safe(v.getVillageName());
                String tags = v.getTags() != null ? v.getTags().toString().toLowerCase(Locale.ROOT) : "";
                boolean hit = name.contains(q) || villageName.contains(q) || tags.contains(q);
                if (!hit) continue;
            }
            shown.add(v);
        }
        adapter.setItems(shown);
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
}
