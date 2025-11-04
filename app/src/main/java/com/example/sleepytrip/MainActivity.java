package com.example.sleepytrip;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.sleepytrip.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Binding для доступа к элементам из XML (вместо findViewById)
    ActivityMainBinding binding;

    // Drawer - это боковое выдвижное меню слева
    private DrawerLayout drawerLayout;

    // NavigationView - это само содержимое drawer меню (пункты меню)
    private NavigationView navigationView;

    // Toolbar - это верхняя панель приложения (где заголовок и кнопки)
    private Toolbar toolbar;

    // ActionBarDrawerToggle - специальный класс который управляет кнопкой гамбургера
    // и анимацией открытия/закрытия drawer
    private ActionBarDrawerToggle drawerToggle;

    // Флаг который показывает, в каком режиме мы сейчас:
    // false = показываем гамбургер (можно открыть drawer)
    // true = показываем стрелку назад (drawer заблокирован)
    private boolean isBackButtonMode = false;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);

        // === ПРИМЕНЯЕМ СОХРАНЁННЫЙ ЯЗЫК ===
        String savedLanguage = SettingsFragment.getCurrentLanguage(this);
        SettingsFragment.setLocale(this, savedLanguage);

        // Создаём binding - это позволяет обращаться к элементам из XML
        // Например: binding.toolbar вместо findViewById(R.id.toolbar)
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // Устанавливаем наш layout как содержимое активности
        setContentView(binding.getRoot());

        // === ЗАПРАШИВАЕМ ВСЕ РАЗРЕШЕНИЯ СРАЗУ ПРИ ЗАПУСКЕ ===
        requestAllPermissions();

        // === ИНИЦИАЛИЗАЦИЯ ЭЛЕМЕНТОВ ===

        // Получаем ссылку на DrawerLayout из нашего XML
        drawerLayout = binding.drawerLayout;

        // Получаем ссылку на NavigationView (боковое меню)
        navigationView = binding.navView;

        // Получаем ссылку на Toolbar (верхняя панель)
        toolbar = binding.toolbar;

        // Говорим системе, что наш toolbar теперь ActionBar (верхняя панель приложения)
        setSupportActionBar(toolbar);

        // === НАСТРОЙКА DRAWER TOGGLE (КНОПКА ГАМБУРГЕРА) ===

        // Создаём ActionBarDrawerToggle - это магия которая:
        // 1. Добавляет иконку гамбургера в toolbar
        // 2. Открывает/закрывает drawer при клике на гамбургер
        // 3. Анимирует превращение гамбургера в стрелку
        drawerToggle = new ActionBarDrawerToggle(
                this,                                    // Контекст (наша активность)
                drawerLayout,                            // DrawerLayout который нужно открывать
                toolbar,                                 // Toolbar куда добавить кнопку
                R.string.navigation_drawer_open,         // Описание для accessibility (для незрячих)
                R.string.navigation_drawer_close         // Описание для accessibility
        );

        // Добавляем drawerToggle как слушателя событий drawer'а
        // Теперь он будет знать когда drawer открывается/закрывается
        drawerLayout.addDrawerListener(drawerToggle);

        // Синхронизируем состояние - обновляем иконку гамбургера
        drawerToggle.syncState();

        // === НАСТРОЙКА ACTION BAR (ВЕРХНЕЙ ПАНЕЛИ) ===

        // Проверяем что ActionBar существует
        if (getSupportActionBar() != null) {
            // Устанавливаем заголовок приложения в toolbar
            getSupportActionBar().setTitle("SleepyTrip");

            // Включаем кнопку "home" (это либо гамбургер, либо стрелка назад)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // === ОБРАБОТКА СИСТЕМНОЙ КНОПКИ "НАЗАД" ===

        // В Android 13+ кнопку "назад" нужно обрабатывать через OnBackPressedCallback
        // Это новый способ (старый onBackPressed() устарел)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Когда пользователь нажимает системную кнопку "назад" на телефоне
                // вызываем нашу функцию которая решает что делать
                handleBackNavigation();
            }
        });

        // === ЗАГРУЗКА НАЧАЛЬНОГО ФРАГМЕНТА ===

        // Проверяем что это первый запуск (не восстановление после поворота экрана)
        if (savedInstanceState == null) {
            // ⭐ ПРОВЕРЯЕМ: Были ли мы в настройках перед перезапуском?
            SharedPreferences prefs = getSharedPreferences("SleepyTripSettings", MODE_PRIVATE);
            boolean wasInSettings = prefs.getBoolean("was_in_settings", false);

            if (wasInSettings) {
                // ⭐ Если были в настройках - возвращаемся туда
                prefs.edit().putBoolean("was_in_settings", false).apply(); // Сбрасываем флаг
                replaceFragment(new SettingsFragment(), false);
            } else {
                // Иначе загружаем домашний экран
                replaceFragment(new HomeFragment(), true);
            }
        }

        // === ОБРАБОТКА КЛИКОВ В DRAWER МЕНЮ ===

        // Устанавливаем слушателя на клики по пунктам бокового меню
        navigationView.setNavigationItemSelectedListener(item -> {
            // Получаем ID нажатого пункта меню
            int itemId = item.getItemId();

            // Проверяем какой пункт меню нажали и открываем нужный фрагмент
            if (itemId == R.id.drawer_settings) {
                // Если нажали "Настройки" - открываем SettingsFragment
                // false = скрываем нижнюю навигацию на этом экране
                replaceFragment(new SettingsFragment(), false);
            } else if (itemId == R.id.drawer_about) {
                // Если нажали "О приложении" - открываем AboutFragment
                replaceFragment(new AboutFragment(), false);
            } else if (itemId == R.id.drawer_exit) {
                // Если нажали "Выход" - закрываем приложение
                finish();
            }

            // После выбора пункта меню закрываем drawer
            // GravityCompat.START означает "слева" (учитывая разные языки)
            drawerLayout.closeDrawer(GravityCompat.START);

            // Возвращаем true = событие обработано
            return true;
        });

        // === ОБРАБОТКА КЛИКА НА FAB (КРУГЛУЮ КНОПКУ ВНИЗУ) ===

        // Когда пользователь нажимает на FAB (Floating Action Button)
        binding.fabAdd.setOnClickListener(v -> {
            // Открываем фрагмент для добавления новой локации
            // false = скрываем нижнюю навигацию
            replaceFragment(new AddLocationFragment(), false);
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Применяем язык перед созданием контекста
        String savedLanguage = newBase.getSharedPreferences("SleepyTripSettings", Context.MODE_PRIVATE)
                .getString("language", "en");

        Locale locale = new Locale(savedLanguage);
        Locale.setDefault(locale);

        Configuration config = new Configuration(newBase.getResources().getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            Context context = newBase.createConfigurationContext(config);
            super.attachBaseContext(context);
        } else {
            config.locale = locale;
            newBase.getResources().updateConfiguration(config, newBase.getResources().getDisplayMetrics());
            super.attachBaseContext(newBase);
        }

        // ⭐ НОВОЕ: Применяем тему в зависимости от системных настроек
        applyThemeBasedOnSystemSettings(newBase);
    }
    // ⭐ НОВЫЙ МЕТОД: Автоматическое применение темы
    private void applyThemeBasedOnSystemSettings(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                // Темная тема включена на устройстве
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;

            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            default:
                // Светлая тема или тема не определена
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }

    // === ФУНКЦИЯ ДЛЯ ОБРАБОТКИ НАВИГАЦИИ НАЗАД ===

    // Эта функция решает что делать когда пользователь хочет вернуться назад
    // Она вызывается и при клике на стрелку в toolbar, и при нажатии системной кнопки "назад"
    private void handleBackNavigation() {
        // Сначала проверяем: открыт ли drawer?
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            // Если drawer открыт - просто закрываем его
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Если drawer закрыт, проверяем на каком мы фрагменте

            // Получаем текущий фрагмент который сейчас показывается
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);

            // Проверяем: это НЕ домашняя страница?
            if (!(currentFragment instanceof HomeFragment)) {
                // Если мы не на домашней странице - возвращаемся на неё
                // replaceFragment() сам установит правильный заголовок и включит гамбургер
                replaceFragment(new HomeFragment(), true);  // true = показываем bottom navigation
            } else {
                // Если мы уже на домашней странице - выходим из приложения
                finish();
            }
        }
    }

    // === ОБРАБОТКА КЛИКОВ НА КНОПКИ В TOOLBAR ===

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Эта функция вызывается когда пользователь нажимает на кнопки в toolbar

        // Проверяем что нажали именно на home кнопку (гамбургер или стрелка)
        if (item.getItemId() == android.R.id.home) {
            // Проверяем в каком мы режиме
            if (isBackButtonMode) {
                // Режим "стрелка назад" - возвращаемся на главную
                handleBackNavigation();
                return true;  // true = событие обработано
            } else {
                // Режим "гамбургер" - передаём обработку drawerToggle
                // Он сам откроет/закроет drawer
                return drawerToggle.onOptionsItemSelected(item);
            }
        }
        // Если нажали на другую кнопку - передаём обработку родителю
        return super.onOptionsItemSelected(item);
    }

    // === СИНХРОНИЗАЦИЯ СОСТОЯНИЯ ПОСЛЕ СОЗДАНИЯ ===

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Синхронизируем состояние drawer toggle после полной инициализации активности
        // Это гарантирует что иконка гамбургера показывается правильно
        drawerToggle.syncState();
    }

    // === ФУНКЦИЯ ДЛЯ ЗАМЕНЫ ФРАГМЕНТА ===

    // Эта функция меняет текущий фрагмент на новый
    // fragment - какой фрагмент показать
    // showBottomNav - показывать ли нижнюю навигацию (BottomAppBar и FAB)
    public void replaceFragment(Fragment fragment, boolean showBottomNav) {
        // Получаем FragmentManager - он управляет фрагментами
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Начинаем транзакцию (группу изменений с фрагментами)
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // === ДОБАВЛЯЕМ АНИМАЦИИ ===
        // setCustomAnimations(enter, exit, popEnter, popExit)
        // enter - анимация появления нового фрагмента
        // exit - анимация исчезновения старого фрагмента
        // popEnter - анимация при нажатии "назад" (появление)
        // popExit - анимация при нажатии "назад" (исчезновение)
        fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right,   // Новый въезжает справа
                R.anim.slide_out_left,   // Старый уезжает влево
                R.anim.slide_in_left,    // При возврате въезжает слева
                R.anim.slide_out_right   // При возврате уезжает вправо
        );

        // Заменяем фрагмент в контейнере frame_layout на новый
        fragmentTransaction.replace(R.id.frame_layout, fragment);

        // Применяем изменения (показываем новый фрагмент)
        fragmentTransaction.commit();

        // Решаем показывать или скрывать нижнюю навигацию
        if (showBottomNav) {
            // Плавно показываем
            binding.bottomAppBar.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(300)
                    .withStartAction(() -> binding.bottomAppBar.setVisibility(View.VISIBLE))
                    .start();

            // Для FAB используем только alpha (без scale!)
            binding.fabAdd.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withStartAction(() -> binding.fabAdd.setVisibility(View.VISIBLE))
                    .start();

            // ДОБАВЬТЕ ЭТО: Добавляем padding снизу для frame_layout
            binding.frameLayout.post(() -> {
                int bottomBarHeight = binding.bottomAppBar.getHeight();
                binding.frameLayout.setPadding(0, 0, 0, bottomBarHeight);
            });

            setToolbarTitle(getString(R.string.home_title));
            enableBackButton(false);
        } else {
            // Плавно скрываем
            binding.bottomAppBar.animate()
                    .alpha(0f)
                    .translationY(binding.bottomAppBar.getHeight())
                    .setDuration(300)
                    .withEndAction(() -> binding.bottomAppBar.setVisibility(View.GONE))
                    .start();

            // Для FAB используем только alpha (без scale!)
            binding.fabAdd.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> binding.fabAdd.setVisibility(View.GONE))
                    .start();

            binding.frameLayout.setPadding(0, 0, 0, 0);

            // ВАЖНО: Если скрываем bottom navigation, значит это Settings/About
            // Определяем какой заголовок показать
            if (fragment instanceof SettingsFragment) {
                setToolbarTitle(getString(R.string.settings_title));
                enableBackButton(true);
            } else if (fragment instanceof AboutFragment) {
                setToolbarTitle(getString(R.string.about_title));
                enableBackButton(true);
            } else if (fragment instanceof AddLocationFragment) {
                setToolbarTitle(getString(R.string.add_location_title));
                enableBackButton(true);
            }
        }
    }

    // === ФУНКЦИЯ ДЛЯ ИЗМЕНЕНИЯ ЗАГОЛОВКА TOOLBAR ===

    // Эта функция меняет текст в toolbar (например "SleepyTrip" на "Settings")
    public void setToolbarTitle(String title) {
        // Проверяем что ActionBar существует
        if (getSupportActionBar() != null) {
            // Устанавливаем новый заголовок
            getSupportActionBar().setTitle(title);
        }
    }

    // === ФУНКЦИЯ ДЛЯ ПЕРЕКЛЮЧЕНИЯ МЕЖДУ ГАМБУРГЕРОМ И СТРЕЛКОЙ НАЗАД ===

    // enable = true: показываем стрелку назад (для страниц Settings, About)
    // enable = false: показываем гамбургер (для домашней страницы)
    public void enableBackButton(boolean enable) {
        // Если состояние не изменилось - ничего не делаем
        if (isBackButtonMode == enable) {
            return;
        }

        // Запоминаем текущий режим в флаг
        isBackButtonMode = enable;

        if (enable) {
            // === РЕЖИМ "СТРЕЛКА НАЗАД" ===

            // Блокируем drawer - теперь его нельзя открыть свайпом
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            // Отключаем индикатор drawer (иконку гамбургера)
            drawerToggle.setDrawerIndicatorEnabled(false);

            // Проверяем что ActionBar существует
            if (getSupportActionBar() != null) {
                // Включаем кнопку "home" (она станет стрелкой)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Устанавливаем свою иконку стрелки назад вместо гамбургера
            toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24);

            // ВАЖНО: Устанавливаем обработчик клика на эту иконку
            // Когда пользователь нажмёт на стрелку, вызовется handleBackNavigation()
            toolbar.setNavigationOnClickListener(v -> handleBackNavigation());
        } else {

            // === РЕЖИМ "ГАМБУРГЕР" ===

            // Разблокируем drawer - теперь его можно открыть свайпом
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

            // Убираем старый listener чтобы не было конфликтов
            drawerLayout.removeDrawerListener(drawerToggle);

            // Пересоздаём drawerToggle с нуля - это гарантирует что всё работает
            drawerToggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );

            // Добавляем новый listener
            drawerLayout.addDrawerListener(drawerToggle);

            // Включаем индикатор drawer (показываем гамбургер)
            drawerToggle.setDrawerIndicatorEnabled(true);

            // Проверяем что ActionBar существует
            if (getSupportActionBar() != null) {
                // Включаем кнопку "home"
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Синхронизируем состояние - это активирует гамбургер
            drawerToggle.syncState();
        }
    }

    // === МЕТОД ДЛЯ ЗАПРОСА ВСЕХ РАЗРЕШЕНИЙ ===
    private void requestAllPermissions() {
        // Список разрешений которые нужно запросить
        java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();

        // 1. Геолокация (обязательно)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // 2. Уведомления (для Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Если есть разрешения для запроса - запрашиваем
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    1001
            );
        } else {
            // Все разрешения уже выданы
            checkOverlayPermission();
        }
    }

    // === ОБРАБОТКА РЕЗУЛЬТАТА ЗАПРОСА РАЗРЕШЕНИЙ ===
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            boolean allGranted = true;
            StringBuilder deniedPermissions = new StringBuilder();

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;

                    // Определяем какое разрешение отклонено
                    if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        deniedPermissions.append(getString(R.string.permission_location)).append("\n");
                    } else if (permissions[i].equals(Manifest.permission.POST_NOTIFICATIONS)) {
                        deniedPermissions.append(getString(R.string.permission_notifications)).append("\n");
                    }
                }
            }

            if (allGranted) {
                Toast.makeText(this, getString(R.string.permission_all_granted), Toast.LENGTH_SHORT).show();
                checkOverlayPermission();
            } else {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.permission_needed_title))
                        .setMessage(getString(R.string.permission_needed_message, deniedPermissions.toString()))
                        .setPositiveButton(getString(R.string.permission_grant), (dialog, which) -> requestAllPermissions())
                        .setNegativeButton(getString(R.string.permission_later), null)
                        .show();
            }
        }
    }

    // === ПРОВЕРКА РАЗРЕШЕНИЯ НА ПОКАЗ ПОВЕРХ ДРУГИХ ОКОН ===
    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.permission_overlay_title))
                        .setMessage(getString(R.string.permission_overlay_message))
                        .setPositiveButton(getString(R.string.permission_allow), (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, 123);
                        })
                        .setNegativeButton(getString(R.string.permission_later), null)
                        .show();
            }
        }
    }
}