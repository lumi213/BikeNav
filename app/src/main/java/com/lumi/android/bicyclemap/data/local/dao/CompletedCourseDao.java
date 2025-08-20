package com.lumi.android.bicyclemap.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.lumi.android.bicyclemap.data.local.entity.CompletedCourseEntity;

import java.util.List;

@Dao
public interface CompletedCourseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CompletedCourseEntity e);

    @Query("SELECT * FROM completed_courses ORDER BY completed_at DESC")
    List<CompletedCourseEntity> getAll();

    @Query("SELECT COUNT(*) FROM completed_courses WHERE course_id = :courseId")
    int countByCourseId(int courseId);
}
