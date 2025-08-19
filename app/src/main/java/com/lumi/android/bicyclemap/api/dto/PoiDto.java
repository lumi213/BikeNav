package com.lumi.android.bicyclemap.api.dto;

import java.util.List;

public class PoiDto {
    private int id;
    private String name;
    private String type;
    private PointDto point;
    private String explanation;
    private String addr;
    private String hour;
    private double rate;
    private String tel;
    private String tag;
    private List<ImageDto> images;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public PointDto getPoint() { return point; }
    public String getExplanation() { return explanation; }
    public String getAddr() { return addr; }
    public String getHour() { return hour; }
    public double getRate() { return rate; }
    public String getTel() { return tel; }
    public String getTag() { return tag; }
    public List<ImageDto> getImages() { return images; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setPoint(PointDto point) { this.point = point; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setAddr(String addr) { this.addr = addr; }
    public void setHour(String hour) { this.hour = hour; }
    public void setRate(double rate) { this.rate = rate; }
    public void setImages(List<ImageDto> images) { this.images = images; }

    public static class PointDto {
        private double lat;
        private double lng;

        public double getLat() { return lat; }
        public double getLng() { return lng; }
        public void setLat(double lat) { this.lat = lat; }
        public void setLng(double lng) { this.lng = lng; }
    }

    public static class ImageDto {
        private String url;
        private boolean is_main;

        public String getUrl() { return url; }
        public boolean isIs_main() { return is_main; }
        public void setUrl(String url) { this.url = url; }
        public void setIs_main(boolean is_main) { this.is_main = is_main; }
    }
} 