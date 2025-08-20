package com.lumi.android.bicyclemap.ui.tour;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.dto.VillagesDto;

import java.util.ArrayList;
import java.util.List;

/**
 * 관광 탭 목록 어댑터
 * - item_tour.xml 레이아웃에 바인딩
 * - 4.7 목록 응답(VillagesListResponse.data.specialties)을 표시
 */
public class TourAdapter extends RecyclerView.Adapter<TourAdapter.TourVH> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull VillagesDto item, int position);
    }

    private final Context context;
    private final OnItemClickListener clickListener;
    private final List<VillagesDto> items = new ArrayList<>();

    public TourAdapter(@NonNull Context context, @NonNull OnItemClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
    }

    public void setItems(List<VillagesDto> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void addItems(List<VillagesDto> more) {
        if (more == null || more.isEmpty()) return;
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public VillagesDto getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public TourVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour, parent, false);
        return new TourVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TourVH h, int position) {
        VillagesDto item = items.get(position);

        // 이미지
        String imageUrl = item.getImageUrl();
        Glide.with(h.itemView)
                .load(imageUrl)
                .placeholder(R.drawable.loading)      // 레이아웃에 쓰신 drawable 참조
                .error(R.drawable.sample_image)       // 실패 시 대체 이미지
                .centerCrop()
                .into(h.poiImage);

        // 이름: name이 비면 village_name 사용
        String title = !TextUtils.isEmpty(item.getName())
                ? item.getName()
                : (!TextUtils.isEmpty(item.getVillageName()) ? item.getVillageName() : "이름 없음");
        h.tourName.setText(title);

        // 카테고리 (영문 → 한글 매핑)
        h.tourCategory.setText(mapTypeToKorean(item.getType()));

        // 주소(목록 4.7에는 보통 village_name만 들어오므로, 우선순위: addr → name)
        String addr = !TextUtils.isEmpty(item.getVillageAddr())
                ? item.getVillageAddr()
                : (!TextUtils.isEmpty(item.getVillageName()) ? item.getVillageName() : "주소 정보 없음");
        h.tourAddress.setText(addr);

        // 설명(상세에만 올 수 있음: content.description; 목록이면 null일 수 있음)
        String desc = (item.getContent() != null && !TextUtils.isEmpty(item.getContent().getDescription()))
                ? item.getContent().getDescription()
                : ""; // 목록에서는 비워두거나 다른 정보로 대체 가능
        h.tourDescription.setText(desc);

        // 클릭
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onItemClick(item, h.getBindingAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TourVH extends RecyclerView.ViewHolder {
        ImageView poiImage;
        TextView tourName;
        TextView tourCategory;
        TextView tourAddress;
        TextView tourDescription;

        TourVH(@NonNull View itemView) {
            super(itemView);
            poiImage = itemView.findViewById(R.id.poiImage);
            tourName = itemView.findViewById(R.id.tourName);
            tourCategory = itemView.findViewById(R.id.tourCategory);
            tourAddress = itemView.findViewById(R.id.tourAddress);
            tourDescription = itemView.findViewById(R.id.tourDescription);
        }
    }

    private static String safe(String s, String fallback) {
        return TextUtils.isEmpty(s) ? fallback : s;
    }

    private static String mapTypeToKorean(String type) {
        if (type == null) return "기타";
        switch (type) {
            case "food": return "음식";
            case "tourism": return "관광";
            case "tradition": return "전통";
            default: return "기타";
        }
    }
}
