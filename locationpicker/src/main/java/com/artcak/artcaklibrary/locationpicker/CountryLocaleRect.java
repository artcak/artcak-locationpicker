package com.artcak.artcaklibrary.locationpicker;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

public class CountryLocaleRect {
    private static final LatLng ID_SBY_LOWER_LEFT = new LatLng(-16.775726, 80.774027);
    private static final LatLng ID_SBY_UPPER_RIGHT = new LatLng(21.186569, 145.948749);

    static LatLng getDefaultLowerLeft() {
        return getLowerLeftFromZone(Locale.getDefault());
    }

    static LatLng getDefaultUpperRight() {
        return getUpperRightFromZone(Locale.getDefault());
    }

    static LatLng getLowerLeftFromZone(Locale locale) {
        return ID_SBY_LOWER_LEFT;
    }

    static LatLng getUpperRightFromZone(Locale locale) {
        return ID_SBY_UPPER_RIGHT;
    }
}
