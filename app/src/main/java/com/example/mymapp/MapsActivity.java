package com.example.mymapp;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.mymapp.databinding.ActivityMainBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMainBinding binding;
    private AutoCompleteTextView searchAutoComplete;
    private Geocoder geocoder;
    private ArrayAdapter<String> adapter;
    private List<Address> addressList;
    private PlacesClient placesClient;
    private List<AutocompletePrediction> predictionList = new ArrayList<>();
    private AutocompleteSessionToken sessionToken;

    private List<Address> currentResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        searchAutoComplete = findViewById(R.id.search_location);
      /*  binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        searchEditText = findViewById(R.id.search_location);*/
        geocoder = new Geocoder(this, Locale.getDefault());
        addressList = new ArrayList<>();
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyDBCV4xnM_DfLFVAXjDUUbNYQ4iOZQe-CU", Locale.getDefault());
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();
        // Khởi tạo bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    1001);
        }

        // Lắng nghe sự kiện khi gõ text (text change)
        searchAutoComplete.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 2) {
                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setSessionToken(sessionToken)
                            .setQuery(s.toString())
                            .build();

                    placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener(response -> {
                                predictionList = response.getAutocompletePredictions();
                                PredictionAdapter predictionAdapter = new PredictionAdapter(MapsActivity.this, predictionList, s.toString());
                                searchAutoComplete.setAdapter(predictionAdapter);
                                predictionAdapter.notifyDataSetChanged();
                                searchAutoComplete.showDropDown();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MapsActivity.this, "Không thể lấy gợi ý", Toast.LENGTH_SHORT).show();
                            });
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        // Khi người dùng chọn một địa chỉ từ gợi ý
        searchAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                AutocompletePrediction prediction = predictionList.get(position);
                String placeId = prediction.getPlaceId();

                List<Place.Field> placeFields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME);
                FetchPlaceRequest placeRequest = FetchPlaceRequest.builder(placeId, placeFields).build();

                placesClient.fetchPlace(placeRequest).addOnSuccessListener(fetchPlaceResponse -> {
                    Place place = fetchPlaceResponse.getPlace();
                    LatLng latLng = place.getLatLng();
                    if (latLng != null) {
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    }
                });

            }
        });

        // Xử lý sự kiện nút search (ví dụ nút Search hoặc bàn phím enter)
        searchAutoComplete.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String query = searchAutoComplete.getText().toString();
                searchAndShowRoute(query);
                return true;
            }
            return false;
        });
    }

    // Hàm lấy danh sách gợi ý địa điểm bằng Geocoder
    private void updateSuggestions(String query) {
        if (!Places.isInitialized()) return;

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build();

        Places.createClient(this).findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    adapter.clear();
                    addressList.clear(); // nếu muốn lưu kết quả

                    for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                        adapter.add(prediction.getFullText(null).toString());
                    }

                    adapter.notifyDataSetChanged();
                    searchAutoComplete.showDropDown();
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(this, "Không thể lấy gợi ý", Toast.LENGTH_SHORT).show();
                });
    }

    // Hàm tìm địa điểm và mở Google Maps chỉ đường từ vị trí hiện tại tới địa chỉ đã nhập
    private void searchAndShowRoute(String query) {
        if (mMap == null) return;

        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses == null || addresses.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy địa chỉ", Toast.LENGTH_SHORT).show();
                return;
            }

            Address address = addresses.get(0);
            LatLng destination = new LatLng(address.getLatitude(), address.getLongitude());

            // Lấy vị trí hiện tại
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double startLat = location.getLatitude();
                    double startLng = location.getLongitude();

                    // Tạo intent mở Google Maps chỉ đường
                    String uri = String.format(Locale.ENGLISH,
                            "https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f&travelmode=driving",
                            startLat, startLng, destination.latitude, destination.longitude);

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    intent.setPackage("com.google.android.apps.maps");
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Không lấy được vị trí hiện tại", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tìm kiếm địa chỉ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Có thể thêm marker mặc định hoặc xin permission vị trí ở đây
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền vị trí", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Ứng dụng cần quyền vị trí để hoạt động", Toast.LENGTH_LONG).show();
            }
        }
    }
}
