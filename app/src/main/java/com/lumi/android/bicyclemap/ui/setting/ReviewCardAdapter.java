package com.lumi.android.bicyclemap.ui.setting;

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

import java.util.ArrayList;
import java.util.List;

/** item_review.xml을 사용하는 단순 카드 어댑터 */
public class ReviewCardAdapter extends RecyclerView.Adapter<ReviewCardAdapter.VH> {

    public static class Row {
        public final String courseTitle;
        public final String diffBadge;   // " 상" / " 중" / " 하" (배지 텍스트)
        public final String courseDetail; // "경로 5.0km · 90분 · #풍경" 등
        public final String content;
        public final String thumbUrl;

        public Row(String courseTitle, String diffBadge, String courseDetail, String content, String thumbUrl) {
            this.courseTitle  = courseTitle;
            this.diffBadge    = diffBadge;
            this.courseDetail = courseDetail;
            this.content      = content;
            this.thumbUrl     = thumbUrl;
        }
    }

    private final List<Row> items = new ArrayList<>();

    public void submit(List<Row> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Row r = items.get(position);
        h.title.setText(r.courseTitle == null ? "" : r.courseTitle);
        h.diff.setText(r.diffBadge == null ? "" : r.diffBadge);
        h.detail.setText(r.courseDetail == null ? "" : r.courseDetail);
        h.contents.setText(r.content == null ? "" : r.content);

        if (!TextUtils.isEmpty(r.thumbUrl)) {
            Glide.with(h.img.getContext()).load(r.thumbUrl).centerCrop()
                    .placeholder(R.drawable.loading)
                    .error(R.drawable.ic_avatar)
                    .into(h.img);
        } else {
            h.img.setImageResource(R.drawable.ic_avatar);
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView diff, title, detail, contents;
        VH(@NonNull View v) {
            super(v);
            img      = v.findViewById(R.id.imgThumb);
            diff     = v.findViewById(R.id.review_course_diff);
            title    = v.findViewById(R.id.review_course_title);
            detail   = v.findViewById(R.id.review_course_detail);
            contents = v.findViewById(R.id.review_contents);
        }
    }
}
