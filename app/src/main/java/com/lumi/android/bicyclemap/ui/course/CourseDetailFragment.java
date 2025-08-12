package com.lumi.android.bicyclemap.ui.course;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
import com.lumi.android.bicyclemap.POI;
import com.lumi.android.bicyclemap.Point;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.Route;

import java.util.Map;

public class CourseDetailFragment extends Fragment implements OnMapReadyCallback {

    /* ───────── 필드 ───────── */
    private Route route;
    private GoogleMap map;
    private MainViewModel viewModel;

    public static final String ARG_ROUTE = "route";

    /* ───────── 인스턴스 팩토리 ───────── */
    public static CourseDetailFragment newInstance(Route route) {
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
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_course_detail, container, false);

        // Route 객체 획득
        if (getArguments() != null) {
            route = (Route) getArguments().getSerializable(ARG_ROUTE);
        }

        // ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // 네비게이션 바 다시 보이게
        requireActivity().findViewById(R.id.nav_view).setVisibility(View.VISIBLE);

        /* ─── View 참조 ─── */
        ImageView image       = v.findViewById(R.id.detail_image);
        TextView  title       = v.findViewById(R.id.detail_title);
        TextView  info        = v.findViewById(R.id.detail_info);
        TextView  explanation = v.findViewById(R.id.detail_explanation);
        LinearLayout tagsLay  = v.findViewById(R.id.detail_tags);
        TextView  tourPoints  = v.findViewById(R.id.detail_tour_points);
        TextView  surrounding = v.findViewById(R.id.surrounding_points);
        Button    btnNavigate = v.findViewById(R.id.btn_navigate);

        if (route != null) {

            /* ───── 이미지 로딩 ───── */
            final int PLACEHOLDER = R.drawable.loading;        // 로딩 중
            final int ERROR_IMG   = R.drawable.sample_image;   // 실패
            final int NO_URL_IMG  = R.drawable.noimg;          // URL 없음

            if (route.image != null && !route.image.trim().isEmpty()) {
                String src = route.image.trim();
                Object glideSrc = src.startsWith("data:image") ? Uri.parse(src) : src;

                Glide.with(this)
                        .load(glideSrc)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .placeholder(PLACEHOLDER)
                        .error(ERROR_IMG)
                        .centerCrop()
                        .into(image);
            } else {
                image.setImageResource(NO_URL_IMG);
            }
            /* ─────────────────────── */

            // 텍스트 바인딩
            title.setText(route.title);
            info.setText("코스 " + route.dist_km + "Km · " + route.time + "분 · " +
                    (route.tourist_point != null && !route.tourist_point.isEmpty()
                            ? route.tourist_point.get(0) : ""));
            explanation.setText(route.explanation != null ? route.explanation : "");

            // 해시태그
            tagsLay.removeAllViews();
            if (route.category != null) {
                for (String tag : route.category) {
                    TextView chip = new TextView(requireContext());
                    chip.setText("#" + tag);
                    chip.setTextColor(0xFF555555);
                    chip.setTextSize(12);
                    chip.setPadding(0,0,24,0);
                    tagsLay.addView(chip);
                }
            }

            // 관광 포인트
            if (route.tourist_point != null) {
                StringBuilder sb = new StringBuilder();
                for (String p : route.tourist_point) sb.append("• ").append(p).append('\n');
                tourPoints.setText(sb.toString());
            }

            // 주변 POI
            Map<Integer, POI> poiMap = viewModel.getPoiMap().getValue();
            if (route.poi != null && poiMap != null) {
                StringBuilder sb = new StringBuilder();
                for (int id : route.poi) {
                    POI poi = poiMap.get(id);
                    if (poi != null) sb.append("• ").append(poi.name).append('\n');
                }
                surrounding.setText(sb.toString());
            }
        }

        /* ─── 버튼: 코스 시작 ─── */
        btnNavigate.setOnClickListener(v1 -> {
            viewModel.setSelectedRoute(route);
            viewModel.setMapState(MainViewModel.MapState.WALKING);

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setBottomNavigationSelected(R.id.navigation_maps);
            }
        });

        /* ─── 지도 ─── */
        SupportMapFragment mapFrag =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.detail_map);
        if (mapFrag != null) mapFrag.getMapAsync(this);

        return v;
    }

    /* ───────── GoogleMap 콜백 ───────── */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        drawRouteOnMap();
    }

    private void drawRouteOnMap() {
        if (map == null || route == null || route.path == null || route.path.isEmpty()) return;

        PolylineOptions poly = new PolylineOptions().width(10).color(0xFF1976D2);
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();

        for (Point p : route.path) {
            LatLng ll = new LatLng(p.lat, p.lng);
            poly.add(ll);
            bounds.include(ll);
        }
        map.addPolyline(poly);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100));

        if (route.poi != null) {
            Map<Integer, POI> poiMap = viewModel.getPoiMap().getValue();
            if (poiMap != null) {
                for (int id : route.poi) {
                    POI poi = poiMap.get(id);
                    if (poi != null) {
                        LatLng ll = new LatLng(poi.point.lat, poi.point.lng);
                        map.addMarker(new MarkerOptions()
                                .position(ll)
                                .title(poi.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                    }
                }
            }
        }
    }
}
