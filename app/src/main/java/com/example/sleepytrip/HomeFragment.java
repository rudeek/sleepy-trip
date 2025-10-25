package com.example.sleepytrip;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyMessage;
    private LocationAdapter adapter;
    private AppDatabase db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Инфлейтим layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Находим элементы
        recyclerView = view.findViewById(R.id.recycler_locations);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);

        // Получаем database
        db = AppDatabase.getInstance(requireContext());

        // Настраиваем RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Создаём adapter с обработчиком удаления
        adapter = new LocationAdapter(location -> {
            // Удаляем локацию из базы данных
            db.locationDao().delete(location);

            // Показываем уведомление
            Toast.makeText(requireContext(),
                    "Локация удалена",
                    Toast.LENGTH_SHORT).show();

            // Обновляем список
            loadLocations();
        });

        recyclerView.setAdapter(adapter);

        // Загружаем локации
        loadLocations();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем список при возврате на фрагмент
        loadLocations();
    }

    // Загрузить локации из базы данных
    private void loadLocations() {
        // Получаем все локации
        List<Location> locations = db.locationDao().getAllLocations();

        // Проверяем пустой ли список
        if (locations.isEmpty()) {
            // Показываем сообщение о пустом списке
            tvEmptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            // Показываем список
            tvEmptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            // Обновляем данные в adapter
            adapter.setLocations(locations);
        }
    }
}