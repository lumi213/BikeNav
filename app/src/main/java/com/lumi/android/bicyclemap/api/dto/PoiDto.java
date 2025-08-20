package com.lumi.android.bicyclemap.api.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.lumi.android.bicyclemap.Point;

import java.io.Serializable;
import java.util.List;

public class PoiDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String type;
    private Point point;
    private String explanation;
    private String addr;
    private String hour;
    private double rate;
    private String tel;
    @SerializedName(value = "tags", alternate = {"tag"})
    @JsonAdapter(TagsFlexAdapter.class)
    private List<String> tag;
    private List<ImageDto> images;
    private ImageDto mainImages;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public Point getPoint() { return point; }
    public String getExplanation() { return explanation; }
    public String getAddr() { return addr; }
    public String getHour() { return hour; }
    public double getRate() { return rate; }
    public String getTel() { return tel; }
    public List<String> getTag() { return tag; }
    public List<ImageDto> getImages() { return images; }
    public ImageDto getMainImages() { return mainImages; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setPoint(Point point) { this.point = point; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setAddr(String addr) { this.addr = addr; }
    public void setHour(String hour) { this.hour = hour; }
    public void setRate(double rate) { this.rate = rate; }
    public void setTel(String tel) { this.tel = tel; }
    public void setTags(List<String> tags) { this.tag = tags; }
    public void setImages(List<ImageDto> images) { this.images = images; }
    public void setMainImages(ImageDto image) { this.mainImages = image; }

    public static class ImageDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String url;
        private boolean is_main;

        public String getUrl() { return url; }
        public boolean isIs_main() { return is_main; }
        public void setUrl(String url) { this.url = url; }
        public void setIs_main(boolean is_main) { this.is_main = is_main; }
    }

    /** PoiDto 내부 이미지 정보에서 대표 이미지 URL을 찾아 반환한다. 없으면 null */
    private String findMainImageUrl(PoiDto detail) {
        if (detail == null) return null;

        // 1) mainImages 필드가 있으면 우선 사용
        if (detail.getMainImages() != null && detail.getMainImages().getUrl() != null) {
            return detail.getMainImages().getUrl();
        }
        // 2) images 리스트에서 is_main=true 우선
        if (detail.getImages() != null) {
            String first = null;
            for (PoiDto.ImageDto im : detail.getImages()) {
                if (im == null || im.getUrl() == null) continue;
                if (first == null) first = im.getUrl();
                if (im.isIs_main()) {
                    return im.getUrl();
                }
            }
            return first;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PoiDto)) return false;

        PoiDto other = (PoiDto) o;
        // id 기반 비교 (필요하면 name, type 등도 포함 가능)
        return id == other.id &&
                Double.compare(rate, other.rate) == 0 &&
                ((name == null && other.name == null) || (name != null && name.equals(other.name))) &&
                ((addr == null && other.addr == null) || (addr != null && addr.equals(other.addr))) &&
                ((tel == null && other.tel == null) || (tel != null && tel.equals(other.tel)));
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (addr != null ? addr.hashCode() : 0);
        result = 31 * result + (tel != null ? tel.hashCode() : 0);
        result = 31 * result + Double.hashCode(rate);
        return result;
    }
} 