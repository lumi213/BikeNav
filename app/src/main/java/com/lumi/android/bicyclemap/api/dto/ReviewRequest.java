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


    /* -------- 정적 팩토리 메서드 -------- */

    /** 코스 후기 (명세 6.1) */
    public static ReviewRequest forCourse(
            int userId, int courseId, int trackingId,
            int rating, String content,
            String imgUrl, String thumbnailUrl) {

        return new ReviewRequest(userId, rating, content, imgUrl, thumbnailUrl)
                .setCourseId(courseId)
                .setTrackingId(trackingId);
    }

    /** POI 후기 (명세 3.3) */
    public static ReviewRequest forPoi(
            int userId, int placeId, int rating, int diff,
            String content, String imgUrl, String thumbnailUrl) {

        return new ReviewRequest(userId, rating, content, imgUrl, thumbnailUrl)
                .setPlaceId(placeId)
                .setDiff(diff);
    }

    /** 편의시설 후기 (명세 7.2) */
    public static ReviewRequest forFacility(
            int userId, int facId, int rating,
            String content, String imgUrl, String thumbnailUrl) {

        return new ReviewRequest(userId, rating, content, imgUrl, thumbnailUrl)
                .setFacId(facId);
    }

    /* -------- 내부 공통 생성자 -------- */

    private ReviewRequest(int userId, int rating,
                          String content, String imgUrl, String thumbnailUrl) {
        this.user_id       = userId;
        this.rating        = rating;
        this.content       = content;
        this.img_url       = imgUrl;
        this.thumbnail_url = thumbnailUrl;
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
    public void setRating(int rating) { this.rating = rating; }
    public void setContent(String content) { this.content = content; }
    public void setImgUrl(String img_url) { this.img_url = img_url; }
    public void setThumbnailUrl(String thumbnail_url) { this.thumbnail_url = thumbnail_url; }
    /* -------- 체인 빌더 스타일 setter -------- */

    private ReviewRequest setCourseId(int id)   { this.course_id = id; return this; }
    private ReviewRequest setTrackingId(int id) { this.tracking_id = id; return this; }
    private ReviewRequest setPlaceId(int id)    { this.place_id = id; return this; }
    private ReviewRequest setFacId(int id)      { this.fac_id = id; return this; }
    private ReviewRequest setDiff(int diff)     { this.diff = diff; return this; }
} 