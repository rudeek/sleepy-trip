package com.example.sleepytrip;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class UnitsBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String PREFS_NAME = "SleepyTripSettings";
    private static final String KEY_UNITS = "units";
    private static final String UNITS_KM = "km";
    private static final String UNITS_MILES = "miles";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_units, container, false);

        //получаем SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        //находим элементы
        LinearLayout optionKilometers = view.findViewById(R.id.option_kilometers);
        LinearLayout optionMiles = view.findViewById(R.id.option_miles);
        ImageView checkKilometers = view.findViewById(R.id.check_kilometers);
        ImageView checkMiles = view.findViewById(R.id.check_miles);

        //получаем текущие настройки
        String currentUnits = prefs.getString(KEY_UNITS, UNITS_KM);

        //устанавливаем галочки
        if (currentUnits.equals(UNITS_KM)) {
            checkKilometers.setVisibility(View.VISIBLE);
            checkMiles.setVisibility(View.GONE);
        } else {
            checkKilometers.setVisibility(View.GONE);
            checkMiles.setVisibility(View.VISIBLE);
        }

        //обработчик для километров
        optionKilometers.setOnClickListener(v -> {
            prefs.edit().putString(KEY_UNITS, UNITS_KM).apply();
            Toast.makeText(requireContext(), "✅ units changed to kilometers", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        //обработчик для миль
        optionMiles.setOnClickListener(v -> {
            prefs.edit().putString(KEY_UNITS, UNITS_MILES).apply();
            Toast.makeText(requireContext(), "✅ units changed to miles", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        return view;
    }
}
