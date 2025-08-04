package com.lumi.android.bicyclemap.api.dto;

import java.util.List;

public class CourseDto {
    private int course_id;
    private String title;
    private double dist_km;
    private int time;
    private String image;
    private int diff;
    private boolean is_recommended;
    private String type;
    private List<PointDto> path;
    private String description;
    private List<ImageDto> images;
    private List<String> tags;
    private List<String> tourist_spots;
    private List<String> nearby_businesses;

    // Getters
    public int getCourse_id() { return course_id; }
    public String getTitle() { return title; }
    public double getDist_km() { return dist_km; }
    public int getTime() { return time; }
    public String getImage() { return image; }
    public int getDiff() { return diff; }
    public boolean isIs_recommended() { return is_recommended; }
    public String getType() { return type; }
    public List<PointDto> getPath() { return path; }
    public String getDescription() { return description; }
    public List<ImageDto> getImages() { return images; }
    public List<String> getTags() { return tags; }
    public List<String> getTourist_spots() { return tourist_spots; }
    public List<String> getNearby_businesses() { return nearby_businesses; }

    // Setters
    public void setCourse_id(int course_id) { this.course_id = course_id; }
    public void setTitle(String title) { this.title = title; }
    public void setDist_km(double dist_km) { this.dist_km = dist_km; }
    public void setTime(int time) { this.time = time; }
    public void setImage(String image) { this.image = image; }
    public void setDiff(int diff) { this.diff = diff; }
    public void setIs_recommended(boolean is_recommended) { this.is_recommended = is_recommended; }
    public void setType(String type) { this.type = type; }
    public void setPath(List<PointDto> path) { this.path = path; }
    public void setDescription(String description) { this.description = description; }
    public void setImages(List<ImageDto> images) { this.images = images; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setTourist_spots(List<String> tourist_spots) { this.tourist_spots = tourist_spots; }
    public void setNearby_businesses(List<String> nearby_businesses) { this.nearby_businesses = nearby_businesses; }

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