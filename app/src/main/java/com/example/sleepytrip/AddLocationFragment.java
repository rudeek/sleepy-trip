package com.example.sleepytrip;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;

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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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

    private Button btnAddLocation;
    private Button btnCancel;
    private Slider sliderRadius;
    private TextView tvRadiusValue;

    private GoogleMap mMap;
    private Marker selectedMarker;
    private Circle radiusCircle;

    // Радиус ВСЕГДА хранится в метрах (внутренний формат)
    private float currentRadiusMeters = 500f;

    // ⭐ НОВОЕ: Клиент для получения текущей локации
    private FusedLocationProviderClient fusedLocationClient;

    // ⭐ НОВОЕ: Текущее местоположение пользователя
    private LatLng currentUserLocation;
    private String currentCity = "Chisinau";  // Дефолтный город
    private String currentCountry = "Moldova"; // Дефолтная страна

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_location, container, false);

        // ⭐ НОВОЕ: Инициализируем FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnAddLocation = view.findViewById(R.id.btn_add_location);
        btnCancel = view.findViewById(R.id.btn_cancel);
        sliderRadius = view.findViewById(R.id.slider_radius);
        tvRadiusValue = view.findViewById(R.id.tv_radius_value);

        SearchView searchView = view.findViewById(R.id.search_view);

        int searchSrcTextId = searchView.getContext()
                .getResources()
                .getIdentifier("android:id/search_src_text", null, null);
        EditText searchEditText = searchView.findViewById(searchSrcTextId);
        searchEditText.setTextColor(Color.parseColor("#939df5"));
        searchEditText.setHintTextColor(Color.parseColor("#a4adfc"));

        int searchMagIconId = searchView.getContext()
                .getResources()
                .getIdentifier("android:id/search_mag_icon", null, null);
        ImageView searchIcon = searchView.findViewById(searchMagIconId);
        searchIcon.setColorFilter(Color.parseColor("#8692f7"), PorterDuff.Mode.SRC_IN);

        // === НАСТРОЙКА СЛАЙДЕРА В ЗАВИСИМОСТИ ОТ ЕДИНИЦ ИЗМЕРЕНИЯ ===
        setupSlider();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query == null || query.trim().isEmpty()) {
                    Toast.makeText(getContext(), "Введите адрес", Toast.LENGTH_SHORT).show();
                    return false;
                }

                performSmartSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // === ОБРАБОТЧИК СЛАЙДЕРА РАДИУСА ===
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            // Значение = метры
            currentRadiusMeters = value;

            // Обновляем текст
            updateRadiusText();

            // Обновляем круг
            if (radiusCircle != null && selectedMarker != null) {
                radiusCircle.setRadius(currentRadiusMeters);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // === КНОПКА CANCEL ===
        btnCancel.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.replaceFragment(new HomeFragment(), true);
            }
        });

        // === КНОПКА ADD LOCATION ===
        btnAddLocation.setOnClickListener(v -> {
            if (selectedMarker != null) {
                LatLng position = selectedMarker.getPosition();
                String address = getAddressFromLatLng(position);

                AppDatabase db = AppDatabase.getInstance(requireContext());

                String[] addressParts = splitAddress(address);
                String locationName = addressParts[0];
                String fullAddress = addressParts[1].isEmpty() ? address : addressParts[1];

                // Сохраняем радиус ВСЕГДА В МЕТРАХ
                com.example.sleepytrip.Location location = new com.example.sleepytrip.Location(
                        locationName,
                        fullAddress,
                        position.latitude,
                        position.longitude,
                        currentRadiusMeters  // Сохраняем в метрах
                );

                db.locationDao().insert(location);

                Toast.makeText(getContext(),
                        "Location saved successfully!",
                        Toast.LENGTH_SHORT).show();

                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.replaceFragment(new HomeFragment(), true);
                }
            } else {
                Toast.makeText(getContext(),
                        "Please select a location on the map",
                        Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // ⭐ НОВЫЙ МЕТОД: Умный поиск с приоритетами
    private void performSmartSearch(String query) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());

        try {
            // ШАГ 1: Ищем БЕЗ привязки к стране (глобальный поиск)
            List<Address> globalResults = geocoder.getFromLocationName(query, 10);

            if (globalResults == null || globalResults.isEmpty()) {
                Toast.makeText(getContext(), "Адрес не найден", Toast.LENGTH_SHORT).show();
                return;
            }

            // ШАГ 2: Анализируем результаты и выбираем лучший
            Address bestMatch = findBestMatch(globalResults, query);

            if (bestMatch != null) {
                showLocationOnMap(bestMatch, query);
            } else {
                Toast.makeText(getContext(), "Адрес не найден", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Ошибка при поиске адреса", Toast.LENGTH_SHORT).show();
        }
    }

    // ⭐ НОВЫЙ МЕТОД: Выбираем лучшее совпадение из результатов
    private Address findBestMatch(List<Address> addresses, String query) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }

        String queryLower = query.toLowerCase().trim();

        // Список результатов с приоритетами
        Address sameCountryMatch = null;     // Совпадение в той же стране
        Address sameCityMatch = null;        // Совпадение в том же городе
        Address majorCityMatch = null;       // Крупный город (явно другая страна)
        Address firstResult = addresses.get(0); // Первый результат как fallback

        for (Address addr : addresses) {
            String addrCountry = addr.getCountryName();
            String addrCity = addr.getLocality();
            String addrAdminArea = addr.getAdminArea(); // Регион/область

            // Проверяем совпадение города в запросе
            boolean queryContainsCity = false;
            if (addrCity != null) {
                queryContainsCity = queryLower.contains(addrCity.toLowerCase());
            }

            // Проверяем совпадение региона в запросе
            boolean queryContainsRegion = false;
            if (addrAdminArea != null) {
                queryContainsRegion = queryLower.contains(addrAdminArea.toLowerCase());
            }

            // ПРИОРИТЕТ 1: Тот же город (самый высокий)
            if (addrCity != null && addrCity.equalsIgnoreCase(currentCity)) {
                if (sameCityMatch == null) {
                    sameCityMatch = addr;
                }
            }

            // ПРИОРИТЕТ 2: Та же страна (средний)
            if (addrCountry != null && addrCountry.equalsIgnoreCase(currentCountry)) {
                if (sameCountryMatch == null) {
                    sameCountryMatch = addr;
                }

                // Если запрос содержит название города - это более точное совпадение
                if (queryContainsCity || queryContainsRegion) {
                    return addr; // Возвращаем сразу
                }
            }

            // ПРИОРИТЕТ 3: Крупный город в другой стране (если запрос содержит его название)
            if (queryContainsCity && addrCity != null && !addrCity.equalsIgnoreCase(currentCity)) {
                if (majorCityMatch == null) {
                    majorCityMatch = addr;
                }
            }
        }

        // Логика выбора результата:

        // 1. Если нашли в текущем городе - берем это
        if (sameCityMatch != null) {
            android.util.Log.d("SmartSearch", "✅ Найдено в текущем городе: " + currentCity);
            return sameCityMatch;
        }

        // 2. Если в запросе явно указан другой город - берем его
        if (majorCityMatch != null) {
            android.util.Log.d("SmartSearch", "✅ Найден указанный город: " + majorCityMatch.getLocality());
            return majorCityMatch;
        }

        // 3. Если нашли в текущей стране - берем это
        if (sameCountryMatch != null) {
            android.util.Log.d("SmartSearch", "✅ Найдено в текущей стране: " + currentCountry);
            return sameCountryMatch;
        }

        // 4. Если ничего не подошло - берем первый результат
        android.util.Log.d("SmartSearch", "⚠️ Используем первый результат");
        return firstResult;
    }

    // ⭐ НОВЫЙ МЕТОД: Показываем локацию на карте
    private void showLocationOnMap(Address address, String originalQuery) {
        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

        // Определяем zoom в зависимости от типа результата
        float zoomLevel = 15f; // Дефолт для улиц/адресов

        if (address.getLocality() != null &&
                (originalQuery.equalsIgnoreCase(address.getLocality()) ||
                        originalQuery.toLowerCase().contains(address.getLocality().toLowerCase()))) {
            // Если искали город - зум меньше
            zoomLevel = 12f;
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));

        if (selectedMarker != null) selectedMarker.remove();
        if (radiusCircle != null) radiusCircle.remove();

        String title = address.getFeatureName() != null ? address.getFeatureName() : originalQuery;
        String snippet = address.getAddressLine(0);

        selectedMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(snippet)
                .draggable(true));

        radiusCircle = mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(currentRadiusMeters)
                .strokeColor(Color.parseColor("#8D6E63"))
                .strokeWidth(3f)
                .fillColor(Color.parseColor("#40D7CCC8")));

        selectedMarker.showInfoWindow();

        // Логируем результат
        android.util.Log.d("SmartSearch",
                "📍 Показан результат: " + title + " (" +
                        address.getLocality() + ", " + address.getCountryName() + ")");
    }

    // ⭐ НОВЫЙ МЕТОД: Получаем текущую локацию пользователя
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());

                        // Определяем город и страну
                        updateCityAndCountry(location);

                        android.util.Log.d("AddLocationFragment",
                                "📍 Текущая локация: " + currentCity + ", " + currentCountry);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AddLocationFragment", "❌ Не удалось получить локацию: " + e.getMessage());
                });
    }

    // ⭐ НОВЫЙ МЕТОД: Определяем город и страну по координатам
    private void updateCityAndCountry(Location location) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Получаем город
                String locality = address.getLocality();
                if (locality != null && !locality.isEmpty()) {
                    currentCity = locality;
                } else {
                    // Если locality пустой, пробуем subAdminArea
                    String subAdminArea = address.getSubAdminArea();
                    if (subAdminArea != null && !subAdminArea.isEmpty()) {
                        currentCity = subAdminArea;
                    }
                }

                // Получаем страну
                String country = address.getCountryName();
                if (country != null && !country.isEmpty()) {
                    currentCountry = country;
                }
            }
        } catch (IOException e) {
            android.util.Log.e("AddLocationFragment", "❌ Ошибка геокодинга: " + e.getMessage());
        }
    }

    // === НАСТРОЙКА СЛАЙДЕРА (УНИВЕРСАЛЬНАЯ ВЕРСИЯ) ===
    private void setupSlider() {
        // Слайдер ВСЕГДА работает в метрах (внутренний формат)
        sliderRadius.setValueFrom(100);
        sliderRadius.setValueTo(2000);
        sliderRadius.setStepSize(50);
        sliderRadius.setValue(currentRadiusMeters);

        // Обновляем текст в зависимости от выбранных единиц
        updateRadiusText();
    }

    // === ОБНОВЛЕНИЕ ТЕКСТА РАДИУСА ===
    private void updateRadiusText() {
        tvRadiusValue.setText(SettingsFragment.formatDistance(requireContext(), currentRadiusMeters));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        LatLng chisinau = new LatLng(47.0105, 28.8638);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chisinau, 12));

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Просто включаем "Моя локация" без запроса (разрешение уже получено)
        try {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }



        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.setOnMapClickListener(latLng -> {
            if (selectedMarker != null) {
                selectedMarker.remove();
            }
            if (radiusCircle != null) {
                radiusCircle.remove();
            }

            String address = getAddressFromLatLng(latLng);
            String[] addressParts = splitAddress(address);

            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(addressParts[0])
                    .snippet(addressParts[1])
                    .draggable(true));

            radiusCircle = mMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(currentRadiusMeters)
                    .strokeColor(Color.parseColor("#8D6E63"))
                    .strokeWidth(3f)
                    .fillColor(Color.parseColor("#40D7CCC8")));

            if (selectedMarker != null) {
                selectedMarker.showInfoWindow();
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                if (radiusCircle != null) {
                    radiusCircle.setCenter(marker.getPosition());
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng newPosition = marker.getPosition();
                String address = getAddressFromLatLng(newPosition);
                String[] addressParts = splitAddress(address);

                marker.setTitle(addressParts[0]);
                marker.setSnippet(addressParts[1]);
                marker.showInfoWindow();

                if (radiusCircle != null) {
                    radiusCircle.setCenter(newPosition);
                }
            }
        });
    }

    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private View mWindow;

        CustomInfoWindowAdapter() {
            mWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            TextView title = mWindow.findViewById(R.id.info_title);
            TextView snippet = mWindow.findViewById(R.id.info_snippet);

            title.setText(marker.getTitle());
            snippet.setText(marker.getSnippet());

            return mWindow;
        }
    }

    private String[] splitAddress(String fullAddress) {
        String[] result = new String[2];

        String[] parts = fullAddress.split("\n", 2);

        if (parts.length >= 2) {
            result[0] = parts[0];
            result[1] = parts[1];
        } else {
            result[0] = fullAddress;
            result[1] = "";
        }

        return result;
    }

    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                StringBuilder fullAddress = new StringBuilder();

                String featureName = address.getFeatureName();
                if (featureName != null && !featureName.matches("^\\d.*")) {
                    fullAddress.append(featureName);
                }

                String houseNumber = address.getSubThoroughfare();
                String streetName = address.getThoroughfare();

                if (streetName != null) {
                    if (fullAddress.length() > 0) {
                        fullAddress.append("\n");
                    }

                    fullAddress.append(streetName);

                    if (houseNumber != null) {
                        fullAddress.append(", ").append(houseNumber);
                    }
                } else if (houseNumber != null) {
                    if (fullAddress.length() > 0) {
                        fullAddress.append("\n");
                    }
                    fullAddress.append(houseNumber);
                }

                String city = address.getLocality();
                if (city != null) {
                    if (fullAddress.length() > 0) {
                        fullAddress.append("\n");
                    }
                    fullAddress.append(city);
                }

                String country = address.getCountryName();
                if (country != null) {
                    if (fullAddress.length() > 0) {
                        fullAddress.append(", ");
                    }
                    fullAddress.append(country);
                }

                return fullAddress.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Lat: " + String.format("%.4f", latLng.latitude) +
                ", Lng: " + String.format("%.4f", latLng.longitude);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем только отображение текста
        updateRadiusText();
    }
}