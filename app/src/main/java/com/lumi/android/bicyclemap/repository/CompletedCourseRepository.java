package com.lumi.android.bicyclemap.data.repository;

import android.content.Context;

import com.lumi.android.bicyclemap.data.local.AppDatabase;
import com.lumi.android.bicyclemap.data.local.dao.CompletedCourseDao;
import com.lumi.android.bicyclemap.data.local.entity.CompletedCourseEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletedCourseRepository {
    private final CompletedCourseDao dao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public CompletedCourseRepository(Context c) {
        dao = AppDatabase.get(c).completedCourseDao();
    }

    public void markCompleted(int courseId, String title, Runnable onDone) {
        io.execute(() -> {
            dao.insert(new CompletedCourseEntity(courseId, title, System.currentTimeMillis()));
            if (onDone != null) onDone.run();
        });
    }

    public interface Callback<T> { void onResult(T data); }

    public void getAll(Callback<List<CompletedCourseEntity>> cb) {
        io.execute(() -> cb.onResult(dao.getAll()));
    }

    public void isCompleted(int courseId, Callback<Boolean> cb) {
        io.execute(() -> cb.onResult(dao.countByCourseId(courseId) > 0));
    }
}
