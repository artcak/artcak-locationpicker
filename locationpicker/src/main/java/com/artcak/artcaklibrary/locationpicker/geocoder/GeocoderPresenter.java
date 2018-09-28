package com.artcak.artcaklibrary.locationpicker.geocoder;

import android.annotation.SuppressLint;
import android.location.Address;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider;

public class GeocoderPresenter {
    private static final int RETRY_COUNT = 3;
    private static final int MAX_PLACES_RESULTS = 3;

    private GeocoderViewInterface view;
    private final GeocoderViewInterface nullView = new GeocoderViewInterface.NullView();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Scheduler scheduler;
    private ReactiveLocationProvider locationProvider;
    private GeocoderRepository geocoderRepository;
    private GooglePlacesDataSource googlePlacesDataSource;
    private boolean isGooglePlacesEnabled = false;

    public GeocoderPresenter(ReactiveLocationProvider reactiveLocationProvider, GeocoderRepository geocoderRepository,
                             GooglePlacesDataSource placesDataSource) {
        this(reactiveLocationProvider, geocoderRepository, placesDataSource, AndroidSchedulers.mainThread());
    }

    public GeocoderPresenter(ReactiveLocationProvider reactiveLocationProvider, GeocoderRepository geocoderRepository,
                             GooglePlacesDataSource placesDataSource, Scheduler scheduler) {
        this.geocoderRepository = geocoderRepository;
        this.view = nullView;
        this.scheduler = scheduler;
        this.locationProvider = reactiveLocationProvider;
        this.googlePlacesDataSource = placesDataSource;
    }

    public void setUI(GeocoderViewInterface geocoderViewInterface) {
        this.view = geocoderViewInterface;
    }

    public void stop() {
        this.view = nullView;
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }

    public void getLastKnownLocation() {
        @SuppressLint("MissingPermission")
        Disposable disposable = locationProvider.getLastKnownLocation()
                .retry(RETRY_COUNT)
                .subscribe(view::showLastLocation, throwable -> {
                }, view::didGetLastLocation);
        compositeDisposable.add(disposable);
    }

    public void getFromLocationName(String query) {
        Log.i("debugs","GeocoderPresenter getFromLocationName 1 query: "+query);
        view.willLoadLocation();
        Disposable disposable = geocoderRepository.getFromLocationName(query)
                .observeOn(scheduler)
                .subscribe(view::showLocations, throwable -> view.showLoadLocationError(),
                        view::didLoadLocation);
        compositeDisposable.add(disposable);
    }

    public void getFromLocationName(String query, LatLng lowerLeft, LatLng upperRight) {
        Log.i("debugs","GeocoderPresenter getFromLocationName b query: "+query);
        view.willLoadLocation();
        Disposable disposable = Observable.zip(
                geocoderRepository.getFromLocationName(query, lowerLeft, upperRight),
                getPlacesFromLocationName(query, lowerLeft, upperRight),
                this::getMergedList)
                .subscribeOn(Schedulers.io())
                .observeOn(scheduler)
                .retry(RETRY_COUNT)
                .subscribe(view::showLocations, throwable -> view.showLoadLocationError(),
                        view::didLoadLocation);
        compositeDisposable.add(disposable);
    }

    public void getDebouncedFromLocationName(String query, int debounceTime) {
        Log.i("debugs","GeocoderPresenter getDebouncedFromLocationName query: "+query);
        view.willLoadLocation();
        Disposable disposable = geocoderRepository.getFromLocationName(query)
                .debounce(debounceTime, TimeUnit.MILLISECONDS, Schedulers.io())
                .observeOn(scheduler)
                .subscribe(view::showDebouncedLocations, throwable -> view.showLoadLocationError(),
                        view::didLoadLocation);
        compositeDisposable.add(disposable);
    }

    public void getDebouncedFromLocationName(String query, LatLng lowerLeft, LatLng upperRight, int debounceTime) {
        Log.i("debugs","GeocoderPresenter getDebouncedFromLocationName query: "+query);

        view.willLoadLocation();
        Disposable disposable = Observable.zip(
                geocoderRepository.getFromLocationName(query, lowerLeft, upperRight),
                getPlacesFromLocationName(query, lowerLeft, upperRight), this::getMergedList)
                .subscribeOn(Schedulers.io())
                .debounce(debounceTime, TimeUnit.MILLISECONDS, Schedulers.io())
                .observeOn(scheduler)
                .subscribe(view::showDebouncedLocations, throwable -> view.showLoadLocationError(),
                        view::didLoadLocation);
        compositeDisposable.add(disposable);
    }

    public void getInfoFromLocation(LatLng latLng) {
        Log.i("debugs","GeocoderPresenter getDebouncedFromLocationName latLng: "+latLng.toString());
        view.willGetLocationInfo(latLng);
        Disposable disposable = geocoderRepository.getFromLocation(latLng)
                .observeOn(scheduler)
                .retry(RETRY_COUNT)
                .subscribe(view::showLocationInfo, throwable -> view.showGetLocationInfoError(),
                        view::didGetLocationInfo);
        compositeDisposable.add(disposable);
    }

    public void enableGooglePlaces() {
        this.isGooglePlacesEnabled = true;
    }

    private Observable<List<Address>> getPlacesFromLocationName(String query, LatLng lowerLeft, LatLng upperRight) {
        Log.i("debugs","GeocoderPresenter getPlacesFromLocationName query: "+query+"| isGooglePlacesEnabled: "+isGooglePlacesEnabled);
        return isGooglePlacesEnabled ? googlePlacesDataSource.getFromLocationName(query, new LatLngBounds(lowerLeft, upperRight))
                .flatMapIterable(addresses -> addresses).take(MAX_PLACES_RESULTS).toList().toObservable()
                .onErrorReturnItem(new ArrayList<>()) : Observable.just(new ArrayList<>());
    }

    @NonNull
    private List<Address> getMergedList(List<Address> geocoderList, List<Address> placesList) {
        Log.i("debugs","GeocoderPresenter ggetMergedList");
        List<Address> mergedList = new ArrayList<>();
        mergedList.addAll(placesList);
        mergedList.addAll(geocoderList);
        return mergedList;
    }
}
