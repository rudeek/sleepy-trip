package com.example.sleepytrip;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Просто возвращаем layout, всё остальное делает MainActivity
        return inflater.inflate(R.layout.fragment_about, container, false);
    }
}