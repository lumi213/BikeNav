package com.lumi.android.bicyclemap.api.dto;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.lumi.android.bicyclemap.Point;

import java.io.Serializable;
import java.util.List;

public class CourseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    @SerializedName("course_id") private int course_id;
    private String image;
    private boolean is_recommended;
    @SerializedName(value = "diff", alternate = {"difficulty"})
    private int diff;
    @SerializedName("time") private int time;
    @SerializedName("title") private String title;
    @SerializedName(value = "type", alternate = {"course_type"})
    private String type;
    @SerializedName(value = "dist_km", alternate = {"distance_km", "distance"})
    private double dist_km;
    @SerializedName(value = "path", alternate = {"path_points", "polyline"})
    private List<Point> path;
    private String description;
    private List<ImageDto> images;
    @SerializedName(value = "tags", alternate = {"tag"})
    @JsonAdapter(TagsFlexAdapter.class)
    private List<String> tags;
    private List<String> tourist_spots;
    private List<String> nearby_businesses;

    public List<Integer> poi;   // 코스 주변 poi id

    // Getters
    public int getCourse_id() { return course_id; }
    public String getTitle() { return title; }
    public double getDist_km() { return dist_km; }
    public int getTime() { return time; }
    public String getImage() { return image; }
    public int getDiff() { return diff; }
    public boolean isIs_recommended() { return is_recommended; }
    public String getType() { return type; }
    public List<Point> getPath() { return path; }
    public String getDescription() { return description; }
    public List<ImageDto> getImages() { return images; }
    public List<String> getTags() { return tags; }
    public List<String> getTourist_spots() { return tourist_spots; }
    public List<String> getNearby_businesses() { return nearby_businesses; }
    public List<Integer> getPoi() { return  poi; }

    // Setters
    public void setCourse_id(int course_id) { this.course_id = course_id; }
    public void setTitle(String title) { this.title = title; }
    public void setDist_km(double dist_km) { this.dist_km = dist_km; }
    public void setTime(int time) { this.time = time; }
    public void setImage(String image) { this.image = image; }
    public void setDiff(int diff) { this.diff = diff; }
    public void setIs_recommended(boolean is_recommended) { this.is_recommended = is_recommended; }
    public void setType(String type) { this.type = type; }
    public void setPath(List<Point> path) { this.path = path; }
    public void setDescription(String description) { this.description = description; }
    public void setImages(List<ImageDto> images) { this.images = images; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setTourist_spots(List<String> tourist_spots) { this.tourist_spots = tourist_spots; }
    public void setNearby_businesses(List<String> nearby_businesses) { this.nearby_businesses = nearby_businesses; }
    public void setPoi(List<Integer> poi) { this.poi = poi; }

    public static class ImageDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String url;
        private boolean is_main;

        public String getUrl() { return url; }
        public boolean isIs_main() { return is_main; }
        public void setUrl(String url) { this.url = url; }
        public void setIs_main(boolean is_main) { this.is_main = is_main; }
    }
} 