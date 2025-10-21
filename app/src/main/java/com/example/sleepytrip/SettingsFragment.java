package com.example.sleepytrip;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Просто возвращаем layout, всё остальное делает MainActivity
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
}