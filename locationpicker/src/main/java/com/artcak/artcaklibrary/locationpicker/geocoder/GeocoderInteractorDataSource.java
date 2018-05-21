package com.artcak.artcaklibrary.locationpicker.geocoder;

import android.location.Address;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public interface GeocoderInteractorDataSource {
    io.reactivex.Observable<List<Address>> getFromLocationName(String query);

    io.reactivex.Observable<List<Address>> getFromLocationName(String query, LatLng lowerLeft, LatLng upperRight);

    io.reactivex.Observable<List<Address>> getFromLocation(double latitude, double longitude);
}
