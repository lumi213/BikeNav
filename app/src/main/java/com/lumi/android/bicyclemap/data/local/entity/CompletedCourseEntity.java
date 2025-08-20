package com.lumi.android.bicyclemap.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "completed_courses")
public class CompletedCourseEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "course_id", index = true)
    public int courseId;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "completed_at")
    public long completedAt; // System.currentTimeMillis()

    public CompletedCourseEntity(int courseId, String title, long completedAt) {
        this.courseId = courseId;
        this.title = title;
        this.completedAt = completedAt;
    }
}
