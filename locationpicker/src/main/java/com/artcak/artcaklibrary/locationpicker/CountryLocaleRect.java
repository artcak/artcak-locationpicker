package com.artcak.artcaklibrary.locationpicker;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

public class CountryLocaleRect {
    private static final LatLng ID_SBY_LOWER_LEFT = new LatLng(-7.397115, 112.594526);
    private static final LatLng ID_SBY_UPPER_RIGHT = new LatLng(-7.186346, 112.932272);

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
