package com.example.sleepytrip;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocationDao {

    // Добавить новую локацию
    @Insert
    void insert(Location location);

    // Обновить существующую локацию
    @Update
    void update(Location location);

    // Удалить локацию
    @Delete
    void delete(Location location);

    // Получить все локации (сортировка по дате добавления, новые сверху)
    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    List<Location> getAllLocations();

    // Получить локацию по ID
    @Query("SELECT * FROM locations WHERE id = :id")
    Location getLocationById(int id);

    // Удалить все локации
    @Query("DELETE FROM locations")
    void deleteAll();
}