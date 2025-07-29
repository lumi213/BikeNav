package com.lumi.android.bicyclemap.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.lumi.android.bicyclemap.MainViewModel;
import com.lumi.android.bicyclemap.POI;
import com.lumi.android.bicyclemap.POIDetailFragment;
import com.lumi.android.bicyclemap.Point;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.Route;
import com.lumi.android.bicyclemap.RouteAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    // === LOG 태그 ===
    public static final String TAG = "MapsFragmentDebug";

    private GoogleMap map;
    private MainViewModel viewModel;
    private RecyclerView routeRecyclerView;
    private RouteAdapter adapter;
    private RadioButton radioWalk, radioBike;
    private List<Route> walkRoutes = new ArrayList<>();
    private List<Route> bikeRoutes = new ArrayList<>();

    // 전역 상태 연동용
    public enum State {
        GENERAL,      // 코스 선택 모드
        WALKING       // 산책/진행 모드
    }
    private State currentState = State.GENERAL;

    private LinearLayout routeInfoLayout;
    private TextView textRouteTitle, textRouteDistTime, textRemainingDistance;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // 뷰 참조
        routeRecyclerView = view.findViewById(R.id.routeRecyclerView);
        routeRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new RouteAdapter(viewModel);
        routeRecyclerView.setAdapter(adapter);

        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(routeRecyclerView);

        routeRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && snapHelper != null) {
                    View centerView = snapHelper.findSnapView(recyclerView.getLayoutManager());
                    if (centerView != null) {
                        int pos = recyclerView.getLayoutManager().getPosition(centerView);
                        Route route = adapter.getCurrentList().get(pos);
                        viewModel.setSelectedRoute(route);
                        // GENERAL 모드에서 슬라이드 변경 시 routeInfoLayout은 자동 GONE 유지
                    }
                }
            }
        });

        radioWalk = view.findViewById(R.id.radio_walk);
        radioBike = view.findViewById(R.id.radio_bike);

        radioWalk.setOnClickListener(v -> {
            routeRecyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(walkRoutes);
        });

        radioBike.setOnClickListener(v -> {
            routeRecyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(bikeRoutes);
        });

        // 산책상태 정보창
        routeInfoLayout = view.findViewById(R.id.routeInfoLayout);
        textRouteTitle = view.findViewById(R.id.text_route_title);
        textRouteDistTime = view.findViewById(R.id.text_route_dist_time);
        textRemainingDistance = view.findViewById(R.id.text_remaining_distance);

        // === 상태 observe (ViewModel의 mapState만 observe)
        viewModel.getMapState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                currentState = (state == MainViewModel.MapState.WALKING) ? State.WALKING : State.GENERAL;
            } else {
                currentState = State.GENERAL;
            }
            Log.d(TAG, "getMapState.observe: currentState = " + currentState);
            updateUIByState();
        });

        // 최초 진입시 리스트 GONE
        routeRecyclerView.setVisibility(View.GONE);

        // --- UI 상태 최초 갱신
        updateUIByState();

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    private void updateUIByState() {
        Log.d(TAG, "updateUIByState: currentState = " + currentState);
        if (currentState == State.GENERAL) {
            routeInfoLayout.setVisibility(View.GONE);
            radioWalk.setVisibility(View.VISIBLE);
            radioBike.setVisibility(View.VISIBLE);
            routeRecyclerView.setVisibility(View.GONE);
        } else if (currentState == State.WALKING) {
            routeInfoLayout.setVisibility(View.VISIBLE);
            radioWalk.setVisibility(View.GONE);
            radioBike.setVisibility(View.GONE);
            routeRecyclerView.setVisibility(View.GONE);
            updateRouteInfo();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        viewModel.getAllRoutes().observe(getViewLifecycleOwner(), routes -> {
            walkRoutes.clear();
            bikeRoutes.clear();
            for (Route r : routes) {
                if (r.image != null && r.image.contains("walk")) walkRoutes.add(r);
                else bikeRoutes.add(r);
            }
            Log.d(TAG, "getAllRoutes: walkRoutes=" + walkRoutes.size() + ", bikeRoutes=" + bikeRoutes.size());
        });

        // 1. observe 전에 현재 값이 있는지 먼저 체크
        Route selected = viewModel.getSelectedRoute().getValue();
        if (selected != null) {
            Log.d(TAG, "onMapReady: selectedRoute exists on init = " + selected.title);
            drawRouteOnMap(selected);
            enableMyLocation(false);
            moveCameraToRoute(selected);
        } else
        {
            enableMyLocation(true);
        }

        viewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            Log.d(TAG, "selectedRoute.observe: currentState = " + currentState + ", route = " + (route != null ? route.title : "null"));
            if (route != null) {
                drawRouteOnMap(route);
                moveCameraToRoute(route);
                if (currentState == State.WALKING) {
                    updateRouteInfo();
                }
            }
        });

        map.setOnMarkerClickListener(marker -> {
            POI poi = (POI) marker.getTag();
            if (poi != null) {
                POIDetailFragment.newInstance(poi).show(getParentFragmentManager(), "poi_detail");
                showPOIDetail(poi);
                return true;
            }
            return false;
        });
    }

    private void updateRouteInfo() {
        Route route = viewModel.getSelectedRoute().getValue();
        Log.d(TAG, "updateRouteInfo: called. route = " + (route != null ? route.title : "null"));
        if (route != null) {
            textRouteTitle.setText(route.title);
            textRouteDistTime.setText("약 " + route.dist_km + "km · " + route.time + "분");
            textRemainingDistance.setText("남은 거리 " + route.dist_km + "Km");
        }
    }

    private void enableMyLocation(boolean moveCameraToCurrent) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);

            if (moveCameraToCurrent) {
                FusedLocationProviderClient fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(requireActivity());
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16));
                    }
                });
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }


    private void drawRouteOnMap(Route route) {
        if (map == null) return;
        map.clear();

        List<Point> path = route.getPath();
        if (path == null || path.isEmpty()) return;

        PolylineOptions polylineOptions = new PolylineOptions().width(10).color(Color.BLUE);
        for (Point p : path) {
            polylineOptions.add(new LatLng(p.lat, p.lng));
        }
        map.addPolyline(polylineOptions);

        Map<Integer, POI> poiMap = viewModel.getPoiMap().getValue();
        if (poiMap == null) return;

        for (int poiId : route.getPoi()) {
            POI poi = poiMap.get(poiId);
            if (poi != null) {
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(new LatLng(poi.point.lat, poi.point.lng))
                        .title(poi.name)
                        .snippet(poi.explanation)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                );
                if (marker != null) marker.setTag(poi);
            }
        }
    }

    private void moveCameraToRoute(Route route) {
        List<Point> path = route.getPath();
        if (path == null || path.isEmpty() || map == null) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Point p : path) {
            builder.include(new LatLng(p.lat, p.lng));
        }
        try {
            LatLngBounds bounds = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            Log.e(TAG, "moveCameraToRoute error: " + e.getMessage());
        }
    }

    private void showPOIDetail(POI poi) {
        // TODO: 상세정보 UI 연결
    }
}
