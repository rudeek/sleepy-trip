package com.example.sleepytrip;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.sleepytrip.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ActionBarDrawerToggle drawerToggle;

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

        // Инициализируем Toolbar
        toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        // Настраиваем drawer toggle (кнопка гамбургер в toolbar)
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Устанавливаем заголовок toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("SleepyTrip");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Показываем кнопку назад/гамбургер
        }

        // Load initial fragment only if savedInstanceState is null
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment(), true);
        }

        // Обработчик для drawer menu
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.drawer_settings) {
                replaceFragment(new SettingsFragment(), false);
            } else if (itemId == R.id.drawer_about) {
                replaceFragment(new AboutFragment(), false);
            } else if (itemId == R.id.drawer_exit) {
                finish();
            }

            // Закрываем drawer после выбора пункта
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Обработчик для FAB кнопки
        binding.fabAdd.setOnClickListener(v -> {
            replaceFragment(new AddLocationFragment(), false);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Обрабатываем нажатие на кнопку гамбургер
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Синхронизируем состояние после восстановления активности
        drawerToggle.syncState();
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