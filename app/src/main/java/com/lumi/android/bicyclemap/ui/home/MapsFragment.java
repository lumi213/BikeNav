package com.lumi.android.bicyclemap.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

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

    private GoogleMap map;
    private MainViewModel viewModel;
    private RecyclerView routeRecyclerView;
    private RouteAdapter adapter;
    private RadioButton radioWalk, radioBike;
    private List<Route> walkRoutes = new ArrayList<>();
    private List<Route> bikeRoutes = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        routeRecyclerView = view.findViewById(R.id.routeRecyclerView);
        routeRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new RouteAdapter(viewModel);
        routeRecyclerView.setAdapter(adapter);

        // 💡 중앙 정렬 스냅 적용
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

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        enableMyLocation();

        viewModel.getAllRoutes().observe(getViewLifecycleOwner(), routes -> {
            walkRoutes.clear();
            bikeRoutes.clear();
            for (Route r : routes) {
                if (r.image.contains("walk")) walkRoutes.add(r);
                else bikeRoutes.add(r);
            }
        });

        viewModel.getSelectedRoute().observe(getViewLifecycleOwner(), route -> {
            if (route != null) {
                drawRouteOnMap(route);
                moveCameraToRoute(route);
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

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16));
                }
            });
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }

    private void drawRouteOnMap(Route route) {
        if (map == null) return;
        map.clear();

        PolylineOptions polylineOptions = new PolylineOptions().width(10).color(Color.BLUE);
        for (Point p : route.getPoints()) {
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
        if (route.getPoints() == null || route.getPoints().isEmpty()) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Point p : route.getPoints()) {
            builder.include(new LatLng(p.lat, p.lng));
        }
        LatLngBounds bounds = builder.build();
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    private void showPOIDetail(POI poi) {
        // TODO: 상세정보 UI 연결
    }
}
