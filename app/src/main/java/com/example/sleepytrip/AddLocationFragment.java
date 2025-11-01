package com.example.sleepytrip;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
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

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_location, container, false);

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

                String fullQuery = query + ", Chisinau, Moldova";
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());

                try {
                    List<Address> addresses = geocoder.getFromLocationName(fullQuery, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                        if (selectedMarker != null) selectedMarker.remove();
                        if (radiusCircle != null) radiusCircle.remove();

                        selectedMarker = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(address.getFeatureName() != null ? address.getFeatureName() : query)
                                .snippet(address.getAddressLine(0))
                                .draggable(true));

                        radiusCircle = mMap.addCircle(new CircleOptions()
                                .center(latLng)
                                .radius(currentRadiusMeters)
                                .strokeColor(Color.parseColor("#8D6E63"))
                                .strokeWidth(3f)
                                .fillColor(Color.parseColor("#40D7CCC8")));

                        selectedMarker.showInfoWindow();
                    } else {
                        Toast.makeText(getContext(), "Адрес не найден в Кишинёве", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Ошибка при поиске адреса", Toast.LENGTH_SHORT).show();
                }

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
                Location location = new Location(
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

    // === КОНВЕРТАЦИЯ ЗНАЧЕНИЯ СЛАЙДЕРА В МЕТРЫ ===
    private float convertSliderValueToMeters(float sliderValue) {
        String units = SettingsFragment.getUnits(requireContext());

        if ("miles".equals(units)) {
            // Слайдер в футах -> конвертируем в метры
            return sliderValue / 3.28084f;
        } else {
            // Слайдер уже в метрах
            return sliderValue;
        }
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

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(getContext(),
                        "Location permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем только отображение текста
        updateRadiusText();
    }
}