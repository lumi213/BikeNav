package com.lumi.android.bicyclemap.ui.course;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.lumi.android.bicyclemap.MainActivity;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.Point;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.dto.CourseDto;
import com.lumi.android.bicyclemap.api.dto.PoiDto;
import com.lumi.android.bicyclemap.databinding.FragmentCourseDetailBinding;

import java.util.Map;

public class CourseDetailFragment extends Fragment implements OnMapReadyCallback {

    /* ───────── 필드 ───────── */
    public static final String ARG_ROUTE = "CourseDto";

    private FragmentCourseDetailBinding binding;  // ✅ 뷰바인딩
    private CourseDto route;
    private GoogleMap map;
    private MainViewModel viewModel;

    /* ───────── 팩토리 ───────── */
    public static CourseDetailFragment newInstance(CourseDto route) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_ROUTE, route);
        CourseDetailFragment f = new CourseDetailFragment();
        f.setArguments(args);
        return f;
    }

    /* ───────── onCreateView ───────── */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCourseDetailBinding.inflate(inflater, container, false);
        View v = binding.getRoot();

        // Route 획득
        if (getArguments() != null) {
            route = (CourseDto) getArguments().getSerializable(ARG_ROUTE);
        }

        // ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // 네비게이션 바 노출 (Activity 뷰에 직접 접근하므로 try/catch)
        View nav = requireActivity().findViewById(R.id.nav_view);
        if (nav != null) nav.setVisibility(View.VISIBLE);

        bindViews();

        return v;
    }

    /* ───────── onViewCreated ───────── */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 지도 콜백 등록은 반드시 뷰 생성 이후에
        SupportMapFragment mapFrag =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.detail_map);
        if (mapFrag != null) {
            mapFrag.getMapAsync(this);
        }
    }

    /* ───────── GoogleMap 콜백 ───────── */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // 뷰/수명주기 가드
        if (!isAdded() || binding == null ||
                !getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            return;
        }
        map = googleMap;
        drawRouteOnMap();
    }

    /* ───────── 바인딩 로직 ───────── */
    private void bindViews() {
        ImageView image       = binding.detailImage;
        TextView  title       = binding.detailTitle;
        TextView  info        = binding.detailInfo;
        TextView  explanation = binding.detailExplanation;
        LinearLayout tagsLay  = binding.detailTags;
        TextView  tourPoints  = binding.detailTourPoints;
        TextView  surrounding = binding.surroundingPoints;
        Button    btnNavigate = binding.btnNavigate;

        if (route != null) {
            // ── 이미지 로딩 ──
            final int PLACEHOLDER = R.drawable.loading;       // 로딩 중
            final int NO_URL_IMG  = R.drawable.noimg;         // URL 없음

            int localResId = resolveCourseImage(image.getContext(), route, NO_URL_IMG);

            if (route.getImage() != null && !route.getImage().trim().isEmpty()) {
                String src = route.getImage().trim();
                Object glideSrc = src.startsWith("data:image") ? Uri.parse(src) : src;

                Glide.with(this)
                        .load(glideSrc)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .placeholder(PLACEHOLDER)
                        .error(localResId)   // 실패 시 로컬 대체
                        .centerCrop()
                        .into(image);
            } else {
                image.setImageResource(localResId);
            }

            // ── 텍스트 ──
            title.setText(route.getTitle());
            String spot = (route.getTourist_spots() != null && !route.getTourist_spots().isEmpty())
                    ? route.getTourist_spots().get(0) : "";
            info.setText("코스 " + route.getDist_km() + "Km · " + route.getTime() + "분 · " + spot);
            explanation.setText(route.getDescription() != null ? route.getDescription() : "");

            // ── 태그 ──
            tagsLay.removeAllViews();
            if (route.getTags() != null) {
                for (String tag : route.getTags()) {
                    TextView chip = new TextView(requireContext());
                    chip.setText("#" + tag);
                    chip.setTextColor(0xFF555555);
                    chip.setTextSize(12);
                    chip.setPadding(0, 0, dp(24), 0);
                    tagsLay.addView(chip);
                }
            }

            // ── 관광 포인트 ──
            if (route.getTourist_spots() != null) {
                StringBuilder sb = new StringBuilder();
                for (String p : route.getTourist_spots()) sb.append("• ").append(p).append('\n');
                tourPoints.setText(sb.toString());
            }

            // ── 주변 POI 텍스트 ──
            if (route.getNearby_businesses() != null) {
                StringBuilder sb = new StringBuilder();
                for (String p : route.getNearby_businesses()) sb.append("• ").append(p).append('\n');
                surrounding.setText(sb.toString());
            }
        }

        // ── 버튼: 코스 시작 ──
        btnNavigate.setOnClickListener(v1 -> {
            viewModel.setSelectedRoute(route);
            viewModel.setMapState(MainViewModel.MapState.WALKING);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setBottomNavigationSelected(R.id.navigation_maps);
            }
        });
    }

    /* ───────── 경로/POI 그리기 ───────── */
    private void drawRouteOnMap() {
        if (map == null || binding == null || route == null ||
                route.getPath() == null || route.getPath().isEmpty()) return;

        PolylineOptions poly = new PolylineOptions().width(10).color(0xFF1976D2);
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();

        for (Point p : route.getPath()) {
            LatLng ll = new LatLng(p.lat, p.lng);
            poly.add(ll);
            bounds.include(ll);
        }
        map.addPolyline(poly);

        fitBoundsSafe(map, bounds.build());

        if (route.poi != null) {
            Map<Integer, PoiDto> poiMap = viewModel.getPoiMap().getValue();
            if (poiMap != null) {
                for (int id : route.poi) {
                    PoiDto poi = poiMap.get(id);
                    if (poi != null && poi.getPoint() != null) {
                        LatLng ll = new LatLng(poi.getPoint().lat, poi.getPoint().lng);
                        map.addMarker(new MarkerOptions()
                                .position(ll)
                                .title(poi.getName())
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                    }
                }
            }
        }
    }

    /* ───────── 카메라 핏 안전 처리 ───────── */
    private void fitBoundsSafe(@NonNull GoogleMap map, @NonNull LatLngBounds bounds) {
        if (binding == null) {
            map.setOnMapLoadedCallback(() -> {
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, dp(24)));
                } catch (Exception ignore) {}
            });
            return;
        }

        // layout XML의 id="@+id/detail_map" (SupportMapFragment/FragmentContainerView)
        final View containerView = requireView().findViewById(R.id.detail_map); // ViewBinding가 생성한 필드명

        final int padding = dp(24);

        if (containerView == null) {
            map.setOnMapLoadedCallback(() -> {
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                } catch (Exception ignore) {}
            });
            return;
        }

        if (containerView.getWidth() == 0 || containerView.getHeight() == 0) {
            containerView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override public void onGlobalLayout() {
                            containerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            try {
                                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(
                                        bounds, containerView.getWidth(), containerView.getHeight(), padding);
                                map.animateCamera(cu);
                            } catch (Exception ignore) {}
                        }
                    }
            );
        } else {
            try {
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(
                        bounds, containerView.getWidth(), containerView.getHeight(), padding);
                map.animateCamera(cu);
            } catch (Exception ignore) {}
        }
    }

    /** 코스ID로 drawable 리소스가 있으면 반환, 없으면 categoryFallback 반환 */
    private int resolveCourseImage(Context ctx, CourseDto route, int categoryFallback) {
        // 예) res/drawable/l201.png 형태로 배치되어 있다고 가정 (필요시 이름 규칙 수정)
        Integer id = route.getCourse_id(); // 프로젝트에 맞게 getter 확인
        if (id != null) {
            String name = "l" + id;
            int resId = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
            if (resId != 0) return resId;
        }
        return categoryFallback;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        map = null;
        binding = null;
    }
}
