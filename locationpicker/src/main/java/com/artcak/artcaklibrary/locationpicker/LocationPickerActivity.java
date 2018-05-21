package com.artcak.artcaklibrary.locationpicker;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.databinding.DataBindingUtil;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.artcak.artcaklibrary.R;
import com.artcak.artcaklibrary.abstractClass.GeneralActivity;
import com.artcak.artcaklibrary.databinding.ActivityLocationPickerBinding;
import com.artcak.artcaklibrary.locationpicker.geocoder.AddressBuilder;
import com.artcak.artcaklibrary.locationpicker.geocoder.AndroidGeocoderDataSource;
import com.artcak.artcaklibrary.locationpicker.geocoder.GeocoderPresenter;
import com.artcak.artcaklibrary.locationpicker.geocoder.GeocoderRepository;
import com.artcak.artcaklibrary.locationpicker.geocoder.GeocoderViewInterface;
import com.artcak.artcaklibrary.locationpicker.geocoder.GoogleGeocoderDataSource;
import com.artcak.artcaklibrary.locationpicker.geocoder.GooglePlacesDataSource;
import com.artcak.artcaklibrary.locationpicker.geocoder.NetworkClient;
import com.artcak.artcaklibrary.tools.PermissionUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.charmas.android.reactivelocation2.ReactiveLocationProvider;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;

public class LocationPickerActivity extends GeneralActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, GoogleMap.OnMapLongClickListener,
        GeocoderViewInterface, GoogleMap.OnMapClickListener{


    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ZIPCODE = "zipcode";
    public static final String ADDRESS = "address";
    public static final String LOCATION_ADDRESS = "location_address";
    public static final String TRANSITION_BUNDLE = "transition_bundle";
    public static final String LAYOUTS_TO_HIDE = "layouts_to_hide";
    public static final String SEARCH_ZONE = "search_zone";
    public static final String BACK_PRESSED_RETURN_OK = "back_pressed_return_ok";
    public static final String ENABLE_SATELLITE_VIEW = "enable_satellite_view";
    public static final String ENABLE_LOCATION_PERMISSION_REQUEST = "enable_location_permission_request";
    public static final String ENABLE_GOOGLE_PLACES = "enable_google_places";
    public static final String POIS_LIST = "pois_list";
    public static final String LEKU_POI = "leku_poi";
    private static final String GEOLOC_API_KEY = "geoloc_api_key";
    private static final String LOCATION_KEY = "location_key";
    private static final String LAST_LOCATION_QUERY = "last_location_query";
    private static final String OPTIONS_HIDE_STREET = "street";
    private static final String OPTIONS_HIDE_CITY = "city";
    private static final String OPTIONS_HIDE_ZIPCODE = "zipcode";

    private static final int REQUEST_PLACE_PICKER = 6655;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int DEFAULT_ZOOM = 16;
    private static final int WIDER_ZOOM = 6;
    private static final int MIN_CHARACTERS = 2;
    private static final int DEBOUNCE_TIME = 400;

    private GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location currentLocation;
    private GeocoderPresenter geocoderPresenter;
    private GoogleGeocoderDataSource apiInteractor;

    private ArrayAdapter<String> adapter;
    private List<String> locationNameList = new ArrayList<>();
    private final List<Address> locationList = new ArrayList<>();
    private Address selectedAddress;
    private Marker currentMarker;
    private TextWatcher textWatcher;
    private boolean hasWiderZoom = false;
    private String searchZone;
    private Bundle bundle = new Bundle();

    ActivityLocationPickerBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this,R.layout.activity_location_picker);
    }

    private void custom(Bundle savedInstanceState){
        setUpMainVariables();
        setUpResultsList();
        setUpToolBar();
        updateValuesFromBundle(savedInstanceState);
        checkLocationPermission();
        setUpSearchView();
        setUpMapIfNeeded();
        setUpFloatingButtons();
        buildGoogleApiClient();
        track(TrackEvents.didLoadLocationPicker);
    }

    private void setUpMainVariables() {
        GooglePlacesDataSource placesDataSource = new GooglePlacesDataSource(Places.getGeoDataClient(this, null));
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        apiInteractor = new GoogleGeocoderDataSource(new NetworkClient(), new AddressBuilder());
//        GeocoderRepository geocoderRepository = new GeocoderRepository(new AndroidGeocoderDataSource(geocoder), apiInteractor);
        GeocoderRepository geocoderRepository = new GeocoderRepository(apiInteractor,new AndroidGeocoderDataSource(geocoder));
        geocoderPresenter = new GeocoderPresenter(new ReactiveLocationProvider(getApplicationContext()), geocoderRepository, placesDataSource);
        geocoderPresenter.setUI(this);
        binding.ivSearchClear.setOnClickListener(view -> {
            binding.etSearch.setText("");
        });
        locationNameList = new ArrayList<>();
    }


    private void setUpResultsList() {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, locationNameList);
        binding.lvSearchResult.setAdapter(adapter);
        binding.lvSearchResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                setNewLocation(locationList.get(i));
                changeListResultVisibility(View.GONE);
                closeKeyboard();
            }
        });
    }

    private void setUpToolBar() {
        setSupportActionBar(binding.toolbarSearch);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void checkLocationPermission() {
        if (PermissionUtils.shouldRequestLocationStoragePermission(getApplicationContext())) {
            PermissionUtils.requestLocationPermission(this);
        }
    }

    private void setUpSearchView() {
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                retrieveLocationFrom(v.getText().toString());
                closeKeyboard();
                handled = true;
            }
            return handled;
        });
        textWatcher = getSearchTextWatcher();
        binding.etSearch.addTextChangedListener(textWatcher);
    }

    private TextWatcher getSearchTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                if ("".equals(charSequence.toString())) {
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                    binding.ivSearchClear.setVisibility(View.INVISIBLE);
                } else {
                    binding.ivSearchClear.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                retrieveLocationWithDebounceTimeFrom(binding.etSearch.getText().toString());
            }
        };
    }

    private void setUpMapIfNeeded() {
        if (map == null) {
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragmentMap)).getMapAsync(this);
        }
    }

    private void retrieveLocationFrom(String query) {
        if (searchZone != null && !searchZone.isEmpty()) {
            retrieveLocationFromZone(query, searchZone);
        } else {
            retrieveLocationFromDefaultZone(query);
        }
    }

    private void retrieveLocationWithDebounceTimeFrom(String query) {
        if (searchZone != null && !searchZone.isEmpty()) {
            retrieveDebouncedLocationFromZone(query, searchZone, DEBOUNCE_TIME);
        } else {
            retrieveDebouncedLocationFromDefaultZone(query, DEBOUNCE_TIME);
        }
    }

    private void retrieveLocationFromDefaultZone(String query) {
        if (CountryLocaleRect.getDefaultLowerLeft() != null) {
            geocoderPresenter.getFromLocationName(query, CountryLocaleRect.getDefaultLowerLeft(),
                    CountryLocaleRect.getDefaultUpperRight());
        } else {
            geocoderPresenter.getFromLocationName(query);
        }
    }

    private void retrieveLocationFromZone(String query, String zoneKey) {
        Locale locale = new Locale(zoneKey);
        if (CountryLocaleRect.getLowerLeftFromZone(locale) != null) {
            geocoderPresenter.getFromLocationName(query, CountryLocaleRect.getLowerLeftFromZone(locale),
                    CountryLocaleRect.getUpperRightFromZone(locale));
        } else {
            geocoderPresenter.getFromLocationName(query);
        }
    }

    private void retrieveDebouncedLocationFromDefaultZone(String query, int debounceTime) {
        if (CountryLocaleRect.getDefaultLowerLeft() != null) {
            geocoderPresenter.getDebouncedFromLocationName(query, CountryLocaleRect.getDefaultLowerLeft(),
                    CountryLocaleRect.getDefaultUpperRight(), debounceTime);
        } else {
            geocoderPresenter.getDebouncedFromLocationName(query, debounceTime);
        }
    }

    private void retrieveDebouncedLocationFromZone(String query, String zoneKey, int debounceTime) {
        Locale locale = new Locale(zoneKey);
        if (CountryLocaleRect.getLowerLeftFromZone(locale) != null) {
            geocoderPresenter.getDebouncedFromLocationName(query, CountryLocaleRect.getLowerLeftFromZone(locale),
                    CountryLocaleRect.getUpperRightFromZone(locale), debounceTime);
        } else {
            geocoderPresenter.getDebouncedFromLocationName(query, debounceTime);
        }
    }

    private void setUpFloatingButtons() {
        binding.fabMyLocation.setOnClickListener(v -> {
            geocoderPresenter.getLastKnownLocation();
            track(TrackEvents.didLocalizeMe);
        });
        binding.fabAccept.setOnClickListener(v -> returnCurrentPosition());
    }

    private void returnCurrentPosition() {
        if (currentLocation != null) {
            Log.i("debugs","LocationPickerActivity LATITUDE: "+currentLocation.getLatitude());
            Log.i("debugs","LocationPickerActivity LONGITUDE: "+currentLocation.getLongitude());
            Log.i("debugs","LocationPickerActivity ADDRESS: "+binding.tvInfoStreet.getText()+" "+binding.tvInfoCity.getText());
            Intent returnIntent = new Intent();
            returnIntent.putExtra(LATITUDE, currentLocation.getLatitude());
            returnIntent.putExtra(LONGITUDE, currentLocation.getLongitude());
            returnIntent.putExtra(ADDRESS, binding.tvInfoStreet.getText()+" "+binding.tvInfoCity.getText());
            returnIntent.putExtra(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE));
            setResult(RESULT_OK, returnIntent);
            track(TrackEvents.RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
            track(TrackEvents.CANCEL);
        }
        finish();
    }

    protected void track(TrackEvents event) {
        LocationPicker.getTracker().onEventTracked(event);
    }

    private synchronized void buildGoogleApiClient() {
        GoogleApiClient.Builder googleApiClientBuilder = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API);
        googleApiClientBuilder.addApi(Places.GEO_DATA_API);
        googleApiClient = googleApiClientBuilder.build();
        googleApiClient.connect();
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Bundle transitionBundle = getIntent().getExtras();
        if (transitionBundle != null) {
            getTransitionBundleParams(transitionBundle);
        }
        if (savedInstanceState != null) {
            getSavedInstanceParams(savedInstanceState);
        }
        geocoderPresenter.enableGooglePlaces();
    }

    private void getSavedInstanceParams(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(TRANSITION_BUNDLE)) {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState.getBundle(TRANSITION_BUNDLE));
        } else {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState);
        }
        if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
            currentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
        }
        setUpDefaultMapLocation();
        if (savedInstanceState.keySet().contains(GEOLOC_API_KEY)) {
            apiInteractor.setApiKey(savedInstanceState.getString(GEOLOC_API_KEY));
        }
        if (savedInstanceState.keySet().contains(SEARCH_ZONE)) {
            searchZone = savedInstanceState.getString(SEARCH_ZONE);
        }
    }
    private void getTransitionBundleParams(Bundle transitionBundle){
        bundle.putBundle(TRANSITION_BUNDLE, transitionBundle);
        if (transitionBundle.keySet().contains(LATITUDE) && transitionBundle.keySet()
                .contains(LONGITUDE)) {
            setLocationFromBundle(transitionBundle);
        }
        if (transitionBundle.keySet().contains(SEARCH_ZONE)) {
            searchZone = transitionBundle.getString(SEARCH_ZONE);
        }
    }

    private void setLocationFromBundle(Bundle transitionBundle) {
        if (currentLocation == null) {
            currentLocation = new Location("network");
        }
        currentLocation.setLatitude(transitionBundle.getDouble(LATITUDE));
        currentLocation.setLongitude(transitionBundle.getDouble(LONGITUDE));
        setCurrentPositionLocation();
    }

    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search by voice");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                "en-EN");

        if (checkPlayServices()) {
            try {
                startActivityForResult(intent, REQUEST_PLACE_PICKER);
            } catch (ActivityNotFoundException e) {
                track(TrackEvents.startVoiceRecognitionActivityFailed);
            }
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(getApplicationContext());
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, CONNECTION_FAILURE_RESOLUTION_REQUEST).show();
            }
            return false;
        }
        return true;
    }

    private void setNewLocation(Address address) {
        this.selectedAddress = address;
        if (currentLocation == null) {
            currentLocation = new Location("network");
        }

        currentLocation.setLatitude(address.getLatitude());
        currentLocation.setLongitude(address.getLongitude());
        setNewMapMarker(new LatLng(address.getLatitude(), address.getLongitude()));
        setLocationInfo(address);
        binding.etSearch.setText("");
    }

    private void setLocationInfo(Address address) {
        Log.i("debugs","setLocationInfo addres: "+address);
        if (address.getAddressLine(0)!= null && !address.getAddressLine(0).equals("")){
            binding.tvInfoStreet.setText(address.getAddressLine(0));
        }
        String infoCity = "";
        if (address.getSubAdminArea()!=null && !address.getSubAdminArea().equals("")){
            infoCity = address.getSubAdminArea();
        }
        if (address.getAdminArea()!=null && !address.getAdminArea().equals("")){
            infoCity = infoCity+", "+address.getAdminArea();
        }
        binding.tvInfoCity.setText(infoCity);
        binding.tvInfoCoordinate.setText(String.format("%.4f", address.getLatitude())+" , "+String.format("%.4f", address.getLongitude()));
        closeKeyboard();
    }
    private void setNewMapMarker(LatLng latLng) {
        if (map != null) {
            if (currentMarker != null) {
                currentMarker.remove();
            }
            float zoomValue = getDefaultZoom();
            if (map.getCameraPosition().zoom>zoomValue){
                zoomValue = map.getCameraPosition().zoom;
            }
            CameraPosition cameraPosition =
                    new CameraPosition.Builder().target(latLng)
                            .zoom(zoomValue)
                            .build();
            hasWiderZoom = false;
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            currentMarker = addMarker(latLng);
            map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                }

                @Override
                public void onMarkerDrag(Marker marker) {
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    if (currentLocation == null) {
                        currentLocation = new Location("network");
                    }
                    currentLocation.setLongitude(marker.getPosition().longitude);
                    currentLocation.setLatitude(marker.getPosition().latitude);
                    setCurrentPositionLocation();
                }
            });
        }
    }
    private void setCurrentPositionLocation() {
        if (currentLocation != null) {
            setNewMapMarker(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
            geocoderPresenter.getInfoFromLocation(new LatLng(currentLocation.getLatitude(),
                    currentLocation.getLongitude()));
        }else{
        }
    }

    private int getDefaultZoom() {
        int zoom;
        if (hasWiderZoom) {
            zoom = WIDER_ZOOM;
        } else {
            zoom = DEFAULT_ZOOM;
        }
        return zoom;
    }

    private void changeListResultVisibility(int visibility) {
        binding.lvSearchResult.setVisibility(visibility);
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        binding.etSearch.clearFocus();
//        binding.etSearch.setFocusableInTouchMode(false);
//        binding.etSearch.setFocusable(false);
    }
    private Marker addMarker(LatLng latLng) {
        return map.addMarker(new MarkerOptions().position(latLng).draggable(true));
    }

    @Override
    public void willLoadLocation() {
        binding.pbLoading.setVisibility(View.VISIBLE);
        changeListResultVisibility(View.GONE);
    }

    @Override
    public void showLocations(List<Address> addresses) {
        if (addresses != null) {
            fillLocationList(addresses);
            if (addresses.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Pencarian tidak ditemukan", Toast.LENGTH_LONG)
                        .show();
            } else {
                updateLocationNameList(addresses);
                if (hasWiderZoom) {
                    binding.etSearch.setText("");
                }
                if (addresses.size() == 1) {
                    setNewLocation(addresses.get(0));
                }
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void showDebouncedLocations(List<Address> addresses) {
        if (addresses != null) {
            fillLocationList(addresses);
            if (!addresses.isEmpty()) {
                updateLocationNameList(addresses);
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void didLoadLocation() {
        binding.pbLoading.setVisibility(View.GONE);
        changeListResultVisibility(locationList.size() >= 1 ? View.VISIBLE : View.GONE);
//        if (locationList.size() > 0 && locationList.get(0) != null) {
//            Log.i("debugs","didLoadLocation VISIBLE");
//            changeLocationInfoLayoutVisibility(View.VISIBLE);
//        } else {
//            changeLocationInfoLayoutVisibility(View.GONE);
//            Log.i("debugs","didLoadLocation GONE");
//        }
        changeLocationInfoLayoutVisibility(View.VISIBLE);
        track(TrackEvents.didSearchLocations);
    }

    @Override
    public void showLoadLocationError() {
        binding.pbLoading.setVisibility(View.GONE);
        changeListResultVisibility(View.GONE);
        Toast.makeText(this, "Terjadi kesalahan, silahkan coba lagi", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLastLocation(Location location) {
        currentLocation = location;
    }

    @Override
    public void didGetLastLocation() {
        if (currentLocation != null) {
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, "Layanan tidak ditemukan", Toast.LENGTH_LONG).show();
                return;
            }
            setUpMapIfNeeded();
        }
        setUpDefaultMapLocation();
    }

    @Override
    public void showLocationInfo(List<Address> addresses) {
        if (addresses != null) {
            if (addresses.size() > 0 && addresses.get(0) != null) {
                selectedAddress = addresses.get(0);
                setLocationInfo(selectedAddress);
            } else {
                setLocationEmpty();
            }
        }
    }

    @Override
    public void willGetLocationInfo(LatLng latLng) {
        binding.tvInfoCoordinate.setText(String.format("%.4f", latLng.latitude)+" , "+String.format("%.4f", latLng.longitude));
    }

    @Override
    public void didGetLocationInfo() {

    }

    @Override
    public void showGetLocationInfoError() {
        setLocationEmpty();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (currentLocation == null) {
            geocoderPresenter.getLastKnownLocation();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                track(TrackEvents.googleApiConnectionFailed);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        setNewPosition(latLng);
        track(TrackEvents.simpleDidLocalizeByPoi);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        setNewPosition(latLng);
        track(TrackEvents.didLocalizeByPoi);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (map == null) {
            map = googleMap;
            setDefaultMapSettings();
            setCurrentPositionLocation();
        }
    }

    public void setLocationEmpty() {
        binding.tvInfoCity.setText("");
        binding.tvInfoCoordinate.setText("");
        binding.tvInfoStreet.setText("");
    }

    private void setDefaultMapSettings() {
        if (map != null) {
            map.setMapType(MAP_TYPE_NORMAL);
            map.setOnMapLongClickListener(this);
            map.setOnMapClickListener(this);
            map.getUiSettings().setCompassEnabled(false);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            map.getUiSettings().setMapToolbarEnabled(false);
        }
    }
    private void setNewPosition(LatLng latLng) {
        if (currentLocation == null) {
            currentLocation = new Location("network");
            Log.i("debugs","setNewPosition currentLocation is null");
        }else{
            Log.i("debugs","setNewPosition currentLocation is not null");
        }
        currentLocation.setLatitude(latLng.latitude);
        currentLocation.setLongitude(latLng.longitude);
        setCurrentPositionLocation();
    }

    private void setUpDefaultMapLocation() {
        if (currentLocation != null) {
            setCurrentPositionLocation();
        } else {
            retrieveLocationFrom(Locale.getDefault().getDisplayCountry());
            hasWiderZoom = true;
        }
    }

    private void changeLocationInfoLayoutVisibility(int visibility) {
        binding.flInfoLocation.setVisibility(visibility);
    }

    private void fillLocationList(List<Address> addresses) {
        locationList.clear();
        locationList.addAll(addresses);
    }

    private void updateLocationNameList(List<Address> addresses) {
        locationNameList.clear();
        for (Address address : addresses) {
            locationNameList.add(getFullAddressString(address));
        }
        adapter.notifyDataSetChanged();
    }

    private String getFullAddressString(Address address) {
        String fullAddress = "";
        if (address.getFeatureName() != null) {
            fullAddress += address.getFeatureName();
        }
        if (address.getSubLocality() != null && !address.getSubLocality().isEmpty()) {
            fullAddress += ", " + address.getSubLocality();
        }
        if (address.getLocality() != null && !address.getLocality().isEmpty()) {
            fullAddress += ", " + address.getLocality();
        }
        if (address.getCountryName() != null && !address.getCountryName().isEmpty()) {
            fullAddress += ", " + address.getCountryName();
        }
        return fullAddress;
    }


    public static class Builder {
        private Double locationLatitude;
        private Double locationLongitude;
        private String locationSearchZone;
        private String layoutsToHide = "";
        private boolean enableSatelliteView = true;
        private boolean shouldReturnOkOnBackPressed = false;
        private String geolocApiKey = null;
        private boolean googlePlacesEnabled = false;

        public Builder() {
        }

        public Builder withLocation(double latitude, double longitude) {
            this.locationLatitude = latitude;
            this.locationLongitude = longitude;
            return this;
        }

        public Builder withLocation(LatLng latLng) {
            if (latLng != null) {
                this.locationLatitude = latLng.latitude;
                this.locationLongitude = latLng.longitude;
            }
            return this;
        }

        public Builder withSearchZone(String searchZone) {
            this.locationSearchZone = searchZone;
            return this;
        }

        public Builder withSatelliteViewHidden() {
            this.enableSatelliteView = false;
            return this;
        }

        public Builder shouldReturnOkOnBackPressed() {
            this.shouldReturnOkOnBackPressed = true;
            return this;
        }

        public Builder withStreetHidden() {
            this.layoutsToHide = String.format("%s|%s", layoutsToHide, OPTIONS_HIDE_STREET);
            return this;
        }

        public Builder withCityHidden() {
            this.layoutsToHide = String.format("%s|%s", layoutsToHide, OPTIONS_HIDE_CITY);
            return this;
        }

        public Builder withZipCodeHidden() {
            this.layoutsToHide = String.format("%s|%s", layoutsToHide, OPTIONS_HIDE_ZIPCODE);
            return this;
        }

        public Builder withGeolocApiKey(String apiKey) {
            this.geolocApiKey = apiKey;
            return this;
        }

        public Builder withGooglePlacesEnabled() {
            this.googlePlacesEnabled = true;
            return this;
        }

        public Intent build(Context context) {
            Intent intent = new Intent(context, LocationPickerActivity.class);

            if (locationLatitude != null) {
                intent.putExtra(LATITUDE, locationLatitude);
            }
            if (locationLongitude != null) {
                intent.putExtra(LONGITUDE, locationLongitude);
            }
            if (locationSearchZone != null) {
                intent.putExtra(SEARCH_ZONE, locationSearchZone);
            }
            if (!layoutsToHide.isEmpty()) {
                intent.putExtra(LAYOUTS_TO_HIDE, layoutsToHide);
            }
            intent.putExtra(BACK_PRESSED_RETURN_OK, shouldReturnOkOnBackPressed);
            intent.putExtra(ENABLE_SATELLITE_VIEW, enableSatelliteView);
            if (geolocApiKey != null) {
                intent.putExtra(GEOLOC_API_KEY, geolocApiKey);
            }
            intent.putExtra(ENABLE_GOOGLE_PLACES, googlePlacesEnabled);

            return intent;
        }
    }
}
