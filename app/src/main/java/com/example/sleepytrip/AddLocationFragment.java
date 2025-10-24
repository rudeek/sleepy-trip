package com.example.sleepytrip;

import android.Manifest;
import android.content.pm.PackageManager;
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
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AddLocationFragment extends Fragment implements OnMapReadyCallback {

    // Элементы интерфейса
    private Button btnAddLocation;
    private Button btnCancel;

    // Google Map объект
    private GoogleMap mMap;

    // Маркер который пользователь устанавливает на карте
    private Marker selectedMarker;

    // Код запроса разрешения на геолокацию
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Инфлейтим (создаём) view из XML layout
        View view = inflater.inflate(R.layout.fragment_add_location, container, false);

        // Находим кнопки в layout
        btnAddLocation = view.findViewById(R.id.btn_add_location);
        btnCancel = view.findViewById(R.id.btn_cancel);

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

                // Показываем уведомление с адресом
                Toast.makeText(getContext(),
                        "Location saved: " + address,
                        Toast.LENGTH_LONG).show();

                // TODO: Здесь будет сохранение в базу данных

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
            // Удаляем предыдущий маркер если он был
            if (selectedMarker != null) {
                selectedMarker.remove();
            }

            // Создаём новый маркер на месте клика
            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location")
                    .draggable(true)); // Маркер можно перетаскивать

            // Получаем адрес по координатам
            String address = getAddressFromLatLng(latLng);

            // Показываем адрес в информационном окне маркера
            if (selectedMarker != null) {
                selectedMarker.setSnippet(address);
                selectedMarker.showInfoWindow();
            }
        });
    }

    // === ФУНКЦИЯ ДЛЯ ПОЛУЧЕНИЯ АДРЕСА ПО КООРДИНАТАМ ===
    // Принимает координаты (широта, долгота)
    // Возвращает читаемый адрес (улица, город, страна)
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

                // Добавляем название улицы если есть
                if (address.getThoroughfare() != null) {
                    fullAddress.append(address.getThoroughfare());
                }

                // Добавляем город если есть
                if (address.getLocality() != null) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getLocality());
                }

                // Добавляем страну если есть
                if (address.getCountryName() != null) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(address.getCountryName());
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