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

    //binding для доступа к элементам из xml (вместо findviewbyid)
    ActivityMainBinding binding;

    //drawer - это боковое выдвижное меню слева
    private DrawerLayout drawerLayout;

    //navigationview - это само содержимое drawer меню (пункты меню)
    private NavigationView navigationView;

    //toolbar - это верхняя панель приложения (где заголовок и кнопки)
    private Toolbar toolbar;

    //actionbardrawertoggle - специальный класс который управляет кнопкой гамбургера
    //и анимацией открытия/закрытия drawer
    private ActionBarDrawerToggle drawerToggle;

    //флаг который показывает, в каком режиме мы сейчас:
    //false = показываем гамбургер (можно открыть drawer)
    //true = показываем стрелку назад (drawer заблокирован)
    private boolean isBackButtonMode = false;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(savedInstanceState);

        // === применяем сохранённый язык ===
        String savedLanguage = SettingsFragment.getCurrentLanguage(this);
        SettingsFragment.setLocale(this, savedLanguage);

        //создаём binding - это позволяет обращаться к элементам из xml
        //например: binding.toolbar вместо findviewbyid(r.id.toolbar)
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        //устанавливаем наш layout как содержимое активности
        setContentView(binding.getRoot());

        // === запрашиваем все разрешения сразу при запуске ===
        requestAllPermissions();

        // === инициализация элементов ===

        //получаем ссылку на drawerlayout из нашего xml
        drawerLayout = binding.drawerLayout;

        //получаем ссылку на navigationview (боковое меню)
        navigationView = binding.navView;

        //получаем ссылку на toolbar (верхняя панель)
        toolbar = binding.toolbar;

        //говорим системе, что наш toolbar теперь actionbar (верхняя панель приложения)
        setSupportActionBar(toolbar);

        // === настройка drawer toggle (кнопка гамбургера) ===

        //создаём actionbardrawertoggle - это магия которая:
        //1. добавляет иконку гамбургера в toolbar
        //2. открывает/закрывает drawer при клике на гамбургер
        //3. анимирует превращение гамбургера в стрелку
        drawerToggle = new ActionBarDrawerToggle(
                this,                                    //контекст (наша активность)
                drawerLayout,                            //drawerlayout который нужно открывать
                toolbar,                                 //toolbar куда добавить кнопку
                R.string.navigation_drawer_open,         //описание для accessibility (для незрячих)
                R.string.navigation_drawer_close         //описание для accessibility
        );

        //добавляем drawertoggle как слушателя событий drawer'а
        //теперь он будет знать когда drawer открывается/закрывается
        drawerLayout.addDrawerListener(drawerToggle);

        //синхронизируем состояние - обновляем иконку гамбургера
        drawerToggle.syncState();

        // === настройка action bar (верхней панели) ===

        //проверяем что actionbar существует
        if (getSupportActionBar() != null) {
            //устанавливаем заголовок приложения в toolbar
            getSupportActionBar().setTitle("SleepyTrip");

            //включаем кнопку "home" (это либо гамбургер, либо стрелка назад)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // === обработка системной кнопки "назад" ===

        //в android 13+ кнопку "назад" нужно обрабатывать через onbackpressedcallback
        //это новый способ (старый onbackpressed() устарел)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                //когда пользователь нажимает системную кнопку "назад" на телефоне
                //вызываем нашу функцию которая решает что делать
                handleBackNavigation();
            }
        });

        // === загрузка начального фрагмента ===

        //проверяем что это первый запуск (не восстановление после поворота экрана)
        if (savedInstanceState == null) {
            //проверяем: были ли мы в настройках перед перезапуском?
            SharedPreferences prefs = getSharedPreferences("SleepyTripSettings", MODE_PRIVATE);
            boolean wasInSettings = prefs.getBoolean("was_in_settings", false);

            if (wasInSettings) {
                //если были в настройках - возвращаемся туда
                prefs.edit().putBoolean("was_in_settings", false).apply(); //сбрасываем флаг
                replaceFragment(new SettingsFragment(), false);
            } else {
                //иначе загружаем домашний экран
                replaceFragment(new HomeFragment(), true);
            }
        }

        // === обработка кликов в drawer меню ===

        //устанавливаем слушателя на клики по пунктам бокового меню
        navigationView.setNavigationItemSelectedListener(item -> {
            //получаем id нажатого пункта меню
            int itemId = item.getItemId();

            //проверяем какой пункт меню нажали и открываем нужный фрагмент
            if (itemId == R.id.drawer_settings) {
                //если нажали "настройки" - открываем settingsfragment
                //false = скрываем нижнюю навигацию на этом экране
                replaceFragment(new SettingsFragment(), false);
            } else if (itemId == R.id.drawer_about) {
                //если нажали "о приложении" - открываем aboutfragment
                replaceFragment(new AboutFragment(), false);
            } else if (itemId == R.id.drawer_exit) {
                //если нажали "выход" - закрываем приложение
                finish();
            }

            //после выбора пункта меню закрываем drawer
            //gravitycompat.start означает "слева" (учитывая разные языки)
            drawerLayout.closeDrawer(GravityCompat.START);

            //возвращаем true = событие обработано
            return true;
        });

        // === обработка клика на fab (круглую кнопку внизу) ===

        //когда пользователь нажимает на fab (floating action button)
        binding.fabAdd.setOnClickListener(v -> {
            //открываем фрагмент для добавления новой локации
            //false = скрываем нижнюю навигацию
            replaceFragment(new AddLocationFragment(), false);
        });
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        //применяем язык перед созданием контекста
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

        //применяем тему в зависимости от системных настроек
        applyThemeBasedOnSystemSettings(newBase);
    }
    //автоматическое применение темы
    private void applyThemeBasedOnSystemSettings(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                //темная тема включена на устройстве
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;

            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            default:
                //светлая тема или тема не определена
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }

    // === функция для обработки навигации назад ===

    //эта функция решает что делать когда пользователь хочет вернуться назад
    //она вызывается и при клике на стрелку в toolbar, и при нажатии системной кнопки "назад"
    private void handleBackNavigation() {
        //сначала проверяем: открыт ли drawer?
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            //если drawer открыт - просто закрываем его
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            //если drawer закрыт, проверяем на каком мы фрагменте

            //получаем текущий фрагмент который сейчас показывается
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);

            //проверяем: это не домашняя страница?
            if (!(currentFragment instanceof HomeFragment)) {
                //если мы не на домашней странице - возвращаемся на неё
                //replacefragment() сам установит правильный заголовок и включит гамбургер
                replaceFragment(new HomeFragment(), true);  //true = показываем bottom navigation
            } else {
                //если мы уже на домашней странице - выходим из приложения
                finish();
            }
        }
    }

    // === обработка кликов на кнопки в toolbar ===

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //эта функция вызывается когда пользователь нажимает на кнопки в toolbar

        //проверяем что нажали именно на home кнопку (гамбургер или стрелка)
        if (item.getItemId() == android.R.id.home) {
            //проверяем в каком мы режиме
            if (isBackButtonMode) {
                //режим "стрелка назад" - возвращаемся на главную
                handleBackNavigation();
                return true;  //true = событие обработано
            } else {
                //режим "гамбургер" - передаём обработку drawertoggle
                //он сам откроет/закроет drawer
                return drawerToggle.onOptionsItemSelected(item);
            }
        }
        //если нажали на другую кнопку - передаём обработку родителю
        return super.onOptionsItemSelected(item);
    }

    // === синхронизация состояния после создания ===

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        //синхронизируем состояние drawer toggle после полной инициализации активности
        //это гарантирует что иконка гамбургера показывается правильно
        drawerToggle.syncState();
    }

    // === функция для замены фрагмента ===

    //эта функция меняет текущий фрагмент на новый
    //fragment - какой фрагмент показать
    //showbottomnav - показывать ли нижнюю навигацию (bottomappbar и fab)
    public void replaceFragment(Fragment fragment, boolean showBottomNav) {
        //получаем fragmentmanager - он управляет фрагментами
        FragmentManager fragmentManager = getSupportFragmentManager();

        //начинаем транзакцию (группу изменений с фрагментами)
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // === добавляем анимации ===
        //setcustomanimations(enter, exit, popenter, popexit)
        //enter - анимация появления нового фрагмента
        //exit - анимация исчезновения старого фрагмента
        //popenter - анимация при нажатии "назад" (появление)
        //popexit - анимация при нажатии "назад" (исчезновение)
        fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right,   //новый въезжает справа
                R.anim.slide_out_left,   //старый уезжает влево
                R.anim.slide_in_left,    //при возврате въезжает слева
                R.anim.slide_out_right   //при возврате уезжает вправо
        );

        //заменяем фрагмент в контейнере frame_layout на новый
        fragmentTransaction.replace(R.id.frame_layout, fragment);

        //применяем изменения (показываем новый фрагмент)
        fragmentTransaction.commit();

        //решаем показывать или скрывать нижнюю навигацию
        if (showBottomNav) {
            //плавно показываем
            binding.bottomAppBar.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(300)
                    .withStartAction(() -> binding.bottomAppBar.setVisibility(View.VISIBLE))
                    .start();

            //для fab используем только alpha (без scale!)
            binding.fabAdd.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withStartAction(() -> binding.fabAdd.setVisibility(View.VISIBLE))
                    .start();

            //добавьте это: добавляем padding снизу для frame_layout
            binding.frameLayout.post(() -> {
                int bottomBarHeight = binding.bottomAppBar.getHeight();
                binding.frameLayout.setPadding(0, 0, 0, bottomBarHeight);
            });

            setToolbarTitle(getString(R.string.home_title));
            enableBackButton(false);
        } else {
            //плавно скрываем
            binding.bottomAppBar.animate()
                    .alpha(0f)
                    .translationY(binding.bottomAppBar.getHeight())
                    .setDuration(300)
                    .withEndAction(() -> binding.bottomAppBar.setVisibility(View.GONE))
                    .start();

            //для fab используем только alpha (без scale!)
            binding.fabAdd.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> binding.fabAdd.setVisibility(View.GONE))
                    .start();

            binding.frameLayout.setPadding(0, 0, 0, 0);

            //важно: если скрываем bottom navigation, значит это settings/about
            //определяем какой заголовок показать
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

    // === функция для изменения заголовка toolbar ===

    //эта функция меняет текст в toolbar (например "sleepytrip" на "settings")
    public void setToolbarTitle(String title) {
        //проверяем что actionbar существует
        if (getSupportActionBar() != null) {
            //устанавливаем новый заголовок
            getSupportActionBar().setTitle(title);
        }
    }

    // === функция для переключения между гамбургером и стрелкой назад ===

    //enable = true: показываем стрелку назад (для страниц settings, about)
    //enable = false: показываем гамбургер (для домашней страницы)
    public void enableBackButton(boolean enable) {
        //если состояние не изменилось - ничего не делаем
        if (isBackButtonMode == enable) {
            return;
        }

        //запоминаем текущий режим в флаг
        isBackButtonMode = enable;

        if (enable) {
            // === режим "стрелка назад" ===

            //блокируем drawer - теперь его нельзя открыть свайпом
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            //отключаем индикатор drawer (иконку гамбургера)
            drawerToggle.setDrawerIndicatorEnabled(false);

            //проверяем что actionbar существует
            if (getSupportActionBar() != null) {
                //включаем кнопку "home" (она станет стрелкой)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            //устанавливаем свою иконку стрелки назад вместо гамбургера
            toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24);

            //важно: устанавливаем обработчик клика на эту иконку
            //когда пользователь нажмёт на стрелку, вызовется handlebacknavigation()
            toolbar.setNavigationOnClickListener(v -> handleBackNavigation());
        } else {

            // === режим "гамбургер" ===

            //разблокируем drawer - теперь его можно открыть свайпом
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

            //убираем старый listener чтобы не было конфликтов
            drawerLayout.removeDrawerListener(drawerToggle);

            //пересоздаём drawertoggle с нуля - это гарантирует что всё работает
            drawerToggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );

            //добавляем новый listener
            drawerLayout.addDrawerListener(drawerToggle);

            //включаем индикатор drawer (показываем гамбургер)
            drawerToggle.setDrawerIndicatorEnabled(true);

            //проверяем что actionbar существует
            if (getSupportActionBar() != null) {
                //включаем кнопку "home"
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            //синхронизируем состояние - это активирует гамбургер
            drawerToggle.syncState();
        }
    }

    // === метод для запроса всех разрешений ===
    private void requestAllPermissions() {
        //список разрешений которые нужно запросить
        java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();

        //1. геолокация (обязательно)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        //2. уведомления (для android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        //если есть разрешения для запроса - запрашиваем
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    1001
            );
        } else {
            //все разрешения уже выданы
            checkOverlayPermission();
        }
    }

    // === обработка результата запроса разрешений ===
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

                    //определяем какое разрешение отклонено
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

    // === проверка разрешения на показ поверх других окон ===
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
