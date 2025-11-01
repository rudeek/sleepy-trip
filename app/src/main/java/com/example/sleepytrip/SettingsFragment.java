package com.example.sleepytrip;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

public class SettingsFragment extends Fragment {

    private LinearLayout settingUnits;
    private LinearLayout settingTheme;
    private LinearLayout settingLanguage;
    private LinearLayout settingDefaultPerimeter;
    private LinearLayout settingLocationFrequency;
    private LinearLayout settingSortingAlarms;
    private LinearLayout settingColorTheme;
    private LinearLayout settingRateApp;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

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
        settingUnits.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Units: выбор километры/мили",
                    Toast.LENGTH_SHORT).show();
            // TODO: Открыть диалог выбора единиц измерения
        });

        // Theme - выбор темы приложения
        settingTheme.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Theme: светлая/тёмная тема",
                    Toast.LENGTH_SHORT).show();
            // TODO: Открыть диалог выбора темы
        });

        // Language - выбор языка
        settingLanguage.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Language: выбор языка приложения",
                    Toast.LENGTH_SHORT).show();
            // TODO: Открыть диалог выбора языка (English/Русский/Română)
        });

        // Default alarm perimeter - радиус будильника по умолчанию
        settingDefaultPerimeter.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Default perimeter: радиус по умолчанию",
                    Toast.LENGTH_SHORT).show();
            // TODO: Открыть диалог выбора радиуса
        });

        // Location update frequency - частота обновления геолокации
        settingLocationFrequency.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Frequency: частота обновления GPS",
                    Toast.LENGTH_SHORT).show();
            // TODO: Открыть диалог выбора частоты (5с/10с/30с)
        });

        // Sorting alarms - сортировка будильников
        settingSortingAlarms.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Sorting: по дате/имени/расстоянию",
                    Toast.LENGTH_SHORT).show();
            // TODO: Открыть диалог выбора сортировки
        });

        // Color theme - цветовая тема
        settingColorTheme.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Color theme: выбор цветовой схемы",
                    Toast.LENGTH_SHORT).show();
            // TODO: Открыть диалог выбора цвета
        });

        // Rate app - оценить приложение
        settingRateApp.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Rate app: открыть Google Play",
                    Toast.LENGTH_SHORT).show();
            // TODO: Открыть приложение в Google Play для оценки
        });

        return view;
    }
}