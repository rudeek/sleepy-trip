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

        //применяет сохранённый язык интерфейса
        String savedLanguage = SettingsFragment.getCurrentLanguage(this);
        SettingsFragment.setLocale(this, savedLanguage);

        //настраивает флаги окна для отображения будильника поверх экрана
        setupWindowFlags();

        setContentView(R.layout.activity_alarm);

        //инициализация базы данных
        db = AppDatabase.getInstance(this);

        //получаем данные локации из intent
        locationName = getIntent().getStringExtra("location_name");
        locationAddress = getIntent().getStringExtra("location_address");

        TextView tvAlarmTitle = findViewById(R.id.tv_alarm_title);
        TextView tvAlarmMessage = findViewById(R.id.tv_alarm_message);
        Button btnStopAlarm = findViewById(R.id.btn_stop_alarm);

        //устанавливает заголовок и сообщение будильника
        tvAlarmTitle.setText(getString(R.string.alarm_title));
        tvAlarmMessage.setText(locationName + "\n" + locationAddress);

        //запускает звук и вибрацию будильника
        playAlarmSound();
        startVibration();

        //обработчик кнопки "остановить"
        btnStopAlarm.setOnClickListener(v -> {
            stopServiceAlarm();
            stopAlarmSound();
            stopVibration();
            disableLocationAndCheckService();
            finish();
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        //применяет язык до создания контекста активности
        String savedLanguage = newBase.getSharedPreferences("SleepyTripSettings", Context.MODE_PRIVATE)
                .getString("language", "en");

        java.util.Locale locale = new java.util.Locale(savedLanguage);
        java.util.Locale.setDefault(locale);

        android.content.res.Configuration config = new android.content.res.Configuration(newBase.getResources().getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            Context context = newBase.createConfigurationContext(config);
            super.attachBaseContext(context);
        } else {
            config.locale = locale;
            newBase.getResources().updateConfiguration(config, newBase.getResources().getDisplayMetrics());
            super.attachBaseContext(newBase);
        }
    }

    private void setupWindowFlags() {
        //разрешает будильнику отображаться поверх заблокированного экрана
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

        //для экранов с вырезами (notch)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }

    private void stopServiceAlarm() {
        //отправляет broadcast для остановки сервиса будильника
        try {
            Intent stopIntent = new Intent("STOP_ALARM");
            stopIntent.setPackage(getPackageName());
            sendBroadcast(stopIntent);
            Log.d("AlarmActivity", "📡 Отправлен broadcast STOP_ALARM");
        } catch (Exception e) {
            Log.e("AlarmActivity", "❌ Ошибка отправки broadcast: " + e.getMessage());
        }
    }

    private void disableLocationAndCheckService() {
        //отключает сработавшую локацию и проверяет, нужно ли остановить сервис
        new Thread(() -> {
            try {
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

                com.example.sleepytrip.Location targetLocation = db.locationDao().getLocationById(locationId);

                if (targetLocation == null) {
                    Log.e("AlarmActivity", "❌ Локация не найдена в БД!");
                    runOnUiThread(() -> {
                        Toast.makeText(AlarmActivity.this,
                                "Ошибка: локация не найдена",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                //отключает локацию и обновляет запись в базе
                targetLocation.setActive(false);
                db.locationDao().update(targetLocation);

                final String locationName = targetLocation.getName();

                //проверяет наличие активных локаций
                List<Location> allLocations = db.locationDao().getAllLocations();
                boolean hasActiveLocation = false;
                for (Location location : allLocations) {
                    if (location.isActive()) hasActiveLocation = true;
                }

                final boolean shouldStopService = !hasActiveLocation;

                //показывает уведомления и при необходимости останавливает сервис
                runOnUiThread(() -> {
                    Toast.makeText(AlarmActivity.this,
                            "Будильник для \"" + locationName + "\" выключен",
                            Toast.LENGTH_SHORT).show();

                    if (shouldStopService) {
                        Intent serviceIntent = new Intent(AlarmActivity.this, LocationService.class);
                        stopService(serviceIntent);
                        Toast.makeText(AlarmActivity.this,
                                "Отслеживание локаций остановлено",
                                Toast.LENGTH_LONG).show();
                    }

                    finish();
                });

            } catch (Exception e) {
                Log.e("AlarmActivity", "❌ Ошибка disableLocationAndCheckService: " + e.getMessage(), e);
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
        //воспроизводит звук будильника
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAlarmSound() {
        //останавливает звук будильника
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
    }

    private void startVibration() {
        //запускает вибрацию при срабатывании будильника
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
        //останавливает вибрацию
        if (vibrator != null) vibrator.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //останавливает звук и вибрацию перед уничтожением активности
        stopAlarmSound();
        stopVibration();

        //отправляет broadcast для сброса состояния локации
        int locationId = getIntent().getIntExtra("location_id", -1);
        if (locationId != -1) {
            Intent resetIntent = new Intent("LOCATION_RESET");
            resetIntent.setPackage(getPackageName());
            resetIntent.putExtra("LOCATION_ID", locationId);
            sendBroadcast(resetIntent);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        //блокирует кнопку "назад", чтобы нельзя было закрыть будильник
    }
}
