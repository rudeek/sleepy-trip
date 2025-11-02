package com.example.sleepytrip;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
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

        // Запускаем рингтон и вибрацию (дополнительно, на случай если сервис не запустил)
        playAlarmSound();
        startVibration();

        // Кнопка остановки будильника
        btnStopAlarm.setOnClickListener(v -> {
            // ⭐ Останавливаем звук и вибрацию В СЕРВИСЕ
            stopServiceAlarm();

            stopAlarmSound();
            stopVibration();

            // ⭐ ВАЖНО: Отключаем локацию И проверяем сервис в ОДНОМ потоке
            disableLocationAndCheckService();
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

    // ⭐ НОВЫЙ МЕТОД: Останавливаем звук и вибрацию в сервисе
    private void stopServiceAlarm() {
        try {
            Intent stopIntent = new Intent("STOP_ALARM");
            stopIntent.setPackage(getPackageName());
            sendBroadcast(stopIntent);
            Log.d("AlarmActivity", "📡 Отправлен broadcast STOP_ALARM");
        } catch (Exception e) {
            Log.e("AlarmActivity", "❌ Ошибка отправки broadcast: " + e.getMessage());
        }
    }

    // ⭐ ОБЪЕДИНЕННЫЙ МЕТОД: Отключаем локацию И проверяем сервис
    private void disableLocationAndCheckService() {
        new Thread(() -> {
            try {
                // Получаем ID из Intent
                final int locationId = getIntent().getIntExtra("location_id", -1);

                if (locationId == -1) {
                    Log.e("AlarmActivity", "❌ location_id не найден в Intent!");
                    runOnUiThread(() -> {
                        Toast.makeText(AlarmActivity.this,
                                "Ошибка: ID локации не найден",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                Log.d("AlarmActivity", "🔍 Отключаем локацию ID=" + locationId);

                // ШАГ 1: Получаем и выключаем локацию
                com.example.sleepytrip.Location targetLocation = db.locationDao().getLocationById(locationId);

                if (targetLocation == null) {
                    Log.e("AlarmActivity", "❌ Локация ID=" + locationId + " НЕ НАЙДЕНА в БД!");
                    runOnUiThread(() -> {
                        Toast.makeText(AlarmActivity.this,
                                "Ошибка: локация не найдена",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                Log.d("AlarmActivity", "✅ Найдена локация: " + targetLocation.getName());

                // Выключаем локацию
                targetLocation.setActive(false);
                db.locationDao().update(targetLocation);

                Log.d("AlarmActivity", "✅ Локация обновлена в БД (Active=false)");

                final String locationName = targetLocation.getName();

                // ШАГ 2: Проверяем есть ли ещё активные локации
                List<Location> allLocations = db.locationDao().getAllLocations();
                Log.d("AlarmActivity", "📋 Всего локаций в БД: " + allLocations.size());

                boolean hasActiveLocation = false;
                for (Location location : allLocations) {
                    Log.d("AlarmActivity", "  - " + location.getName() +
                            " (ID=" + location.getId() + ", Active=" + location.isActive() + ")");
                    if (location.isActive()) {
                        hasActiveLocation = true;
                    }
                }

                Log.d("AlarmActivity", "🎯 Результат проверки: hasActiveLocation = " + hasActiveLocation);

                final boolean shouldStopService = !hasActiveLocation;

                // ШАГ 3: Показываем уведомления и останавливаем сервис если нужно
                runOnUiThread(() -> {
                    // Первое уведомление - о выключении локации
                    Toast.makeText(AlarmActivity.this,
                            "Будильник для \"" + locationName + "\" выключен",
                            Toast.LENGTH_SHORT).show();

                    if (shouldStopService) {
                        Log.d("AlarmActivity", "🛑 Останавливаем LocationService");

                        // Останавливаем сервис
                        Intent serviceIntent = new Intent(AlarmActivity.this, LocationService.class);
                        boolean stopped = stopService(serviceIntent);

                        Log.d("AlarmActivity", "📡 stopService() вернул: " + stopped);

                        // Второе уведомление - об остановке сервиса
                        Toast.makeText(AlarmActivity.this,
                                "Отслеживание локаций остановлено",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Log.d("AlarmActivity", "✅ Сервис продолжает работать (есть активные локации)");
                    }

                    // Закрываем активность
                    finish();
                });

            } catch (Exception e) {
                Log.e("AlarmActivity", "❌ ОШИБКА disableLocationAndCheckService: " + e.getMessage(), e);
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(AlarmActivity.this,
                            "Ошибка при отключении локации",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ⭐ КРИТИЧНО: Останавливаем звук и вибрацию при закрытии
        stopAlarmSound();
        stopVibration();

        // Получаем ID локации из Intent
        int locationId = getIntent().getIntExtra("location_id", -1);

        if (locationId != -1) {
            Log.d("AlarmActivity", "🧹 onDestroy: сбрасываю локацию ID=" + locationId);

            // Отправляем broadcast для очистки triggeredAlarms
            Intent resetIntent = new Intent("LOCATION_RESET");
            resetIntent.setPackage(getPackageName());
            resetIntent.putExtra("LOCATION_ID", locationId);
            sendBroadcast(resetIntent);

            Log.d("AlarmActivity", "📡 Broadcast LOCATION_RESET отправлен для ID=" + locationId);
        } else {
            Log.w("AlarmActivity", "⚠️ location_id не найден в Intent!");
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Блокируем кнопку назад
    }
}