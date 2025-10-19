package com.example.sleepytrip;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.sleepytrip.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Инициализируем DrawerLayout и NavigationView
        drawerLayout = binding.drawerLayout;
        navigationView = binding.navView;

        // Fix bottom navigation padding
        binding.bottomNavigationView.setBackground(null);
        binding.bottomNavigationView.setPadding(0, 0, 0, 0);

        // Try to remove padding from child view as well
        if (binding.bottomNavigationView.getChildCount() > 0) {
            ViewGroup menuView = (ViewGroup) binding.bottomNavigationView.getChildAt(0);
            if (menuView != null) {
                menuView.setPadding(0, 0, 0, 0);
            }
        }

        // Load initial fragment only if savedInstanceState is null
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment(), true);
        }

        // Обработчик для bottom navigation
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.home) {
                replaceFragment(new HomeFragment(), true);
                return true;
            } else if (itemId == R.id.more) {
                // Открываем drawer при клике на More
                drawerLayout.openDrawer(navigationView);
                return true;
            }

            return false;
        });

        // Обработчик для drawer menu
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.drawer_settings) {
                // Открыть настройки
                replaceFragment(new SettingsFragment(), true);
            } else if (itemId == R.id.drawer_about) {
                // О приложении
                replaceFragment(new AboutFragment(), true);
            } else if (itemId == R.id.drawer_exit) {
                // Выход
                finish();
            }

            // Закрываем drawer после выбора пункта
            drawerLayout.closeDrawer(navigationView);
            return true;
        });

        // Обработчик для FAB кнопки
        binding.fabAdd.setOnClickListener(v -> {
            replaceFragment(new AddLocationFragment(), false);
        });

        // Apply window insets to frame_layout instead of main
        ViewCompat.setOnApplyWindowInsetsListener(binding.frameLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Only apply top padding, let bottom navigation handle bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    public void replaceFragment(Fragment fragment, boolean showBottomNav) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();

        // Показываем или скрываем bottom navigation
        if (showBottomNav) {
            binding.bottomAppBar.setVisibility(View.VISIBLE);
            binding.fabAdd.setVisibility(View.VISIBLE);
        } else {
            binding.bottomAppBar.setVisibility(View.GONE);
            binding.fabAdd.setVisibility(View.GONE);
        }
    }
}