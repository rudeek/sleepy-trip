package com.example.sleepytrip;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class SettingsFragment extends Fragment {

    private LinearLayout settingUnits;
    private LinearLayout settingTheme;
    private LinearLayout settingLanguage;
    private LinearLayout settingDefaultPerimeter;
    private LinearLayout settingLocationFrequency;
    private LinearLayout settingSortingAlarms;
    private LinearLayout settingColorTheme;
    private LinearLayout settingRateApp;

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "SleepyTripSettings";
    private static final String KEY_UNITS = "units";
    private static final String UNITS_KM = "km";
    private static final String UNITS_MILES = "miles";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Инициализируем SharedPreferences
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Находим все элементы настроек
        settingUnits = view.findViewById(R.id.setting_units);
        settingTheme = view.findViewById(R.id.setting_theme);
        settingLanguage = view.findViewById(R.id.setting_language);
        settingDefaultPerimeter = view.findViewById(R.id.setting_default_perimeter);
        settingLocationFrequency = view.findViewById(R.id.setting_location_frequency);
        settingSortingAlarms = view.findViewById(R.id.setting_sorting_alarms);
        settingColorTheme = view.findViewById(R.id.setting_color_theme);
        settingRateApp = view.findViewById(R.id.setting_rate_app);

        // === ОБРАБОТЧИКИ КЛИКОВ ===

        // Units - выбор единиц измерения (км/мили)
        settingUnits.setOnClickListener(v -> showUnitsBottomSheet());

        // Theme - выбор темы приложения
        settingTheme.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Theme: светлая/тёмная тема",
                    Toast.LENGTH_SHORT).show();
        });

        // Language - выбор языка
        settingLanguage.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Language: выбор языка приложения",
                    Toast.LENGTH_SHORT).show();
        });

        // Default alarm perimeter - радиус будильника по умолчанию
        settingDefaultPerimeter.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Default perimeter: радиус по умолчанию",
                    Toast.LENGTH_SHORT).show();
        });

        // Location update frequency - частота обновления геолокации
        settingLocationFrequency.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Frequency: частота обновления GPS",
                    Toast.LENGTH_SHORT).show();
        });

        // Sorting alarms - сортировка будильников
        settingSortingAlarms.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Sorting: по дате/имени/расстоянию",
                    Toast.LENGTH_SHORT).show();
        });

        // Color theme - цветовая тема
        settingColorTheme.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Color theme: выбор цветовой схемы",
                    Toast.LENGTH_SHORT).show();
        });

        // Rate app - оценить приложение
        settingRateApp.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Rate app: открыть Google Play",
                    Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    // === МЕТОД ДЛЯ ПОКАЗА КРАСИВОГО BOTTOMSHEET ===
    private void showUnitsBottomSheet() {
        // Создаём BottomSheetDialog
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());

        // Инфлейтим наш красивый layout
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_units, null);

        bottomSheet.setContentView(sheetView);

        // Находим элементы
        androidx.cardview.widget.CardView optionKilometers = sheetView.findViewById(R.id.option_kilometers);
        androidx.cardview.widget.CardView optionMiles = sheetView.findViewById(R.id.option_miles);
        ImageView checkKilometers = sheetView.findViewById(R.id.check_kilometers);
        ImageView checkMiles = sheetView.findViewById(R.id.check_miles);

        // Получаем текущие настройки
        String currentUnits = prefs.getString(KEY_UNITS, UNITS_KM);

        // Устанавливаем галочки
        if (UNITS_KM.equals(currentUnits)) {
            checkKilometers.setVisibility(View.VISIBLE);
            checkMiles.setVisibility(View.GONE);
        } else {
            checkKilometers.setVisibility(View.GONE);
            checkMiles.setVisibility(View.VISIBLE);
        }

        // === КЛИК НА КИЛОМЕТРЫ ===
        optionKilometers.setOnClickListener(v -> {
            // Сохраняем выбор
            prefs.edit().putString(KEY_UNITS, UNITS_KM).apply();

            // Показываем уведомление
            Toast.makeText(requireContext(),
                    "✅ Kilometers selected",
                    Toast.LENGTH_SHORT).show();

            // Закрываем BottomSheet
            bottomSheet.dismiss();
        });

        // === КЛИК НА МИЛИ ===
        optionMiles.setOnClickListener(v -> {
            // Сохраняем выбор
            prefs.edit().putString(KEY_UNITS, UNITS_MILES).apply();

            // Показываем уведомление
            Toast.makeText(requireContext(),
                    "✅ Miles selected",
                    Toast.LENGTH_SHORT).show();

            // Закрываем BottomSheet
            bottomSheet.dismiss();
        });

        // Показываем BottomSheet
        bottomSheet.show();
    }

    // === ПУБЛИЧНЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ ЕДИНИЦ ===
    public static String getUnits(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_UNITS, UNITS_KM);
    }

    // === МЕТОД ДЛЯ ФОРМАТИРОВАНИЯ РАССТОЯНИЙ ===
    public static String formatDistance(Context context, float meters) {
        String units = getUnits(context);

        if (UNITS_MILES.equals(units)) {
            // Мили/футы
            float miles = meters * 0.000621371f;

            if (miles >= 1) {
                return String.format("%.1f mi", miles);
            } else {
                float feet = meters * 3.28084f;
                return String.format("%.0f ft", feet);
            }
        } else {
            // Километры/метры
            if (meters >= 1000) {
                return String.format("%.1f km", meters / 1000);
            } else {
                return String.format("%.0f m", meters);
            }
        }
    }
}