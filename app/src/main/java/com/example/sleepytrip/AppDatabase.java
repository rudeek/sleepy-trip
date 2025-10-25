package com.example.sleepytrip;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Location.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Singleton instance
    private static AppDatabase instance;

    // Абстрактный метод для получения DAO
    public abstract LocationDao locationDao();

    // Получить единственный экземпляр базы данных (Singleton pattern)
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "sleepytrip_database"
                    )
                    .allowMainThreadQueries() // ТОЛЬКО для разработки! В продакшене используйте AsyncTask или Coroutines
                    .build();
        }
        return instance;
    }
}