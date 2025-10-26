package com.example.sleepytrip;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Location.class}, version = 2, exportSchema = false)  // version = 2!
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract LocationDao locationDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "sleepytrip_database"
                    )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()  // ВАЖНО: пересоздаёт БД при изменении версии
                    .build();
        }
        return instance;
    }
}