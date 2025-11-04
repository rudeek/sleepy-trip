package com.example.sleepytrip;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Locale;

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
    private static final String KEY_LANGUAGE = "language";
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
        settingLanguage = view.findViewById(R.id.setting_language);
        settingRateApp = view.findViewById(R.id.setting_rate_app);

        // === ОБРАБОТЧИКИ КЛИКОВ ===

        // Units - выбор единиц измерения (км/мили)
        settingUnits.setOnClickListener(v -> showUnitsBottomSheet());

        // Language - выбор языка
        settingLanguage.setOnClickListener(v -> showLanguageBottomSheet());



        // Rate app - оценить приложение
        settingRateApp.setOnClickListener(v -> {
            Toast.makeText(requireContext(),
                    "Rate app: открыть Google Play",
                    Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    // === МЕТОД ДЛЯ ПОКАЗА BOTTOMSHEET С ВЫБОРОМ ЕДИНИЦ ===
    private void showUnitsBottomSheet() {
        // ⭐ ИСПОЛЬЗУЕМ СТИЛЬ BottomSheetDialogTheme
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);

        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_units, null);

        bottomSheet.setContentView(sheetView);

        androidx.cardview.widget.CardView optionKilometers = sheetView.findViewById(R.id.option_kilometers);
        androidx.cardview.widget.CardView optionMiles = sheetView.findViewById(R.id.option_miles);
        ImageView checkKilometers = sheetView.findViewById(R.id.check_kilometers);
        ImageView checkMiles = sheetView.findViewById(R.id.check_miles);

        String currentUnits = prefs.getString(KEY_UNITS, UNITS_KM);

        if (UNITS_KM.equals(currentUnits)) {
            checkKilometers.setVisibility(View.VISIBLE);
            checkMiles.setVisibility(View.GONE);
        } else {
            checkKilometers.setVisibility(View.GONE);
            checkMiles.setVisibility(View.VISIBLE);
        }

        optionKilometers.setOnClickListener(v -> {
            prefs.edit().putString(KEY_UNITS, UNITS_KM).apply();
            Toast.makeText(requireContext(),
                    getString(R.string.units_changed_km),
                    Toast.LENGTH_SHORT).show();
            bottomSheet.dismiss();
        });

        optionMiles.setOnClickListener(v -> {
            prefs.edit().putString(KEY_UNITS, UNITS_MILES).apply();
            Toast.makeText(requireContext(),
                    getString(R.string.units_changed_miles),
                    Toast.LENGTH_SHORT).show();
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }

    // === МЕТОД ДЛЯ ПОКАЗА BOTTOMSHEET С ВЫБОРОМ ЯЗЫКА ===
    private void showLanguageBottomSheet() {
        // ⭐ ИСПОЛЬЗУЕМ СТИЛЬ BottomSheetDialogTheme
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);

        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_language, null);

        bottomSheet.setContentView(sheetView);

        androidx.cardview.widget.CardView optionEnglish = sheetView.findViewById(R.id.option_english);
        androidx.cardview.widget.CardView optionRussian = sheetView.findViewById(R.id.option_russian);
        androidx.cardview.widget.CardView optionRomanian = sheetView.findViewById(R.id.option_romanian);
        ImageView checkEnglish = sheetView.findViewById(R.id.check_english);
        ImageView checkRussian = sheetView.findViewById(R.id.check_russian);
        ImageView checkRomanian = sheetView.findViewById(R.id.check_romanian);

        String currentLanguage = prefs.getString(KEY_LANGUAGE, "en");

        // Устанавливаем галочки
        checkEnglish.setVisibility(currentLanguage.equals("en") ? View.VISIBLE : View.GONE);
        checkRussian.setVisibility(currentLanguage.equals("ru") ? View.VISIBLE : View.GONE);
        checkRomanian.setVisibility(currentLanguage.equals("ro") ? View.VISIBLE : View.GONE);

        // English
        optionEnglish.setOnClickListener(v -> {
            changeLanguage("en");
            bottomSheet.dismiss();
        });

        // Russian
        optionRussian.setOnClickListener(v -> {
            changeLanguage("ru");
            bottomSheet.dismiss();
        });

        // Romanian
        optionRomanian.setOnClickListener(v -> {
            changeLanguage("ro");
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }

    // === МЕТОД ДЛЯ СМЕНЫ ЯЗЫКА ===
    private void changeLanguage(String languageCode) {
        // Сохраняем выбранный язык
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();

        // Применяем язык
        setLocale(requireContext(), languageCode);

        // Показываем уведомление
        Toast.makeText(requireContext(),
                getString(R.string.language_changed),
                Toast.LENGTH_LONG).show();

        // Перезапускаем активность для применения изменений
        requireActivity().recreate();
    }

    // === МЕТОД ДЛЯ УСТАНОВКИ LOCALE ===
    public static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            context.createConfigurationContext(config);
        } else {
            config.locale = locale;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    // === ПУБЛИЧНЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ ТЕКУЩЕГО ЯЗЫКА ===
    public static String getCurrentLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    // === ПУБЛИЧНЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ ТЕКУЩИХ ЕДИНИЦ ИЗМЕРЕНИЯ ===
    public static String getUnits(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_UNITS, UNITS_KM);
    }

    // === МЕТОД ДЛЯ КОНВЕРТАЦИИ МЕТРОВ В ТЕКУЩИЕ ЕДИНИЦЫ ===
    public static String formatDistance(Context context, float meters) {
        String units = getUnits(context);

        if (UNITS_MILES.equals(units)) {
            float miles = meters * 0.000621371f;

            if (miles >= 1) {
                return String.format("%.1f mi", miles);
            } else {
                float feet = meters * 3.28084f;
                return String.format("%.0f ft", feet);
            }
        } else {
            if (meters >= 1000) {
                return String.format("%.1f km", meters / 1000);
            } else {
                return String.format("%.0f m", meters);
            }
        }
    }
}