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

    //радиус хранится в метрах
    private float currentRadiusMeters = 500f;

    //объект для получения текущей локации
    private FusedLocationProviderClient fusedLocationClient;

    //координаты и данные пользователя
    private LatLng currentUserLocation;
    private String currentCity = "Chisinau";
    private String currentCountry = "Moldova";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //инициализация вью и элементов
        View view = inflater.inflate(R.layout.fragment_add_location, container, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        btnAddLocation = view.findViewById(R.id.btn_add_location);
        btnCancel = view.findViewById(R.id.btn_cancel);
        sliderRadius = view.findViewById(R.id.slider_radius);
        tvRadiusValue = view.findViewById(R.id.tv_radius_value);
        SearchView searchView = view.findViewById(R.id.search_view);

        //настройка цвета текста и иконки поиска
        int searchSrcTextId = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        EditText searchEditText = searchView.findViewById(searchSrcTextId);
        searchEditText.setTextColor(Color.parseColor("#939df5"));
        searchEditText.setHintTextColor(Color.parseColor("#a4adfc"));
        int searchMagIconId = searchView.getContext().getResources().getIdentifier("android:id/search_mag_icon", null, null);
        ImageView searchIcon = searchView.findViewById(searchMagIconId);
        searchIcon.setColorFilter(Color.parseColor("#8692f7"), PorterDuff.Mode.SRC_IN);

        //настройка слайдера радиуса
        setupSlider();

        //обработка поиска адреса
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query == null || query.trim().isEmpty()) {
                    Toast.makeText(getContext(), getString(R.string.add_location_enter_address), Toast.LENGTH_SHORT).show();
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

        //обновление радиуса при перемещении слайдера
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            currentRadiusMeters = value;
            updateRadiusText();
            if (radiusCircle != null && selectedMarker != null) {
                radiusCircle.setRadius(currentRadiusMeters);
            }
        });

        //инициализация карты
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        //обработка кнопки отмены
        btnCancel.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).replaceFragment(new HomeFragment(), true);
            }
        });

        //обработка кнопки добавления локации
        btnAddLocation.setOnClickListener(v -> {
            if (selectedMarker != null) {
                LatLng position = selectedMarker.getPosition();
                String address = getAddressFromLatLng(position);
                AppDatabase db = AppDatabase.getInstance(requireContext());
                String[] addressParts = splitAddress(address);
                String locationName = addressParts[0];
                String fullAddress = addressParts[1].isEmpty() ? address : addressParts[1];

                //сохраняем объект локации в базу
                com.example.sleepytrip.Location location = new com.example.sleepytrip.Location(
                        locationName, fullAddress, position.latitude, position.longitude, currentRadiusMeters);
                db.locationDao().insert(location);

                Toast.makeText(getContext(), getString(R.string.add_location_saved), Toast.LENGTH_SHORT).show();
                ((MainActivity) getActivity()).replaceFragment(new HomeFragment(), true);
            } else {
                Toast.makeText(getContext(), getString(R.string.add_location_select_prompt), Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    //выполняет умный поиск адреса
    private void performSmartSearch(String query) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> globalResults = geocoder.getFromLocationName(query, 10);
            if (globalResults == null || globalResults.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.add_location_not_found), Toast.LENGTH_SHORT).show();
                return;
            }
            Address bestMatch = findBestMatch(globalResults, query);
            if (bestMatch != null) showLocationOnMap(bestMatch, query);
            else Toast.makeText(getContext(), getString(R.string.add_location_not_found), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), getString(R.string.add_location_error), Toast.LENGTH_SHORT).show();
        }
    }

    //ищет наилучшее совпадение среди результатов
    private Address findBestMatch(List<Address> addresses, String query) {
        if (addresses == null || addresses.isEmpty()) return null;
        String queryLower = query.toLowerCase().trim();
        Address sameCountryMatch = null;
        Address sameCityMatch = null;
        Address majorCityMatch = null;
        Address firstResult = addresses.get(0);

        for (Address addr : addresses) {
            String addrCountry = addr.getCountryName();
            String addrCity = addr.getLocality();
            String addrAdminArea = addr.getAdminArea();

            boolean queryContainsCity = addrCity != null && queryLower.contains(addrCity.toLowerCase());
            boolean queryContainsRegion = addrAdminArea != null && queryLower.contains(addrAdminArea.toLowerCase());

            if (addrCity != null && addrCity.equalsIgnoreCase(currentCity) && sameCityMatch == null)
                sameCityMatch = addr;

            if (addrCountry != null && addrCountry.equalsIgnoreCase(currentCountry)) {
                if (sameCountryMatch == null) sameCountryMatch = addr;
                if (queryContainsCity || queryContainsRegion) return addr;
            }

            if (queryContainsCity && addrCity != null && !addrCity.equalsIgnoreCase(currentCity) && majorCityMatch == null)
                majorCityMatch = addr;
        }

        if (sameCityMatch != null) return sameCityMatch;
        if (majorCityMatch != null) return majorCityMatch;
        if (sameCountryMatch != null) return sameCountryMatch;
        return firstResult;
    }

    //отображает найденную локацию на карте
    private void showLocationOnMap(Address address, String originalQuery) {
        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
        float zoomLevel = 15f;
        if (address.getLocality() != null &&
                (originalQuery.equalsIgnoreCase(address.getLocality()) ||
                        originalQuery.toLowerCase().contains(address.getLocality().toLowerCase())))
            zoomLevel = 12f;

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
        if (selectedMarker != null) selectedMarker.remove();
        if (radiusCircle != null) radiusCircle.remove();

        String title = address.getFeatureName() != null ? address.getFeatureName() : originalQuery;
        String snippet = address.getAddressLine(0);

        selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(snippet).draggable(true));
        radiusCircle = mMap.addCircle(new CircleOptions().center(latLng).radius(currentRadiusMeters)
                .strokeColor(Color.parseColor("#8D6E63")).strokeWidth(3f).fillColor(Color.parseColor("#40D7CCC8")));
        selectedMarker.showInfoWindow();
    }

    //настраивает диапазон и шаг слайдера
    private void setupSlider() {
        sliderRadius.setValueFrom(100);
        sliderRadius.setValueTo(2000);
        sliderRadius.setStepSize(50);
        sliderRadius.setValue(currentRadiusMeters);
        updateRadiusText();
    }

    //обновляет текст текущего радиуса
    private void updateRadiusText() {
        tvRadiusValue.setText(SettingsFragment.formatDistance(requireContext(), currentRadiusMeters));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        //инициализация карты
        mMap = googleMap;
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
        LatLng chisinau = new LatLng(47.0105, 28.8638);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chisinau, 12));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        try {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        } catch (SecurityException ignored) {}

        //обработка кликов по карте
        mMap.setOnMapClickListener(latLng -> {
            if (selectedMarker != null) selectedMarker.remove();
            if (radiusCircle != null) radiusCircle.remove();

            String address = getAddressFromLatLng(latLng);
            String[] addressParts = splitAddress(address);

            selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                    .title(addressParts[0]).snippet(addressParts[1]).draggable(true));
            radiusCircle = mMap.addCircle(new CircleOptions().center(latLng)
                    .radius(currentRadiusMeters).strokeColor(Color.parseColor("#8D6E63"))
                    .strokeWidth(3f).fillColor(Color.parseColor("#40D7CCC8")));
            selectedMarker.showInfoWindow();
        });

        //обработка перетаскивания маркера
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override public void onMarkerDragStart(Marker marker) {}
            @Override public void onMarkerDrag(Marker marker) {
                if (radiusCircle != null) radiusCircle.setCenter(marker.getPosition());
            }
            @Override public void onMarkerDragEnd(Marker marker) {
                LatLng newPosition = marker.getPosition();
                String address = getAddressFromLatLng(newPosition);
                String[] addressParts = splitAddress(address);
                marker.setTitle(addressParts[0]);
                marker.setSnippet(addressParts[1]);
                marker.showInfoWindow();
                if (radiusCircle != null) radiusCircle.setCenter(newPosition);
            }
        });
    }

    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private View mWindow;
        CustomInfoWindowAdapter() {
            //создаёт кастомное окно для маркера
            mWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
        }
        @Override public View getInfoWindow(Marker marker) { return null; }
        @Override public View getInfoContents(Marker marker) {
            TextView title = mWindow.findViewById(R.id.info_title);
            TextView snippet = mWindow.findViewById(R.id.info_snippet);
            title.setText(marker.getTitle());
            snippet.setText(marker.getSnippet());
            return mWindow;
        }
    }

    //делит адрес на заголовок и дополнительную часть
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

    //получает адрес по координатам
    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder fullAddress = new StringBuilder();
                String featureName = address.getFeatureName();
                if (featureName != null && !featureName.matches("^\\d.*")) fullAddress.append(featureName);
                String houseNumber = address.getSubThoroughfare();
                String streetName = address.getThoroughfare();
                if (streetName != null) {
                    if (fullAddress.length() > 0) fullAddress.append("\n");
                    fullAddress.append(streetName);
                    if (houseNumber != null) fullAddress.append(", ").append(houseNumber);
                } else if (houseNumber != null) {
                    if (fullAddress.length() > 0) fullAddress.append("\n");
                    fullAddress.append(houseNumber);
                }
                String city = address.getLocality();
                if (city != null) {
                    if (fullAddress.length() > 0) fullAddress.append("\n");
                    fullAddress.append(city);
                }
                String country = address.getCountryName();
                if (country != null) {
                    if (fullAddress.length() > 0) fullAddress.append(", ");
                    fullAddress.append(country);
                }
                return fullAddress.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Lat: " + String.format("%.4f", latLng.latitude) + ", Lng: " + String.format("%.4f", latLng.longitude);
    }

    @Override
    public void onResume() {
        super.onResume();
        //обновляет отображение текста радиуса
        updateRadiusText();
    }
}
