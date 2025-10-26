package com.example.sleepytrip;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
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

    // Launcher для запроса разрешений
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Регистрируем launcher для запроса разрешений
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocation = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean backgroundLocation = result.get(Manifest.permission.ACCESS_BACKGROUND_LOCATION);

                    if (fineLocation != null && fineLocation) {
                        // Разрешение получено, запускаем сервис
                        startLocationService();
                    } else {
                        Toast.makeText(requireContext(),
                                "Разрешение на геолокацию необходимо для работы будильника",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

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

        // Создаём adapter с обработчиком переключения switch
        adapter = new LocationAdapter((location, isChecked) -> {
            // Обновляем статус локации в базе данных
            location.setActive(isChecked);
            db.locationDao().update(location);

            if (isChecked) {
                // Локация включена
                Toast.makeText(requireContext(),
                        "Будильник для \"" + location.getName() + "\" включен",
                        Toast.LENGTH_SHORT).show();

                // Проверяем разрешения и запускаем сервис
                checkPermissionsAndStartService();
            } else {
                // Локация выключена
                Toast.makeText(requireContext(),
                        "Будильник для \"" + location.getName() + "\" выключен",
                        Toast.LENGTH_SHORT).show();

                // Проверяем есть ли ещё активные локации
                checkAndStopService();
            }
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

    // Проверяем разрешения и запускаем сервис
    private void checkPermissionsAndStartService() {
        // Проверяем разрешение на точную геолокацию
        boolean hasFineLocation = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Для Android 10+ нужно разрешение на фоновую геолокацию
        boolean hasBackgroundLocation = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasBackgroundLocation = ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        if (hasFineLocation && hasBackgroundLocation) {
            // Все разрешения есть - запускаем сервис
            startLocationService();
        } else {
            // Запрашиваем разрешения
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                });
            } else {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                });
            }
        }
    }

    // Запустить сервис отслеживания локации
    private void startLocationService() {
        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
    }

    // Проверяем есть ли активные локации и останавливаем сервис если нет
    private void checkAndStopService() {
        List<Location> locations = db.locationDao().getAllLocations();

        // Проверяем есть ли хоть одна активная локация
        boolean hasActiveLocation = false;
        for (Location location : locations) {
            if (location.isActive()) {
                hasActiveLocation = true;
                break;
            }
        }

        // Если нет активных локаций - останавливаем сервис
        if (!hasActiveLocation) {
            Intent serviceIntent = new Intent(requireContext(), LocationService.class);
            requireContext().stopService(serviceIntent);

            Toast.makeText(requireContext(),
                    "Отслеживание локаций остановлено",
                    Toast.LENGTH_SHORT).show();
        }
    }
}