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
import com.lumi.android.bicyclemap.util.ImageLoader;

public class POIDetailFragment extends BottomSheetDialogFragment {

    private static final String ARG_POI = "arg_poi";
    private POI poi;

    /* ───────────────────────── 인스턴스 팩토리 ───────────────────────── */
    public static POIDetailFragment newInstance(@NonNull POI poi) {
        POIDetailFragment f = new POIDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_POI, poi);
        f.setArguments(args);
        return f;
    }

    /* ───────────────────────── onCreateView ───────────────────────── */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.poi_detail, container, false);

        if (getArguments() != null) {
            poi = (POI) getArguments().getSerializable(ARG_POI);
        }

        TextView name  = v.findViewById(R.id.poi_name);
        TextView desc  = v.findViewById(R.id.poi_description);
        ImageView img  = v.findViewById(R.id.poi_image);

        if (poi != null) {
            name.setText(poi.name);
            desc.setText(poi.explanation);

            /* ─── 이미지 로딩 ─── */
            final int PLACEHOLDER = R.drawable.loading;       // 로딩 중
            final int ERROR_IMG   = R.drawable.sample_image;  // 로딩 실패
            final int NO_URL_IMG  = R.drawable.noimg;         // URL 없음

            if (poi.image != null && !poi.image.trim().isEmpty()) {
                // http/https URL 또는 data:image;base64 형식 모두 처리
                ImageLoader.loadFlexible(
                        requireContext(),
                        poi.image.trim(),
                        img,
                        PLACEHOLDER      // placeholder & error = loading
                );

                /* Glide error 처리: ImageLoader.loadFlexible() 안에서
                   error(placeholderResId) 로 지정되어 있으므로 Glide 실패 시
                   loading.png 가 남습니다. 필요하면 아래처럼 콜백으로 errorImage 변경 가능
                 */
                // Glide.with(this).clear(img); // 예: 커스텀 에러 이미지를 쓰려면
            } else {
                img.setImageResource(NO_URL_IMG);
            }
        }

        return v;
    }

    /* ───────────────────────── BottomSheet 높이 조정 ───────────────────────── */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(di -> {
            BottomSheetDialog d = (BottomSheetDialog) di;
            View sheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                sheet.setFitsSystemWindows(false);
            }
        });
        return dialog;
    }
}
