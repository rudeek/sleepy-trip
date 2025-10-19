package com.example.sleepytrip;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class AddLocationFragment extends Fragment {

    private Button btnAddLocation;
    private Button btnCancel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_location, container, false);

        btnAddLocation = view.findViewById(R.id.btn_add_location);
        btnCancel = view.findViewById(R.id.btn_cancel);

        // Обработчик кнопки Cancel - возврат на HomeFragment
        btnCancel.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.replaceFragment(new HomeFragment(), true);
            }
        });

        // Обработчик кнопки Add Location
        btnAddLocation.setOnClickListener(v -> {
            // Здесь будет логика добавления локации
            // Пока просто возвращаемся на HomeFragment
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.replaceFragment(new HomeFragment(), true);
            }
        });

        return view;
    }
}