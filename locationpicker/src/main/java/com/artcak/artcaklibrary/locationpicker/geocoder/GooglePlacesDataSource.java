package com.artcak.artcaklibrary.locationpicker.geocoder;
import android.location.Address;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.data.DataBufferUtils;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBufferResponse;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;

public class GooglePlacesDataSource {
    private final GeoDataClient geoDataClient;

    public GooglePlacesDataSource(GeoDataClient geoDataClient) {
        this.geoDataClient = geoDataClient;
    }

    public Observable<List<Address>> getFromLocationName(String query, LatLngBounds latLngBounds) {
        Log.i("debug","GooglePlacesDataSource getFromLocationName query : "+query);
        return Observable.defer(() -> {
            Task<AutocompletePredictionBufferResponse> results =
                    geoDataClient.getAutocompletePredictions(query, latLngBounds, null);
            try {
                Tasks.await(results, 6, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException | TimeoutException ignored) {
            }
            try {
                AutocompletePredictionBufferResponse autocompletePredictions = results.getResult();
                List<AutocompletePrediction> predictionList = DataBufferUtils.freezeAndClose(autocompletePredictions);
                List<Address> addressList = getAddressListFromPrediction(predictionList);
                return Observable.just(addressList);
            } catch (RuntimeExecutionException e) {
                return Observable.empty();
            }
        });
    }

    @NonNull
    private List<Address> getAddressListFromPrediction(List<AutocompletePrediction> predictionList) {
        Log.i("debug","GooglePlacesDataSource getAddressListFromPrediction predictionList : "+predictionList.size());
        List<Address> addressList = new ArrayList<>();
        for (AutocompletePrediction prediction : predictionList) {
            Task<PlaceBufferResponse> placeBufferResponseTask = geoDataClient.getPlaceById(prediction.getPlaceId());
            try {
                Tasks.await(placeBufferResponseTask, 3, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException | TimeoutException ignored) {
            }
            PlaceBufferResponse placeBufferResponse = placeBufferResponseTask.getResult();
            Place place = placeBufferResponse.get(0);
            addressList.add(mapPlaceToAddress(place));
        }
        Log.i("debug","GooglePlacesDataSource getAddressListFromPrediction addressList : "+addressList.size());
        return addressList;
    }

    @NonNull
    private Address mapPlaceToAddress(Place place) {
        Log.i("debug","GooglePlacesDataSource mapPlaceToAddress place : "+place.getName().toString());
        Address address = new Address(Locale.getDefault());
        address.setLatitude(place.getLatLng().latitude);
        address.setLongitude(place.getLatLng().longitude);
        String addressName = place.getName().toString() + " | " + place.getAddress().toString();
        address.setAddressLine(0, addressName);
        address.setFeatureName(addressName);
        return address;
    }
}
