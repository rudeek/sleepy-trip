package com.example.sleepytrip;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class AlarmActivity extends AppCompatActivity {

    private Ringtone ringtone;
    private Vibrator vibrator;
    private AppDatabase db;

    private String locationName;
    private String locationAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // КРИТИЧНО: Настраиваем окно ДО setContentView
        setupWindowFlags();

        setContentView(R.layout.activity_alarm);

        // Инициализируем БД
        db = AppDatabase.getInstance(this);

        // Получаем данные о локации
        locationName = getIntent().getStringExtra("location_name");
        locationAddress = getIntent().getStringExtra("location_address");

        // Находим элементы
        TextView tvAlarmTitle = findViewById(R.id.tv_alarm_title);
        TextView tvAlarmMessage = findViewById(R.id.tv_alarm_message);
        Button btnStopAlarm = findViewById(R.id.btn_stop_alarm);

        // Устанавливаем текст
        tvAlarmTitle.setText("🔔 Вы прибыли!");
        tvAlarmMessage.setText(locationName + "\n" + locationAddress);

        // Запускаем рингтон и вибрацию
        playAlarmSound();
        startVibration();

        // Кнопка остановки будильника
        btnStopAlarm.setOnClickListener(v -> {
            stopAlarmSound();
            stopVibration();

            // Отключаем локацию в БД
            disableLocation();

            // Проверяем, нужно ли остановить сервис
            checkAndStopService();

            finish();
        });
    }

    // Настройка флагов окна для показа поверх экрана блокировки
    private void setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Для Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    // Отключаем локацию в БД по имени и адресу
    private void disableLocation() {
        new Thread(() -> {
            try {
                android.util.Log.d("AlarmActivity", "🔍 Начинаем поиск локации: " + locationName);

                // Ищем локацию по имени и адресу
                List<Location> allLocations = db.locationDao().getAllLocations();
                android.util.Log.d("AlarmActivity", "📋 Всего локаций в БД: " + allLocations.size());

                Location targetLocation = null;
                for (Location location : allLocations) {
                    android.util.Log.d("AlarmActivity", "🔎 Проверяем: " + location.getName() +
                            " (ID: " + location.getId() + ", Active: " + location.isActive() + ")");

                    if (location.getName().equals(locationName) &&
                            location.getAddress().equals(locationAddress)) {
                        targetLocation = location;
                        android.util.Log.d("AlarmActivity", "✅ Нашли целевую локацию! ID: " + location.getId());
                        break;
                    }
                }

                if (targetLocation != null) {
                    android.util.Log.d("AlarmActivity", "🔧 Выключаем локацию ID: " + targetLocation.getId());

                    // Выключаем локацию
                    targetLocation.setActive(false);
                    db.locationDao().update(targetLocation);

                    android.util.Log.d("AlarmActivity", "✅ Локация обновлена в БД");

                    // ВАЖНО: Уведомляем LocationService о выключении
                    final int locationId = targetLocation.getId();
                    runOnUiThread(() -> {
                        Intent intent = new Intent("com.example.sleepytrip.LOCATION_DISABLED");
                        intent.putExtra("location_id", locationId);
                        sendBroadcast(intent);
                        android.util.Log.d("AlarmActivity", "📡 Отправлен broadcast для ID: " + locationId);
                    });

                    // Показываем toast на главном потоке
                    final String name = targetLocation.getName();
                    runOnUiThread(() -> {
                        Toast.makeText(AlarmActivity.this,
                                "Будильник для \"" + name + "\" выключен",
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    android.util.Log.e("AlarmActivity", "❌ Локация НЕ НАЙДЕНА!");
                }
            } catch (Exception e) {
                android.util.Log.e("AlarmActivity", "❌ ОШИБКА: " + e.getMessage(), e);
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(AlarmActivity.this,
                            "Ошибка при отключении локации",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Проверяем, нужно ли остановить сервис
    private void checkAndStopService() {
        new Thread(() -> {
            try {
                android.util.Log.d("AlarmActivity", "🔍 Проверяем активные локации...");

                List<Location> locations = db.locationDao().getAllLocations();
                android.util.Log.d("AlarmActivity", "📋 Всего локаций: " + locations.size());

                boolean hasActiveLocation = false;
                for (Location location : locations) {
                    android.util.Log.d("AlarmActivity", "  - " + location.getName() +
                            ": Active=" + location.isActive());
                    if (location.isActive()) {
                        hasActiveLocation = true;
                        break;
                    }
                }

                android.util.Log.d("AlarmActivity", "🎯 Есть активные локации? " + hasActiveLocation);

                if (!hasActiveLocation) {
                    android.util.Log.d("AlarmActivity", "🛑 Останавливаем LocationService");

                    // Останавливаем сервис если нет активных локаций
                    Intent serviceIntent = new Intent(AlarmActivity.this, LocationService.class);
                    stopService(serviceIntent);

                    runOnUiThread(() -> {
                        Toast.makeText(AlarmActivity.this,
                                "Отслеживание локаций остановлено",
                                Toast.LENGTH_SHORT).show();
                    });
                } else {
                    android.util.Log.d("AlarmActivity", "✅ Сервис продолжает работать");
                }
            } catch (Exception e) {
                android.util.Log.e("AlarmActivity", "❌ ОШИБКА checkAndStopService: " + e.getMessage(), e);
                e.printStackTrace();
            }
        }).start();
    }

    private void playAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 200, 500, 200, 500};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    // В AlarmActivity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        int locationId = getIntent().getIntExtra("LOCATION_ID", -1);
        if (locationId != -1) {
            Log.d("AlarmActivity", "🧹 onDestroy: сбрасываю локацию " + locationId);
            Intent resetIntent = new Intent("LOCATION_RESET");
            resetIntent.setPackage(getPackageName());
            resetIntent.putExtra("LOCATION_ID", locationId);
            sendBroadcast(resetIntent);
        }
    }



    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Блокируем кнопку назад
    }
}