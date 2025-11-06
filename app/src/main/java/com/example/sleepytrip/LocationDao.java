package com.example.sleepytrip;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocationDao {

    //добавить локацию в базу
    @Insert
    void insert(Location location);

    //обновить локацию
    @Update
    void update(Location location);

    //удалить локацию
    @Delete
    void delete(Location location);

    //получить все локации, отсортированные по времени создания
    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    List<Location> getAllLocations();

    //получить локацию по id
    @Query("SELECT * FROM locations WHERE id = :id")
    Location getLocationById(int id);

    //удалить все локации
    @Query("DELETE FROM locations")
    void deleteAll();

    //получить только активные локации
    @Query("SELECT * FROM locations WHERE isActive = 1")
    List<Location> getActiveLocations();
}
