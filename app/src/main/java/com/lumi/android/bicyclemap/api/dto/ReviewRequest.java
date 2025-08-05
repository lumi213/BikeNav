package com.lumi.android.bicyclemap.api.dto;

public class ReviewRequest {
    private int user_id;
    private int course_id;
    private int place_id;
    private int fac_id;
    private int tracking_id;
    private int rating;
    private int diff;
    private String content;
    private String img_url;
    private String thumbnail_url;

    // 코스 후기용 생성자
    public ReviewRequest(int user_id, int course_id, int tracking_id, int rating, String content, String img_url, String thumbnail_url) {
        this.user_id = user_id;
        this.course_id = course_id;
        this.tracking_id = tracking_id;
        this.rating = rating;
        this.content = content;
        this.img_url = img_url;
        this.thumbnail_url = thumbnail_url;
    }

    // POI 후기용 생성자
    public ReviewRequest(int user_id, int place_id, int rating, int diff, String content, String img_url, String thumbnail_url) {
        this.user_id = user_id;
        this.place_id = place_id;
        this.rating = rating;
        this.diff = diff;
        this.content = content;
        this.img_url = img_url;
        this.thumbnail_url = thumbnail_url;
    }

    // 편의시설 후기용 생성자
    public ReviewRequest(int user_id, int fac_id, int rating, String content, String img_url, String thumbnail_url) {
        this.user_id = user_id;
        this.fac_id = fac_id;
        this.rating = rating;
        this.content = content;
        this.img_url = img_url;
        this.thumbnail_url = thumbnail_url;
    }

    // Getters
    public int getUserId() { return user_id; }
    public int getCourseId() { return course_id; }
    public int getPlaceId() { return place_id; }
    public int getFacId() { return fac_id; }
    public int getTrackingId() { return tracking_id; }
    public int getRating() { return rating; }
    public int getDiff() { return diff; }
    public String getContent() { return content; }
    public String getImgUrl() { return img_url; }
    public String getThumbnailUrl() { return thumbnail_url; }

    // Setters
    public void setUserId(int user_id) { this.user_id = user_id; }
    public void setCourseId(int course_id) { this.course_id = course_id; }
    public void setPlaceId(int place_id) { this.place_id = place_id; }
    public void setFacId(int fac_id) { this.fac_id = fac_id; }
    public void setTrackingId(int tracking_id) { this.tracking_id = tracking_id; }
    public void setRating(int rating) { this.rating = rating; }
    public void setDiff(int diff) { this.diff = diff; }
    public void setContent(String content) { this.content = content; }
    public void setImgUrl(String img_url) { this.img_url = img_url; }
    public void setThumbnailUrl(String thumbnail_url) { this.thumbnail_url = thumbnail_url; }
} 