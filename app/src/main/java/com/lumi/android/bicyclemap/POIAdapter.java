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
import com.google.android.gms.actions.ItemListIntents;

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
        h.rating.setText(p.rate > 0 ? String.format("%.1f", p.rate) : "0");
        h.category.setText(categoryToKor(p));
        h.time.setText(makeHourString(p.hour));
        h.tel.setText(p.tel);
        h.option.setText(makeOptionString(p));

        // 이미지
        final int PLACEHOLDER = R.drawable.loading;
        final int ERROR_IMG   = R.drawable.sample_image;
        final int NO_URL_IMG  = R.drawable.noimg;

        String img = p.getImage(); // ★ POI.getImage() == mainImageUrl
        if (img != null && !img.trim().isEmpty()) {
            String src = img.trim();
            Object glideSrc = src.startsWith("data:image") ? Uri.parse(src) : src;

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
        ImageView image, ratingImg;  TextView name, rating, category, time, option, tel;
        POIViewHolder(@NonNull View v) {
            super(v);
            image     = v.findViewById(R.id.poiImage);
            ratingImg = v.findViewById(R.id.poiRatingImg);
            name      = v.findViewById(R.id.poiName);
            rating    = v.findViewById(R.id.poiRating);
            category  = v.findViewById(R.id.poiCategory);
            time      = v.findViewById(R.id.poiTime);
            option    = v.findViewById(R.id.poiOption);
            tel       = v.findViewById(R.id.poiTel);
        }
    }

    /* ───────────────────────── 유틸 함수 ───────────────────────── */
    private static String categoryToKor(POI p){
        StringBuilder sb = new StringBuilder();
        if("biz".equalsIgnoreCase(p.type)) sb.append("상권") ;
        if("util".equalsIgnoreCase(p.type)) sb.append("편의시설");
        if("tourist".equalsIgnoreCase(p.type)) sb.append("관광지");
        if (p.tags != null && !p.tags.isEmpty()) sb.append(" * "+p.tags.get(0));
        return sb.toString();
    }

    private static String makeHourString(String s){
        return s;
    }

    private static String makeOptionString(POI p){
        StringBuilder sb = new StringBuilder();
        if (p.menu != null && !p.menu.isEmpty()) sb.append(p.menu);
        return sb.toString();
    }
}
