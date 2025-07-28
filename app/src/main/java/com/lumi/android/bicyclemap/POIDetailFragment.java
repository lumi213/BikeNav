package com.lumi.android.bicyclemap;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class POIDetailFragment extends BottomSheetDialogFragment {

    private static final String ARG_POI = "arg_poi";
    private POI poi;

    public static POIDetailFragment newInstance(POI poi) {
        POIDetailFragment fragment = new POIDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_POI, poi);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.poi_detail, container, false);

        if (getArguments() != null) {
            poi = (POI) getArguments().getSerializable(ARG_POI);
        }

        TextView name = view.findViewById(R.id.poi_name);
        TextView desc = view.findViewById(R.id.poi_description);
        ImageView image = view.findViewById(R.id.poi_image);

        if (poi != null) {
            name.setText(poi.name);
            desc.setText(poi.explanation);
            int resId = getResources().getIdentifier(
                    poi.image.replace(".jpg", "").replace(".png", ""),
                    "drawable",
                    requireContext().getPackageName()
            );
            image.setImageResource(resId);
        }

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                bottomSheet.setLayoutParams(layoutParams);
                bottomSheet.setFitsSystemWindows(false);
            }
        });
        return dialog;
    }
}
