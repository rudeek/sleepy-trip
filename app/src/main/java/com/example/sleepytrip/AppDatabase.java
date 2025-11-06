package com.example.sleepytrip;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Location.class}, version = 2, exportSchema = false)  // version = 2!
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    //возвращает DAO для работы с локациями
    public abstract LocationDao locationDao();

    //возвращает один и тот же экземпляр базы данных
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "sleepytrip_database"
                    )
                    .allowMainThreadQueries()  //разрешает запросы в главном потоке
                    .fallbackToDestructiveMigration()  //важно: пересоздаёт БД при изменении версии
                    .build();
        }
        return instance;
    }
}
