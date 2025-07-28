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
        holder.name.setText(poi.name);
        holder.description.setText(poi.explanation);

        // 이미지 로드 (drawable 리소스 기준)
        String imageName = poi.image.replace(".jpg", "").replace(".png", "");
        int resId = holder.image.getContext().getResources()
                .getIdentifier(imageName, "drawable", holder.image.getContext().getPackageName());
        Glide.with(holder.image.getContext())
                .load(resId)
                .into(holder.image);
    }

    static class POIViewHolder extends RecyclerView.ViewHolder {
        TextView name, description;
        ImageView image;

        public POIViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name_poi);
            description = itemView.findViewById(R.id.explanation_poi);
            image = itemView.findViewById(R.id.image_poi);
        }
    }

    public static final DiffUtil.ItemCallback<POI> DIFF_CALLBACK = new DiffUtil.ItemCallback<POI>() {
        @Override
        public boolean areItemsTheSame(@NonNull POI oldItem, @NonNull POI newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull POI oldItem, @NonNull POI newItem) {
            return oldItem.name.equals(newItem.name)
                    && oldItem.explanation.equals(newItem.explanation)
                    && oldItem.image.equals(newItem.image);
        }
    };
}
