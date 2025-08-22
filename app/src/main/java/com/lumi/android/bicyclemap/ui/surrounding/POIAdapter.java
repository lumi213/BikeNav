package com.lumi.android.bicyclemap.ui.surrounding;

import android.net.Uri;
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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.lumi.android.bicyclemap.R;
import com.lumi.android.bicyclemap.api.dto.PoiDto;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class POIAdapter extends ListAdapter<PoiDto, POIAdapter.POIViewHolder> {

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
        PoiDto p = getItem(pos);
        if (p == null) {
            h.name.setText("");
            h.rating.setText("0");
            h.category.setText("");
            h.time.setText("");
            h.tel.setText("");
            h.option.setText("");
            h.image.setImageResource(R.drawable.noimg);
            return;
        }

        // 텍스트들 NPE 방어
        h.name.setText(p.getName() != null ? p.getName() : "");
        h.rating.setText(p.getRate() > 0 ? String.format("%.1f", p.getRate()) : "0");
        h.category.setText(categoryToKor(p));
        h.time.setText(p.getHour() != null ? p.getHour() : "");
        h.tel.setText(p.getTel() != null ? p.getTel() : "");
        h.option.setText(makeOptionString(p));

        // 이미지 (null-safe, 다단계 폴백)
        final int PLACEHOLDER = R.drawable.loading;
        final int ERROR_IMG   = R.drawable.sample_image;
        final int NO_URL_IMG  = R.drawable.noimg;

        String imageUrl = findMainImageUrl(p);
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Object glideSrc = imageUrl.startsWith("data:image") ? Uri.parse(imageUrl) : imageUrl;
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
    /** 대표 이미지 URL 안전 추출: mainImages → images(is_main) → images 첫 URL */
    private String findMainImageUrl(PoiDto d) {
        if (d == null) return null;

        PoiDto.ImageDto main = d.getMainImages();
        if (main != null && main.getUrl() != null && !main.getUrl().isEmpty()) {
            return main.getUrl();
        }

        if (d.getImages() != null) {
            String first = null;
            for (PoiDto.ImageDto im : d.getImages()) {
                if (im == null) continue;
                String url = im.getUrl();
                if (url == null || url.isEmpty()) continue;
                if (first == null) first = url;
                if (im.isIs_main()) return url;
            }
            return first;
        }
        return null;
    }

    /* ───────────────────────── DIFF_CALLBACK ───────────────────────── */
    public static final DiffUtil.ItemCallback<PoiDto> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PoiDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull PoiDto o,@NonNull PoiDto n){
                    return o.getId() == n.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull PoiDto o,@NonNull PoiDto n){
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
    private static String categoryToKor(PoiDto p) {
        StringBuilder sb = new StringBuilder();

        if ("biz".equalsIgnoreCase(p.getType())) sb.append("상권");
        if ("util".equalsIgnoreCase(p.getType())) sb.append("편의시설");
        if ("tourist".equalsIgnoreCase(p.getType())) sb.append("관광지");

        // ----- tag1("식당") 추출 -----
        String tag1Value = null;
        List<String> tags = p.getTag();

        if (tags != null && !tags.isEmpty()) {
            // 1) 리스트 안의 문자열 중 JSON 객체처럼 보이는 항목에서 "tag1" 뽑기
            for (String t : tags) {
                if (t == null) continue;
                String s = t.trim();
                if (s.startsWith("{") && s.endsWith("}")) {
                    try {
                        JsonObject obj = JsonParser.parseString(s).getAsJsonObject();
                        if (obj.has("tag1")) {
                            tag1Value = obj.get("tag1").getAsString(); // <-- "식당"
                            break;
                        }
                    } catch (Exception ignore) { /* 무시하고 다음 후보 검사 */ }
                }
            }

            // 2) JSON이 아니면(평문 리스트라면) 첫 값으로 폴백
            if (tag1Value == null) {
                // 만약 "tag1":"식당" 같은 문자열이 들어오는 경우를 대비한 정규식 처리
                Pattern TAG1 = Pattern.compile("\"tag1\"\\s*:\\s*\"([^\"]+)\"");
                for (String t : tags) {
                    if (t == null) continue;
                    Matcher m = TAG1.matcher(t);
                    if (m.find()) { tag1Value = m.group(1); break; }
                }
            }
            if (tag1Value == null) {
                // 정말로 평문 태그 리스트라면 첫 항목 사용
                tag1Value = tags.get(0);
            }
        }

        if (tag1Value != null && !tag1Value.isEmpty()) {
            sb.append(" * ").append(tag1Value);
        }

        return sb.toString();
    }

    private static String makeHourString(String s){
        return s;
    }

    private static String makeOptionString(PoiDto p){
        StringBuilder sb = new StringBuilder();
        //if (p.menu != null && !p.menu.isEmpty()) sb.append(p.menu);
        return sb.toString();
    }
}
