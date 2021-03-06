package com.artcak.artcaklibrary.locationpicker.geocoder;

import android.location.Address;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;

import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;

public class GoogleGeocoderDataSource implements GeocoderInteractorDataSource {

    private static final String QUERY_REQUEST = "https://maps.googleapis.com/maps/api/geocode/json?address=%1$s&key=%2$s";
    private static final String QUERY_REQUEST_WITH_RECTANGLE
            = "https://maps.googleapis.com/maps/api/geocode/json?address=%1$s&key=%2$s&bounds=%3$f,%4$f|%5$f,%6$f";
    private static final String QUERY_LAT_LONG = "https://maps.googleapis.com/maps/api/geocode/json?latlng=%1$f,%2$f&key=%3$s";
    private String apiKey;
    private final NetworkClient networkClient;
    private final AddressBuilder addressBuilder;

    public GoogleGeocoderDataSource(NetworkClient networkClient, AddressBuilder addressBuilder) {
        Log.i("debug","GoogleGeocoderDataSource ");
        this.networkClient = networkClient;
        this.addressBuilder = addressBuilder;
    }

    public void setApiKey(String apiKey) {
        Log.i("debug","GoogleGeocoderDataSource ");
        this.apiKey = apiKey;
    }

    @Override
    public Observable<List<Address>> getFromLocationName(String query) {
        Log.i("debug","GoogleGeocoderDataSource getFromLocationName query A : "+query+"|apiKey : "+apiKey);
        return Observable.create(subscriber -> {
            if (apiKey == null) {
                subscriber.onComplete();
                return;
            }
            try {
                String urlRequest = String.format(Locale.ENGLISH,
                        QUERY_REQUEST, query.trim(), apiKey);
                String result = networkClient.requestFromLocationName(urlRequest);
                List<Address> addresses = addressBuilder.parseResult(result);
                Log.i("debugs","GoogleGeocoderDataSource getFromLocationName short QUERY_REQUEST A: "+urlRequest);
                Log.i("debugs","GoogleGeocoderDataSource getFromLocationName short result: "+result);
                subscriber.onNext(addresses);
                subscriber.onComplete();
            } catch (JSONException e) {
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<List<Address>> getFromLocationName(String query, LatLng lowerLeft,LatLng upperRight) {
        Log.i("debug","GoogleGeocoderDataSource getFromLocationName query B : "+query+"|apiKey : "+apiKey);
        return Observable.create(subscriber -> {
            if (apiKey == null) {
                subscriber.onComplete();
                return;
            }
            try {
                String urlRequest = String.format(Locale.ENGLISH,
                        QUERY_REQUEST_WITH_RECTANGLE, query.trim(), apiKey, lowerLeft.latitude,
                        lowerLeft.longitude, upperRight.latitude, upperRight.longitude);
                String result = networkClient.requestFromLocationName(urlRequest);
                List<Address> addresses = addressBuilder.parseResult(result);
                Log.i("debugs","GoogleGeocoderDataSource getFromLocationName short QUERY_REQUEST B: "+urlRequest);
                Log.i("debugs","GoogleGeocoderDataSource getFromLocationName short result: "+result);
                subscriber.onNext(addresses);
                subscriber.onComplete();
            } catch (JSONException e) {
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<List<Address>> getFromLocation(double latitude, double longitude) {
        Log.i("debug","GoogleGeocoderDataSource getFromLocation: "+latitude+"|"+longitude);
        return Observable.create(subscriber -> {
            if (apiKey == null) {
                subscriber.onComplete();
                return;
            }
            try {
                String urlRequest = String.format(Locale.ENGLISH,
                        QUERY_LAT_LONG, latitude, longitude, apiKey);
                String result = networkClient.requestFromLocationName(urlRequest);
                List<Address> addresses = addressBuilder.parseResult(result);
                Log.i("debugs","GoogleGeocoderDataSource getFromLocationName short QUERY_REQUEST C: "+urlRequest);
                Log.i("debugs","GoogleGeocoderDataSource getFromLocationName short result: "+result);

                subscriber.onNext(addresses);
                subscriber.onComplete();
            } catch (JSONException e) {
                subscriber.onError(e);
            }
        });
    }
}

