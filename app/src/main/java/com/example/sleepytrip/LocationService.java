package com.example.sleepytrip;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final String ALARM_CHANNEL_ID = "AlarmChannel";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AppDatabase db;
    private PowerManager.WakeLock wakeLock;

    private Location currentUserLocation;

    private Handler notificationUpdateHandler;
    private Runnable notificationUpdateRunnable;

    private static final Map<Integer, Boolean> triggeredAlarms = new ConcurrentHashMap<>();

    private Ringtone alarmRingtone;
    private Vibrator alarmVibrator;

    private final BroadcastReceiver resetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("LOCATION_RESET".equals(intent.getAction())) {
                int locationId = intent.getIntExtra("LOCATION_ID", -1);

                if (locationId != -1) {
                    Boolean wasRemoved = triggeredAlarms.remove(locationId);

                    if (wasRemoved != null) {
                        Log.d("LocationService", "✅ Сброшен статус локации ID=" + locationId);
                    } else {
                        Log.w("LocationService", "⚠️ Локация ID=" + locationId + " не найдена в triggeredAlarms");
                    }

                    Log.d("LocationService", "📊 Активных будильников: " + triggeredAlarms.size());
                } else {
                    Log.e("LocationService", "❌ LOCATION_ID не передан в broadcast!");
                }
            }
        }
    };

    private final BroadcastReceiver stopAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("STOP_ALARM".equals(intent.getAction())) {
                Log.d("LocationService", "🔇 Получен сигнал остановки будильника");
                stopAlarmSoundAndVibrate();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter("LOCATION_RESET");
        registerReceiver(resetReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        IntentFilter stopAlarmFilter = new IntentFilter("STOP_ALARM");
        registerReceiver(stopAlarmReceiver, stopAlarmFilter, Context.RECEIVER_NOT_EXPORTED);

        Log.d("LocationService", "✅ BroadcastReceivers зарегистрированы");

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
            startForeground(1, createNotification("Отслеживание локаций", "Запуск..."));
        } catch (Exception e) {
            Log.e("LocationService", "Foreground start failed", e);
        }

        startLocationTracking();
        startPeriodicNotificationUpdates();
    }

    private void startPeriodicNotificationUpdates() {
        notificationUpdateHandler = new Handler(Looper.getMainLooper());

        notificationUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentUserLocation != null) {
                    updateNotificationWithDistance(currentUserLocation);
                    Log.d("LocationService", "📍 Notification обновлен");
                }

                notificationUpdateHandler.postDelayed(this, 10000);
            }
        };

        notificationUpdateHandler.postDelayed(notificationUpdateRunnable, 3000);
    }

    private void startLocationTracking() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000
        )
                .setMinUpdateIntervalMillis(5000)
                .setWaitForAccurateLocation(false)
                .setMaxUpdateDelayMillis(10000)
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
            Log.d("LocationService", "⚠️ Нет активных локаций - notification скрыт");

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(1, createMinimalNotification());
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
        List<com.example.sleepytrip.Location> allLocations = db.locationDao().getAllLocations();
        for (com.example.sleepytrip.Location location : allLocations) {
            if (!location.isActive() && triggeredAlarms.containsKey(location.getId())) {
                Log.d("LocationService", "🔄 Сбрасываем флаг для неактивной локации ID: " + location.getId());
                triggeredAlarms.remove(location.getId());
            }
        }

        List<com.example.sleepytrip.Location> activeLocations = db.locationDao().getActiveLocations();

        Log.d("LocationService", "🔍 Проверка близости: активных локаций = " + activeLocations.size());

        for (com.example.sleepytrip.Location savedLocation : activeLocations) {
            Log.d("LocationService", "📍 Проверяем локацию: " + savedLocation.getName() +
                    " (ID: " + savedLocation.getId() + ", Active: " + savedLocation.isActive() + ")");

            Boolean hasTriggered = triggeredAlarms.get(savedLocation.getId());
            Log.d("LocationService", "  ⏰ Будильник уже срабатывал? " + hasTriggered);

            if (hasTriggered != null && hasTriggered) {
                Log.d("LocationService", "  ⏭️ Будильник уже срабатывал, пропускаем");
                continue;
            }

            float[] results = new float[1];
            Location.distanceBetween(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    savedLocation.getLatitude(),
                    savedLocation.getLongitude(),
                    results
            );

            float distance = results[0];
            Log.d("LocationService", "  📏 Расстояние: " + distance + "м, Радиус: " + savedLocation.getRadius() + "м");

            if (distance <= savedLocation.getRadius()) {
                Log.w("LocationService",
                        "🚨 БУДИЛЬНИК СРАБОТАЛ! Дистанция: " + distance + "м, Радиус: " + savedLocation.getRadius() + "м");

                triggeredAlarms.put(savedLocation.getId(), true);
                Log.d("LocationService", "✅ ID " + savedLocation.getId() + " добавлен в triggeredAlarms");

                showAlarmActivity(savedLocation);
            }
        }
    }

    // ⭐ ИСПРАВЛЕННЫЙ МЕТОД: Определяем состояние экрана и запускаем Activity
    private void showAlarmActivity(com.example.sleepytrip.Location location) {
        try {
            playAlarmSoundAndVibrate();

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            boolean isScreenOn = powerManager.isInteractive();
            Log.d("LocationService", "📱 Экран включён: " + isScreenOn);

            Intent alarmIntent = new Intent(this, AlarmActivity.class);
            alarmIntent.putExtra("location_name", location.getName());
            alarmIntent.putExtra("location_address", location.getAddress());
            alarmIntent.putExtra("location_id", location.getId());

            if (isScreenOn) {
                // ⭐ ЭКРАН ВКЛЮЧЁН - запускаем Activity напрямую
                Log.d("LocationService", "🔓 Экран включён, запускаем Activity напрямую");

                // Проверяем разрешение на показ поверх других окон (для Android 10+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !Settings.canDrawOverlays(this)) {
                    Log.w("LocationService", "⚠️ Нет разрешения SYSTEM_ALERT_WINDOW");
                }

                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);

                startActivity(alarmIntent);
                Log.d("LocationService", "✅ Activity запущена напрямую");

            } else {
                // ⭐ ЭКРАН ВЫКЛЮЧЁН - используем Full Screen Intent
                Log.d("LocationService", "🔒 Экран выключен, используем Full Screen Intent");

                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                        Intent.FLAG_ACTIVITY_NO_HISTORY);

                PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                        this,
                        10000 + location.getId(),
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle("🚨 Будильник")
                        .setContentText("Вы прибыли: " + location.getName())
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setAutoCancel(true)
                        .setOngoing(false)
                        .setFullScreenIntent(fullScreenPendingIntent, true)
                        .setContentIntent(fullScreenPendingIntent)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    int notificationId = 10000 + location.getId();
                    notificationManager.notify(notificationId, builder.build());
                    Log.d("LocationService", "✅ Full Screen Intent отправлен");

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        notificationManager.cancel(notificationId);
                    }, 2000);
                }
            }

        } catch (Exception e) {
            Log.e("LocationService", "❌ Ошибка запуска будильника: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void playAlarmSoundAndVibrate() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            alarmRingtone = RingtoneManager.getRingtone(this, alarmUri);
            if (alarmRingtone != null) {
                alarmRingtone.play();
                Log.d("LocationService", "🔊 Звук будильника запущен");
            }
        } catch (Exception e) {
            Log.e("LocationService", "❌ Ошибка воспроизведения звука: " + e.getMessage());
        }

        try {
            alarmVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (alarmVibrator != null && alarmVibrator.hasVibrator()) {
                long[] pattern = {0, 500, 200, 500, 200, 500};

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    alarmVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    alarmVibrator.vibrate(pattern, 0);
                }
                Log.d("LocationService", "📳 Вибрация запущена");
            }
        } catch (Exception e) {
            Log.e("LocationService", "❌ Ошибка вибрации: " + e.getMessage());
        }
    }

    public void stopAlarmSoundAndVibrate() {
        if (alarmRingtone != null && alarmRingtone.isPlaying()) {
            alarmRingtone.stop();
            alarmRingtone = null;
            Log.d("LocationService", "🔇 Звук остановлен");
        }

        if (alarmVibrator != null) {
            alarmVibrator.cancel();
            alarmVibrator = null;
            Log.d("LocationService", "📴 Вибрация остановлена");
        }
    }

    private Notification createMinimalNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SleepyTrip")
                .setContentText("Ожидание активных локаций")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSound(null)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
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
                .setOngoing(true)
                .setSound(null)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Отслеживание локаций",
                        NotificationManager.IMPORTANCE_LOW
                );
                serviceChannel.setDescription("Показывает расстояние до локаций");
                serviceChannel.setSound(null, null);
                serviceChannel.enableVibration(false);
                serviceChannel.setShowBadge(false);
                manager.createNotificationChannel(serviceChannel);

                NotificationChannel alarmChannel = new NotificationChannel(
                        ALARM_CHANNEL_ID,
                        "Будильник прибытия",
                        NotificationManager.IMPORTANCE_HIGH
                );
                alarmChannel.setDescription("Срабатывает при прибытии к месту назначения");
                alarmChannel.enableVibration(false);
                alarmChannel.setSound(null, null);
                alarmChannel.setShowBadge(false);
                alarmChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                manager.createNotificationChannel(alarmChannel);

                Log.d("LocationService", "✅ Notification channels созданы");
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

        stopAlarmSoundAndVibrate();

        try {
            unregisterReceiver(resetReceiver);
            unregisterReceiver(stopAlarmReceiver);
            Log.d("LocationService", "✅ BroadcastReceivers отписаны");
        } catch (IllegalArgumentException e) {
            Log.w("LocationService", "⚠️ Receivers уже были отписаны");
        }

        if (notificationUpdateHandler != null && notificationUpdateRunnable != null) {
            notificationUpdateHandler.removeCallbacks(notificationUpdateRunnable);
            Log.d("LocationService", "✅ Notification updates остановлены");
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d("LocationService", "✅ Location updates остановлены");
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d("LocationService", "✅ WakeLock освобождён");
        }

        triggeredAlarms.clear();

        Log.d("LocationService", "🛑 LocationService полностью уничтожен");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}