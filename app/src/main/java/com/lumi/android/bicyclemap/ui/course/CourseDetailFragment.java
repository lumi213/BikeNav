package com.lumi.android.bicyclemap.ui.course;

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
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.Point;
import com.lumi.android.bicyclemap.POI;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.Route;

import java.util.List;
import java.util.Map;

public class CourseDetailFragment extends Fragment implements OnMapReadyCallback {

    private Route route;
    private GoogleMap map;
    private MainViewModel viewModel;

    public static final String ARG_ROUTE = "route";

    public static CourseDetailFragment newInstance(Route route) {
        CourseDetailFragment fragment = new CourseDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ROUTE, route);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course_detail, container, false);

        if (getArguments() != null) {
            route = (Route) getArguments().getSerializable(ARG_ROUTE);
        }

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        requireActivity().findViewById(R.id.nav_view).setVisibility(View.VISIBLE);

        // 기본 정보 바인딩
        ImageView image = view.findViewById(R.id.detail_image);
        TextView title = view.findViewById(R.id.detail_title);
        TextView info = view.findViewById(R.id.detail_info);
        TextView explanation = view.findViewById(R.id.detail_explanation);
        LinearLayout tagsLayout = view.findViewById(R.id.detail_tags);
        TextView tourPoints = view.findViewById(R.id.detail_tour_points);
        TextView surrounding = view.findViewById(R.id.surrounding_points);
        Button btnNavigate = view.findViewById(R.id.btn_navigate);

        if (route != null) {
            int resId = getResources().getIdentifier(
                    route.image.replace(".jpg", "").replace(".png", ""),
                    "drawable", requireContext().getPackageName());
            image.setImageResource(resId);

            title.setText(route.name);
            info.setText("코스 " + route.distance + "Km · " + route.time + "분 · " +
                    (route.tourist_point != null && !route.tourist_point.isEmpty() ? route.tourist_point.get(0) : ""));
            explanation.setText(route.explanation != null ? route.explanation : "");

            tagsLayout.removeAllViews();
            if (route.category != null) {
                for (String tag : route.category) {
                    TextView chip = new TextView(requireContext());
                    chip.setText("#" + tag);
                    chip.setTextColor(0xFF555555);
                    chip.setTextSize(12);
                    chip.setPadding(0, 0, 24, 0);
                    tagsLayout.addView(chip);
                }
            }

            if (route.tourist_point != null) {
                StringBuilder sb = new StringBuilder();
                for (String point : route.tourist_point) {
                    sb.append("• ").append(point).append("\n");
                }
                tourPoints.setText(sb.toString());
            }

            Map<Integer, POI> poiMap = viewModel.getPoiMap().getValue();
            if (route.poi != null && poiMap != null) {
                StringBuilder sb = new StringBuilder();
                for (int poiId : route.poi) {
                    POI poi = poiMap.get(poiId);
                    if (poi != null) {
                        sb.append("• ").append(poi.name).append("\n");
                    }
                }
                surrounding.setText(sb.toString());
            }
        }

        btnNavigate.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).popBackStack();
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.detail_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        drawRouteOnMap();
    }

    private void drawRouteOnMap() {
        if (map == null || route == null || route.points == null || route.points.isEmpty()) return;

        PolylineOptions polyline = new PolylineOptions().width(10).color(0xFF1976D2);
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (Point p : route.points) {
            LatLng latLng = new LatLng(p.lat, p.lng);
            polyline.add(latLng);
            boundsBuilder.include(latLng);
        }

        map.addPolyline(polyline);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));

        if (route.poi != null) {
            Map<Integer, POI> poiMap = viewModel.getPoiMap().getValue();
            if (poiMap != null) {
                for (int poiId : route.poi) {
                    POI poi = poiMap.get(poiId);
                    if (poi != null) {
                        LatLng latLng = new LatLng(poi.point.lat, poi.point.lng);
                        map.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(poi.name)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                    }
                }
            }
        }
    }
}
