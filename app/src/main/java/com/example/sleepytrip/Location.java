package com.example.sleepytrip;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "locations")
public class Location {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;          //название локации или остановки
    private String address;       //полный адрес
    private double latitude;      //широта
    private double longitude;     //долгота
    private float radius;         //радиус зоны в метрах
    private long timestamp;       //время добавления локации
    private boolean isActive;     //активна ли локация

    //конструктор создаёт объект с параметрами и ставит время добавления
    public Location(String name, String address, double latitude, double longitude, float radius) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.timestamp = System.currentTimeMillis();
        this.isActive = false;  //по умолчанию выключена
    }

    //возвращает состояние активности
    public boolean isActive() {
        return isActive;
    }

    //устанавливает активность локации
    public void setActive(boolean active) {
        isActive = active;
    }

    //геттеры и сеттеры для всех полей
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
