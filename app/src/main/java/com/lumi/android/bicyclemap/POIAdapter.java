package com.lumi.android.bicyclemap;

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

public class POIAdapter extends ListAdapter<POI, POIAdapter.POIViewHolder> {

    public POIAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public POIViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poi, parent, false);
        return new POIViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull POIViewHolder holder, int position) {
        POI poi = getItem(position);

        // 상호명
        holder.name.setText(poi.name);

        // 별점(rate) - 소수점 한 자리 표시
        holder.rating.setText(poi.rate > 0 ? String.format("%.1f", poi.rate) : "-");
        // 별점 아이콘은 XML에 이미지로 있음

        // 카테고리(음식점/카페/화장실 등)
        holder.category.setText(categoryToKor(poi.type));

        // 영업시간(hour)
        holder.time.setText(poi.hour != null ? poi.hour : "");

        // 옵션(메뉴, 전화, 기타)
        holder.option.setText(makeOptionString(poi));

        // 이미지
        if (poi.image != null && !poi.image.isEmpty()) {
            int resId = holder.image.getContext().getResources()
                    .getIdentifier(poi.image.replace(".jpg", "").replace(".png", ""),
                            "drawable", holder.image.getContext().getPackageName());
            if (resId != 0) {
                Glide.with(holder.image.getContext())
                        .load(resId)
                        .placeholder(R.drawable.loading)
                        .into(holder.image);
            } else {
                holder.image.setImageResource(R.drawable.loading);
            }
        } else {
            holder.image.setImageResource(R.drawable.noimg);
        }
    }

    static class POIViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, rating, category, time, option;

        public POIViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.poiImage);
            name = itemView.findViewById(R.id.poiName);
            rating = itemView.findViewById(R.id.poiRating);
            category = itemView.findViewById(R.id.poiCategory);
            time = itemView.findViewById(R.id.poiTime);
            option = itemView.findViewById(R.id.poiOption);
        }
    }

    public static final DiffUtil.ItemCallback<POI> DIFF_CALLBACK = new DiffUtil.ItemCallback<POI>() {
        @Override
        public boolean areItemsTheSame(@NonNull POI oldItem, @NonNull POI newItem) {
            return oldItem.id == newItem.id;
        }
        @Override
        public boolean areContentsTheSame(@NonNull POI oldItem, @NonNull POI newItem) {
            return oldItem.equals(newItem);
        }
    };

    // 유틸: 타입 영문→한글 변환 (예시)
    private static String categoryToKor(String type) {
        if ("Restaurant".equalsIgnoreCase(type) || "음식점".equals(type)) return "음식점";
        if ("Cafe".equalsIgnoreCase(type) || "카페".equals(type)) return "카페";
        if ("Toilet".equalsIgnoreCase(type) || "화장실".equals(type)) return "화장실";
        return type != null ? type : "";
    }

    // 유틸: 옵션 문자열 생성 (전화, 메뉴 등 POI 필드 조합, 필요에 맞게 추가)
    private static String makeOptionString(POI poi) {
        StringBuilder sb = new StringBuilder();
        if (poi.menu != null && !poi.menu.isEmpty()) {
            sb.append(poi.menu);
        }
        if (poi.tel != null && !poi.tel.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("☎ ").append(poi.tel);
        }
        return sb.length() > 0 ? sb.toString() : ""; // 필요시 기본값
    }
}
