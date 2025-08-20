package com.lumi.android.bicyclemap.api.dto;

import com.google.gson.annotations.SerializedName;
import com.lumi.android.bicyclemap.Point;

import java.util.List;

public class VillagesDto {

    // 공통 (목록/상세)
    @SerializedName("id")
    private int id;

    @SerializedName("village_id")
    private int villageId;

    @SerializedName("village_name")
    private String villageName;

    // 목록에만 오는 경우가 많은 필드들
    @SerializedName("type")               // "food" | "tourism" | "tradition"
    private String type;

    @SerializedName("name")
    private String name;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("recommended")
    private Boolean recommended;          // null 가능

    @SerializedName("path")
    private List<Point> path;       // null 가능 (상세에선 보통 없음)

    // 상세에서 추가 제공되는 필드들
    @SerializedName("village_addr")
    private String villageAddr;           // null 가능

    @SerializedName("tags")
    private List<String> tags;            // null 가능

    @SerializedName("content")
    private Content content;              // null 가능

    // --- inner classes ---
    public static class Content {
        @SerializedName("description")
        private String description;

        @SerializedName("price")
        private String price;

        @SerializedName("menu")
        private List<String> menu;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getPrice() { return price; }
        public void setPrice(String price) { this.price = price; }

        public List<String> getMenu() { return menu; }
        public void setMenu(List<String> menu) { this.menu = menu; }
    }

    // --- getters / setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getVillageId() { return villageId; }
    public void setVillageId(int villageId) { this.villageId = villageId; }

    public String getVillageName() { return villageName; }
    public void setVillageName(String villageName) { this.villageName = villageName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getRecommended() { return recommended; }
    public void setRecommended(Boolean recommended) { this.recommended = recommended; }

    public List<Point> getPath() { return path; }
    public void setPath(List<Point> path) { this.path = path; }

    public String getVillageAddr() { return villageAddr; }
    public void setVillageAddr(String villageAddr) { this.villageAddr = villageAddr; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Content getContent() { return content; }
    public void setContent(Content content) { this.content = content; }
}
