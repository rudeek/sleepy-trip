package com.example.sleepytrip;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyMessage;
    private Button btnCancelDelete;
    private Button btnConfirmDelete;

    private LocationAdapter adapter;
    private AppDatabase db;

    // Флаг режима удаления
    private boolean isDeleteMode = false;

    // Флаг "выбраны все"
    private boolean isAllSelected = false;

    // MenuItem для галочки
    private MenuItem selectAllItem;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Включаем меню в toolbar
        setHasOptionsMenu(true);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocation = result.get(Manifest.permission.ACCESS_FINE_LOCATION);

                    if (fineLocation != null && fineLocation) {
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
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Находим элементы
        recyclerView = view.findViewById(R.id.recycler_locations);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);

        // ⭐ Обновите эти строки
        View deleteModeBar = view.findViewById(R.id.delete_mode_bottom_bar);
        btnCancelDelete = view.findViewById(R.id.btn_cancel_delete);
        btnConfirmDelete = view.findViewById(R.id.btn_confirm_delete);

        db = AppDatabase.getInstance(requireContext());

        // Настраиваем RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new LocationAdapter((location, isChecked) -> {
            location.setActive(isChecked);
            db.locationDao().update(location);

            if (isChecked) {
                Toast.makeText(requireContext(),
                        "Будильник для \"" + location.getName() + "\" включен",
                        Toast.LENGTH_SHORT).show();
                checkPermissionsAndStartService();
            } else {
                Toast.makeText(requireContext(),
                        "Будильник для \"" + location.getName() + "\" выключен",
                        Toast.LENGTH_SHORT).show();
                checkAndStopService();
            }
        });

        recyclerView.setAdapter(adapter);

        adapter.setOnSelectionChangeListener((allSelected, noneSelected) -> {
            isAllSelected = allSelected;
            updateSelectAllIcon();
        });

        // === КНОПКА ОТМЕНЫ ===
        btnCancelDelete.setOnClickListener(v -> exitDeleteMode());

        // === КНОПКА УДАЛЕНИЯ ===
        btnConfirmDelete.setOnClickListener(v -> {
            List<Location> selectedLocations = adapter.getSelectedLocations();

            if (selectedLocations.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Выберите локации для удаления",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Удаляем выбранные локации
            for (Location location : selectedLocations) {
                db.locationDao().delete(location);
            }

            Toast.makeText(requireContext(),
                    "Удалено локаций: " + selectedLocations.size(),
                    Toast.LENGTH_SHORT).show();

            // Выходим из режима удаления
            exitDeleteMode();

            // Обновляем список
            loadLocations();
        });

        loadLocations();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.home_menu, menu);
        selectAllItem = menu.findItem(R.id.action_select_all);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLocations();
    }

    // Войти в режим удаления
    private void enterDeleteMode() {
        isDeleteMode = true;
        adapter.setDeleteMode(true);

        // Получаем ссылку на delete bar
        View deleteBar = getView().findViewById(R.id.delete_mode_bottom_bar);

        // Показываем delete bar с анимацией снизу вверх
        deleteBar.setVisibility(View.VISIBLE);
        deleteBar.setTranslationY(deleteBar.getHeight()); // Начальная позиция - за экраном
        deleteBar.animate()
                .translationY(0) // Конечная позиция - на месте
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Добавляем padding снизу для RecyclerView после анимации
        deleteBar.post(() -> {
            int deleteBarHeight = deleteBar.getHeight();
            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(),
                    deleteBarHeight + 8 // +8dp для отступа
            );
        });

        // Скрываем bottom navigation с анимацией
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();

            activity.binding.bottomAppBar.animate()
                    .alpha(0f)
                    .translationY(activity.binding.bottomAppBar.getHeight())
                    .setDuration(300)
                    .withEndAction(() -> activity.binding.bottomAppBar.setVisibility(View.GONE))
                    .start();

            activity.binding.fabAdd.animate()
                    .alpha(0f)
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(300)
                    .withEndAction(() -> activity.binding.fabAdd.setVisibility(View.GONE))
                    .start();

            activity.binding.frameLayout.setPadding(0, 0, 0, 0);
        }
    }

    // Выйти из режима удаления
    private void exitDeleteMode() {
        isDeleteMode = false;
        isAllSelected = false;
        adapter.setDeleteMode(false);

        updateSelectAllIcon();

        View deleteBar = getView().findViewById(R.id.delete_mode_bottom_bar);

        // DeleteBar уезжает СВЕРХУ ВНИЗ
        deleteBar.animate()
                .translationY(deleteBar.getHeight()) // Уезжает вниз
                .setDuration(300)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> deleteBar.setVisibility(View.GONE))
                .start();

        recyclerView.setPadding(
                recyclerView.getPaddingLeft(),
                recyclerView.getPaddingTop(),
                recyclerView.getPaddingRight(),
                0
        );

        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();

            // BottomAppBar появляется СНИЗУ ВВЕРХ
            activity.binding.bottomAppBar.setVisibility(View.VISIBLE);
            activity.binding.bottomAppBar.setAlpha(0f);
            activity.binding.bottomAppBar.setTranslationY(activity.binding.bottomAppBar.getHeight()); // Начинается снизу
            activity.binding.bottomAppBar.animate()
                    .alpha(1f)
                    .translationY(0) // Поднимается на свою позицию
                    .setDuration(300)
                    .start();

            // Просто показываем FAB
            activity.binding.fabAdd.setVisibility(View.VISIBLE);
            activity.binding.fabAdd.setAlpha(1f);
            activity.binding.fabAdd.setScaleX(1f);
            activity.binding.fabAdd.setScaleY(1f);

            // Восстанавливаем отступ снизу
            activity.binding.frameLayout.post(() -> {
                int bottomBarHeight = activity.binding.bottomAppBar.getHeight();
                activity.binding.frameLayout.setPadding(0, 0, 0, bottomBarHeight);
            });
        }
    }



    // Переключить "выбрать все"
    private void toggleSelectAll() {
        if (isAllSelected) {
            adapter.deselectAll();
            isAllSelected = false;
        } else {
            adapter.selectAll();
            isAllSelected = true;
        }
        updateSelectAllIcon();
    }

    // Выбрать все
    private void selectAll() {
        adapter.selectAll();
        isAllSelected = true;
        updateSelectAllIcon();
    }

    // Обновить иконку галочки
    private void updateSelectAllIcon() {
        if (selectAllItem != null) {
            if (isAllSelected) {
                selectAllItem.setIcon(R.drawable.ic_check_box_checked);
            } else {
                selectAllItem.setIcon(R.drawable.ic_check_box_outline);
            }
        }
    }

    private void loadLocations() {
        List<Location> locations = db.locationDao().getAllLocations();

        if (locations.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setLocations(locations);
        }
    }

    private void checkPermissionsAndStartService() {
        boolean hasFineLocation = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean hasBackgroundLocation = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasBackgroundLocation = ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        if (hasFineLocation && hasBackgroundLocation) {
            startLocationService();
        } else {
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

    private void startLocationService() {
        Intent serviceIntent = new Intent(requireContext(), LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
    }

    private void checkAndStopService() {
        List<Location> locations = db.locationDao().getAllLocations();

        boolean hasActiveLocation = false;
        for (Location location : locations) {
            if (location.isActive()) {
                hasActiveLocation = true;
                break;
            }
        }

        if (!hasActiveLocation) {
            Intent serviceIntent = new Intent(requireContext(), LocationService.class);
            requireContext().stopService(serviceIntent);

            Toast.makeText(requireContext(),
                    "Отслеживание локаций остановлено",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_select_all) {
            // Проверяем есть ли локации
            if (adapter.getItemCount() == 0) {
                Toast.makeText(requireContext(),
                        "Нет локаций для выбора",
                        Toast.LENGTH_SHORT).show();
                return true;
            }

            // Клик по галочке
            if (!isDeleteMode) {
                // Если не в режиме удаления - входим в него и выбираем все
                enterDeleteMode();
                selectAll();
            } else {
                // Если уже в режиме удаления - переключаем выбор всех
                toggleSelectAll();
            }
            return true;
        } else if (item.getItemId() == R.id.action_delete_mode) {
            // Проверяем есть ли локации
            if (adapter.getItemCount() == 0) {
                Toast.makeText(requireContext(),
                        "Нет локаций для удаления",
                        Toast.LENGTH_SHORT).show();
                return true;
            }

            // Клик по корзине
            if (!isDeleteMode) {
                enterDeleteMode();
            } else {
                exitDeleteMode();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}