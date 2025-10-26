package com.example.sleepytrip;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AppDatabase db;

    @Override
    public void onCreate() {
        super.onCreate();

        // Инициализация
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = AppDatabase.getInstance(this);

        // Создаём notification channel
        createNotificationChannel();

        // Запускаем foreground service
        startForeground(1, createNotification());

        // Настраиваем отслеживание локации
        startLocationTracking();
    }

    private void startLocationTracking() {
        // Настройки запроса локации
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000  // Обновление каждые 10 секунд
        )
                .setMinUpdateIntervalMillis(5000)  // Минимум 5 секунд между обновлениями
                .build();

        // Callback для получения локации
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location currentLocation = locationResult.getLastLocation();
                if (currentLocation != null) {
                    checkProximity(currentLocation);
                }
            }
        };

        // Проверяем разрешения
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Запускаем отслеживание
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
        }
    }



    // Показать будильник
    private void showAlarm(com.example.sleepytrip.Location location) {
        Intent intent = new Intent(this, AlarmActivity.class);
        intent.putExtra("location_name", location.getName());
        intent.putExtra("location_address", location.getAddress());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SleepyTrip активен")
                .setContentText("Отслеживание локаций включено")
                .setSmallIcon(R.drawable.ic_location)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Проверяем близость к активным локациям
    private void checkProximity(Location currentLocation) {
        // Получаем ТОЛЬКО активные локации (оптимизация)
        List<com.example.sleepytrip.Location> locations = db.locationDao().getActiveLocations();

        for (com.example.sleepytrip.Location savedLocation : locations) {
            // Вычисляем расстояние
            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    savedLocation.getLatitude(),
                    savedLocation.getLongitude(),
                    results
            );

            float distance = results[0];

            // Если вошли в радиус - показываем будильник
            if (distance <= savedLocation.getRadius()) {
                showAlarm(savedLocation);

                // ВАЖНО: Автоматически выключаем локацию после срабатывания
                // Чтобы будильник не срабатывал повторно
                savedLocation.setActive(false);
                db.locationDao().update(savedLocation);
            }
        }
    }
}