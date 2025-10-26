package com.example.sleepytrip;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocationDao {

    @Insert
    void insert(Location location);

    @Update
    void update(Location location);

    @Delete
    void delete(Location location);

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    List<Location> getAllLocations();

    @Query("SELECT * FROM locations WHERE id = :id")
    Location getLocationById(int id);

    @Query("DELETE FROM locations")
    void deleteAll();

    // Получить только активные локации
    @Query("SELECT * FROM locations WHERE isActive = 1")
    List<Location> getActiveLocations();
}