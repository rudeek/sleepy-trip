package com.example.sleepytrip;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AddLocationFragment extends Fragment implements OnMapReadyCallback {

    // Элементы интерфейса
    private Button btnAddLocation;
    private Button btnCancel;
    private Slider sliderRadius;
    private TextView tvRadiusValue;

    // Google Map объект
    private GoogleMap mMap;

    // Маркер который пользователь устанавливает на карте
    private Marker selectedMarker;

    // Круг радиуса вокруг маркера
    private Circle radiusCircle;

    // Текущий радиус в метрах (по умолчанию 500м)
    private float currentRadius = 500f;

    // Код запроса разрешения на геолокацию
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Инфлейтим (создаём) view из XML layout
        View view = inflater.inflate(R.layout.fragment_add_location, container, false);

        // Находим элементы в layout
        btnAddLocation = view.findViewById(R.id.btn_add_location);
        btnCancel = view.findViewById(R.id.btn_cancel);
        sliderRadius = view.findViewById(R.id.slider_radius);
        tvRadiusValue = view.findViewById(R.id.tv_radius_value);

        // === ОБРАБОТЧИК СЛАЙДЕРА РАДИУСА ===
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            // Обновляем текущий радиус
            currentRadius = value;

            // Обновляем текст с значением радиуса
            if (value >= 1000) {
                // Если больше 1000м, показываем в километрах
                tvRadiusValue.setText(String.format("%.1f км", value / 1000));
            } else {
                // Иначе показываем в метрах
                tvRadiusValue.setText(String.format("%.0f м", value));
            }

            // Обновляем круг на карте если маркер установлен
            if (radiusCircle != null && selectedMarker != null) {
                radiusCircle.setRadius(currentRadius);
            }
        });

        // Получаем SupportMapFragment из layout
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        // Асинхронно загружаем карту
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // === ОБРАБОТЧИК КНОПКИ CANCEL ===
        // Возвращаемся на главную страницу без сохранения
        btnCancel.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.replaceFragment(new HomeFragment(), true);
            }
        });

        // === ОБРАБОТЧИК КНОПКИ ADD LOCATION ===
        // Сохраняем выбранную локацию
        btnAddLocation.setOnClickListener(v -> {
            // Проверяем что пользователь установил маркер
            if (selectedMarker != null) {
                // Получаем координаты маркера
                LatLng position = selectedMarker.getPosition();

                // Получаем адрес по координатам
                String address = getAddressFromLatLng(position);

                // Показываем уведомление с адресом и радиусом
                Toast.makeText(getContext(),
                        "Location saved: " + address + "\nRadius: " + (int)currentRadius + "m",
                        Toast.LENGTH_LONG).show();

// Получаем database
                AppDatabase db = AppDatabase.getInstance(requireContext());

// Разделяем адрес на название и полный адрес
                String[] addressParts = splitAddress(address);
                String locationName = addressParts[0];
                String fullAddress = addressParts[1].isEmpty() ? address : addressParts[1];

// Создаём объект Location
                Location location = new Location(
                        locationName,           // Название
                        fullAddress,            // Адрес
                        position.latitude,      // Широта
                        position.longitude,     // Долгота
                        currentRadius           // Радиус
                );

// Сохраняем в базу данных
                db.locationDao().insert(location);

// Показываем уведомление
                Toast.makeText(getContext(),
                        "Location saved successfully!",
                        Toast.LENGTH_SHORT).show();
                // Сохраняем: position.latitude, position.longitude, address, currentRadius

                // Возвращаемся на главную страницу
                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.replaceFragment(new HomeFragment(), true);
                }
            } else {
                // Если маркер не установлен - показываем предупреждение
                Toast.makeText(getContext(),
                        "Please select a location on the map",
                        Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // === CALLBACK КОГДА КАРТА ГОТОВА ===
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // Сохраняем ссылку на карту
        mMap = googleMap;

        // === УСТАНАВЛИВАЕМ КАСТОМНЫЙ INFO WINDOW АДАПТЕР ===
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        // Устанавливаем начальную позицию (Кишинёв, Молдова)
        LatLng chisinau = new LatLng(47.0105, 28.8638);

        // Перемещаем камеру на Кишинёв с зумом 12
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chisinau, 12));

        // === НАСТРОЙКА ЭЛЕМЕНТОВ КАРТЫ ===

        // Включаем элементы управления зумом (+/-)
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Включаем жесты (масштабирование пальцами)
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Включаем кнопку "Моя геолокация"
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Проверяем разрешение на геолокацию
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Если разрешение есть - включаем слой "Моя локация"
            mMap.setMyLocationEnabled(true);
        } else {
            // Если разрешения нет - запрашиваем его
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // === ОБРАБОТЧИК КЛИКА ПО КАРТЕ ===
        // Когда пользователь нажимает на карту
        mMap.setOnMapClickListener(latLng -> {
            // Удаляем предыдущий маркер и круг если они были
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            if (radiusCircle != null) {
                radiusCircle.remove();
            }

            // Получаем адрес по координатам
            String address = getAddressFromLatLng(latLng);

            // Разделяем адрес на заголовок и описание
            String[] addressParts = splitAddress(address);

            // Создаём новый маркер на месте клика
            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(addressParts[0])      // Название остановки/улица
                    .snippet(addressParts[1])    // Полный адрес
                    .draggable(true));            // Маркер можно перетаскивать

            // Создаём круг радиуса вокруг маркера
            radiusCircle = mMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(currentRadius)                          // Радиус в метрах
                    .strokeColor(Color.parseColor("#8D6E63"))       // Цвет границы (коричневый)
                    .strokeWidth(3f)                                 // Толщина границы
                    .fillColor(Color.parseColor("#40D7CCC8")));     // Цвет заливки (полупрозрачный бежевый)

            // Показываем информационное окно маркера
            if (selectedMarker != null) {
                selectedMarker.showInfoWindow();
            }
        });

        // === ОБРАБОТЧИК ПЕРЕТАСКИВАНИЯ МАРКЕРА ===
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                // Начало перетаскивания - ничего не делаем
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // Во время перетаскивания обновляем позицию круга
                if (radiusCircle != null) {
                    radiusCircle.setCenter(marker.getPosition());
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // Конец перетаскивания - обновляем адрес
                LatLng newPosition = marker.getPosition();
                String address = getAddressFromLatLng(newPosition);
                String[] addressParts = splitAddress(address);

                marker.setTitle(addressParts[0]);
                marker.setSnippet(addressParts[1]);
                marker.showInfoWindow();

                // Обновляем позицию круга
                if (radiusCircle != null) {
                    radiusCircle.setCenter(newPosition);
                }
            }
        });
    }

    // === КАСТОМНЫЙ АДАПТЕР ДЛЯ INFO WINDOW ===
    // Этот класс создаёт кастомное окно с информацией о маркере
    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private View mWindow;

        CustomInfoWindowAdapter() {
            // Инфлейтим наш кастомный layout
            mWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            // Возвращаем null чтобы использовать стандартную рамку
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            // Заполняем наш кастомный layout данными маркера
            TextView title = mWindow.findViewById(R.id.info_title);
            TextView snippet = mWindow.findViewById(R.id.info_snippet);

            // Устанавливаем заголовок
            title.setText(marker.getTitle());

            // Устанавливаем описание
            snippet.setText(marker.getSnippet());

            return mWindow;
        }
    }

    // === ФУНКЦИЯ ДЛЯ РАЗДЕЛЕНИЯ АДРЕСА НА ЧАСТИ ===
    // Разделяет адрес на заголовок (для title) и полное описание (для snippet)
    private String[] splitAddress(String fullAddress) {
        String[] result = new String[2];

        // Разделяем по первому переносу строки
        String[] parts = fullAddress.split("\n", 2);

        if (parts.length >= 2) {
            result[0] = parts[0]; // Первая строка - заголовок
            result[1] = parts[1]; // Остальное - описание
        } else {
            result[0] = fullAddress;
            result[1] = "";
        }

        return result;
    }

    // === ФУНКЦИЯ ДЛЯ ПОЛУЧЕНИЯ АДРЕСА ПО КООРДИНАТАМ ===
    // Принимает координаты (широта, долгота)
    // Возвращает читаемый адрес (остановка, улица, город, страна)
    private String getAddressFromLatLng(LatLng latLng) {
        // Geocoder - класс для преобразования координат в адреса
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());

        try {
            // Получаем список адресов по координатам (максимум 1)
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude,  // Широта
                    latLng.longitude, // Долгота
                    1                 // Количество результатов
            );

            // Если адрес найден
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Формируем читаемую строку адреса
                StringBuilder fullAddress = new StringBuilder();

                // === НАЗВАНИЕ ОСТАНОВКИ / ТОЧКИ ИНТЕРЕСА ===
                // getFeatureName() возвращает название места (остановка, магазин, парк и т.д.)
                String featureName = address.getFeatureName();
                if (featureName != null && !featureName.matches("^\\d.*")) {
                    // Если название не является просто цифрой (номером дома)
                    fullAddress.append(featureName);
                }

                // === НОМЕР ДОМА ===
                // getSubThoroughfare() обычно возвращает номер дома
                String houseNumber = address.getSubThoroughfare();

                // === НАЗВАНИЕ УЛИЦЫ ===
                // getThoroughfare() возвращает название улицы
                String streetName = address.getThoroughfare();

                // Формируем адрес: "Улица, номер дома"
                if (streetName != null) {
                    // Если уже есть название остановки, добавляем запятую
                    if (fullAddress.length() > 0) {
                        fullAddress.append("\n"); // Перенос на новую строку
                    }

                    fullAddress.append(streetName);

                    // Добавляем номер дома если есть
                    if (houseNumber != null) {
                        fullAddress.append(", ").append(houseNumber);
                    }
                } else if (houseNumber != null) {
                    // Если улицы нет, но номер дома есть
                    if (fullAddress.length() > 0) {
                        fullAddress.append("\n");
                    }
                    fullAddress.append(houseNumber);
                }

                // === ГОРОД ===
                String city = address.getLocality();
                if (city != null) {
                    if (fullAddress.length() > 0) {
                        fullAddress.append("\n");
                    }
                    fullAddress.append(city);
                }

                // === СТРАНА ===
                String country = address.getCountryName();
                if (country != null) {
                    if (fullAddress.length() > 0) {
                        fullAddress.append(", ");
                    }
                    fullAddress.append(country);
                }

                // Возвращаем полный адрес
                return fullAddress.toString();
            }
        } catch (IOException e) {
            // Если произошла ошибка при получении адреса
            e.printStackTrace();
        }

        // Если адрес не найден - возвращаем координаты
        return "Lat: " + String.format("%.4f", latLng.latitude) +
                ", Lng: " + String.format("%.4f", latLng.longitude);
    }

    // === ОБРАБОТКА РЕЗУЛЬТАТА ЗАПРОСА РАЗРЕШЕНИЙ ===
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Проверяем что это наш запрос разрешения на геолокацию
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Проверяем что разрешение было выдано
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Проверяем разрешение ещё раз (требование Android)
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Включаем слой "Моя локация" на карте
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                // Если пользователь отказал в разрешении
                Toast.makeText(getContext(),
                        "Location permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}