package com.example.sleepytrip;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final String ALARM_CHANNEL_ID = "AlarmChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AppDatabase db;
    private PowerManager.WakeLock wakeLock;

    private final Map<Integer, Long> lastAlarmTriggers = new HashMap<>();
    private static final long ALARM_COOLDOWN = 5 * 60 * 1000; // 5 минут

    private Location currentUserLocation;

    // Handler для периодических обновлений notification
    private Handler notificationUpdateHandler;
    private Runnable notificationUpdateRunnable;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = AppDatabase.getInstance(this);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SleepyTrip::LocationWakeLock");
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 60 * 1000L);
        }

        createNotificationChannels();

        try {
            startForeground(1, createNotification("Инициализация...", ""));
        } catch (Exception e) {
            Log.e("LocationService", "Foreground start failed", e);
        }

        startLocationTracking();
        startPeriodicNotificationUpdates(); // Запускаем периодические обновления
    }

    // === ПЕРИОДИЧЕСКОЕ ОБНОВЛЕНИЕ NOTIFICATION КАЖДЫЕ 15 СЕКУНД ===
    private void startPeriodicNotificationUpdates() {
        notificationUpdateHandler = new Handler(Looper.getMainLooper());

        notificationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // Обновляем notification если есть текущая локация
                if (currentUserLocation != null) {
                    updateNotificationWithDistance(currentUserLocation);
                    Log.d("LocationService", "📍 Notification обновлен");
                }

                // Повторяем через 15 секунд
                notificationUpdateHandler.postDelayed(this, 15000);
            }
        };

        // Первое обновление через 5 секунд после старта
        notificationUpdateHandler.postDelayed(notificationUpdateRunnable, 5000);
    }

    private void startLocationTracking() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000  // Каждые 10 секунд
        )
                .setMinUpdateIntervalMillis(5000)
                .setWaitForAccurateLocation(false)
                .setMaxUpdateDelayMillis(20000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentUserLocation = location;
                    Log.d("LocationService", String.format("📍 Локация обновлена: %.6f, %.6f (точность: %.1fm)",
                            location.getLatitude(), location.getLongitude(), location.getAccuracy()));

                    // Проверяем близость (будильник)
                    checkProximity(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );

            // Получаем текущую локацию сразу при старте
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            currentUserLocation = location;
                            Log.d("LocationService", "✅ Начальная локация получена");
                            checkProximity(location);
                            updateNotificationWithDistance(location);
                        }
                    });
        }
    }

    private void updateNotificationWithDistance(Location currentLocation) {
        List<com.example.sleepytrip.Location> locations = db.locationDao().getActiveLocations();

        if (locations.isEmpty()) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(1, createNotification("Нет активных локаций", "Включите локации в приложении"));
            }
            return;
        }

        com.example.sleepytrip.Location nearestLocation = null;
        float minDistance = Float.MAX_VALUE;

        for (com.example.sleepytrip.Location location : locations) {
            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    location.getLatitude(),
                    location.getLongitude(),
                    results
            );

            float distance = results[0];
            if (distance < minDistance) {
                minDistance = distance;
                nearestLocation = location;
            }
        }

        if (nearestLocation != null) {
            String locationName = nearestLocation.getName();
            String distanceText;

            if (minDistance >= 1000) {
                distanceText = String.format(Locale.getDefault(), "%.1f км", minDistance / 1000);
            } else {
                distanceText = String.format(Locale.getDefault(), "%.0f м", minDistance);
            }

            String title = "📍 " + locationName;
            String text;

            // Если очень близко - предупреждение
            if (minDistance <= nearestLocation.getRadius()) {
                text = "🔔 ВЫ В ЗОНЕ! " + distanceText;
            } else if (minDistance <= nearestLocation.getRadius() * 2) {
                text = "⚠️ Близко к зоне! " + distanceText;
            } else {
                text = "Расстояние: " + distanceText;
            }

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(1, createNotification(title, text));
            }
        }
    }

    private void checkProximity(Location currentLocation) {
        List<com.example.sleepytrip.Location> locations = db.locationDao().getActiveLocations();

        for (com.example.sleepytrip.Location savedLocation : locations) {
            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    savedLocation.getLatitude(),
                    savedLocation.getLongitude(),
                    results
            );

            float distance = results[0];
            Long lastTrigger = lastAlarmTriggers.get(savedLocation.getId());
            long currentTime = System.currentTimeMillis();
            boolean canTrigger = (lastTrigger == null) || (currentTime - lastTrigger > ALARM_COOLDOWN);

            if (distance <= savedLocation.getRadius() && canTrigger) {
                Log.w("LocationService",
                        "🚨 БУДИЛЬНИК СРАБОТАЛ! Дистанция: " + distance + "м, Радиус: " + savedLocation.getRadius() + "м");

                lastAlarmTriggers.put(savedLocation.getId(), currentTime);

                // СНАЧАЛА notification (более надёжно)
                showAlarmNotification(savedLocation);

                // Затем пытаемся запустить Activity
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    showAlarmActivity(savedLocation);
                }, 500);

                // Выключаем локацию через 3 секунды
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    savedLocation.setActive(false);
                    db.locationDao().update(savedLocation);
                    lastAlarmTriggers.remove(savedLocation.getId());
                    Log.d("LocationService", "❌ Локация выключена: " + savedLocation.getName());
                }, 3000);
            }
        }
    }

    private void showAlarmNotification(com.example.sleepytrip.Location location) {
        Intent fullScreenIntent = new Intent(this, AlarmActivity.class);
        fullScreenIntent.putExtra("location_name", location.getName());
        fullScreenIntent.putExtra("location_address", location.getAddress());
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                999, // Уникальный request code
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Создаём звук/вибрацию для будильника
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("🔔 ВЫ ПРИБЫЛИ!")
                .setContentText(location.getName())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(fullScreenPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setOngoing(false); // Можно смахнуть после нажатия

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(999, builder.build());
            Log.d("LocationService", "🔔 Alarm notification показан");
        }
    }

    private void showAlarmActivity(com.example.sleepytrip.Location location) {
        try {
            Intent intent = new Intent(this, AlarmActivity.class);
            intent.putExtra("location_name", location.getName());
            intent.putExtra("location_address", location.getAddress());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
            Log.d("LocationService", "✅ AlarmActivity запущена");
        } catch (Exception e) {
            Log.e("LocationService", "❌ Ошибка запуска AlarmActivity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Notification createNotification(String title, String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)  // НЕ СМАХИВАЕТСЯ
                .setSound(null)    // БЕЗ ЗВУКА
                .setOnlyAlertOnce(true) // Звук только при первом показе
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                // Channel для отслеживания (БЕЗ ЗВУКА)
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Отслеживание локаций",
                        NotificationManager.IMPORTANCE_LOW
                );
                serviceChannel.setDescription("Показывает расстояние до локаций");
                serviceChannel.setSound(null, null); // БЕЗ ЗВУКА
                serviceChannel.enableVibration(false);
                serviceChannel.setShowBadge(false);
                manager.createNotificationChannel(serviceChannel);

                // Channel для будильника (СО ЗВУКОМ)
                NotificationChannel alarmChannel = new NotificationChannel(
                        ALARM_CHANNEL_ID,
                        "Будильник прибытия",
                        NotificationManager.IMPORTANCE_HIGH
                );
                alarmChannel.setDescription("Срабатывает при прибытии к локации");
                alarmChannel.enableVibration(true);
                alarmChannel.setShowBadge(true);
                // Используем стандартный звук уведомления
                manager.createNotificationChannel(alarmChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Останавливаем периодические обновления
        if (notificationUpdateHandler != null && notificationUpdateRunnable != null) {
            notificationUpdateHandler.removeCallbacks(notificationUpdateRunnable);
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}