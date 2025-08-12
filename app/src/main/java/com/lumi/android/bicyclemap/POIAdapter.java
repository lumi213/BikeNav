package com.lumi.android.bicyclemap;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class POIAdapter extends ListAdapter<POI, POIAdapter.POIViewHolder> {

    public POIAdapter() { super(DIFF_CALLBACK); }

    /* ───────────────────────── ViewHolder 생성 ───────────────────────── */
    @NonNull
    @Override
    public POIViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poi, parent, false);
        return new POIViewHolder(v);
    }

    /* ───────────────────────── 데이터 바인딩 ───────────────────────── */
    @Override
    public void onBindViewHolder(@NonNull POIViewHolder h, int pos) {
        POI p = getItem(pos);

        // 텍스트
        h.name.setText(p.name);
        h.rating.setText(p.rate > 0 ? String.format("%.1f", p.rate) : "-");
        h.category.setText(categoryToKor(p.type));
        h.time.setText(p.hour != null ? p.hour : "");
        h.option.setText(makeOptionString(p));

        // 이미지
        final int PLACEHOLDER = R.drawable.loading;       // 로딩 중
        final int ERROR_IMG   = R.drawable.sample_image;  // 로딩 실패
        final int NO_URL_IMG  = R.drawable.noimg;         // URL 없음

        if (p.image != null && !p.image.trim().isEmpty()) {
            String src = p.image.trim();
            Object glideSrc = src.startsWith("data:image")
                    ? Uri.parse(src)           // data URI
                    : src;                     // http / https

            Glide.with(h.image.getContext())
                    .load(glideSrc)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .placeholder(PLACEHOLDER)
                    .error(ERROR_IMG)
                    .centerCrop()
                    .into(h.image);
        } else {
            h.image.setImageResource(NO_URL_IMG);
        }
    }

    /* ───────────────────────── DIFF_CALLBACK ───────────────────────── */
    public static final DiffUtil.ItemCallback<POI> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<POI>() {
                @Override
                public boolean areItemsTheSame(@NonNull POI o,@NonNull POI n){
                    return o.id == n.id;
                }
                @Override
                public boolean areContentsTheSame(@NonNull POI o,@NonNull POI n){
                    return o.equals(n);
                }
            };

    /* ───────────────────────── ViewHolder ───────────────────────── */
    static class POIViewHolder extends RecyclerView.ViewHolder {
        ImageView image;  TextView name, rating, category, time, option;
        POIViewHolder(@NonNull View v) {
            super(v);
            image     = v.findViewById(R.id.poiImage);
            name      = v.findViewById(R.id.poiName);
            rating    = v.findViewById(R.id.poiRating);
            category  = v.findViewById(R.id.poiCategory);
            time      = v.findViewById(R.id.poiTime);
            option    = v.findViewById(R.id.poiOption);
        }
    }

    /* ───────────────────────── 유틸 함수 ───────────────────────── */
    private static String categoryToKor(String t){
        if("Restaurant".equalsIgnoreCase(t)||"음식점".equals(t)) return "음식점";
        if("Cafe".equalsIgnoreCase(t)      ||"카페".equals(t))   return "카페";
        if("Toilet".equalsIgnoreCase(t)    ||"화장실".equals(t)) return "화장실";
        return t!=null ? t : "";
    }

    private static String makeOptionString(POI p){
        StringBuilder sb = new StringBuilder();
        if (p.menu != null && !p.menu.isEmpty()) sb.append(p.menu);
        if (p.tel  != null && !p.tel.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("☎ ").append(p.tel);
        }
        return sb.toString();
    }
}
