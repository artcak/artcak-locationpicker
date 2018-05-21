package com.artcak.artcaklibrary.locationpicker.geocoder;

import android.location.Address;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddressBuilder {
    public List<Address> parseResult(String json) throws JSONException {
        List<Address> addresses = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray results = root.getJSONArray("results");
        for (int i = 0; i < results.length(); i++) {
            addresses.add(parseAddress(results.getJSONObject(i)));
        }
        return addresses;
    }

    private Address parseAddress(JSONObject jsonObject) throws JSONException {
        JSONObject location = jsonObject.getJSONObject("geometry").getJSONObject("location");
        double latitude = location.getDouble("lat");
        double longitude = location.getDouble("lng");

        List<AddressComponent> components = getAddressComponents(jsonObject.getJSONArray("address_components"));

        String formatted_address = jsonObject.getString("formatted_address");

        Address address = new Address(Locale.getDefault());
        address.setLatitude(latitude);
        address.setLongitude(longitude);
        address.setFeatureName(formatted_address);
        String street = "",number = "";
        String admin_lvl4 = "",admin_lvl3 = "";
        for (AddressComponent component : components) {
            if (component.types.contains("postal_code")) {
                address.setPostalCode(component.short_name);
            }
            if (component.types.contains("locality")) {
                address.setLocality(component.short_name);
            }
            if (component.types.contains("street_number")) {
                number = component.long_name;
            }
            if (component.types.contains("route")) {
                street = component.long_name;
            }
            if (component.types.contains("country")) {
                address.setCountryName(component.long_name);
                address.setCountryCode(component.short_name);
            }
            if (component.types.contains("administrative_area_level_1")){
                address.setAdminArea(component.short_name);
            }
            if (component.types.contains("administrative_area_level_2")){
                address.setSubAdminArea(component.short_name);
            }
            if (component.types.contains("administrative_area_level_3")){
                admin_lvl3 = component.short_name;
            }
            if (component.types.contains("administrative_area_level_4")){
                admin_lvl4 = component.short_name;
            }

        }
        StringBuilder fullAddress = new StringBuilder();
        fullAddress.append(street);
        if (!street.isEmpty() && !number.isEmpty()) {
            fullAddress.append(", ").append(number);
        }
        if (!admin_lvl4.equals("")){
            fullAddress.append(", ").append(admin_lvl4);
        }
        if (!admin_lvl3.equals("")){
            address.setSubAdminArea(admin_lvl3+", "+address.getSubAdminArea());
        }
        address.setLocality(admin_lvl3);
        address.setAddressLine(0, fullAddress.toString());
        return address;
    }

    private List<AddressComponent> getAddressComponents(JSONArray jsonComponents) throws JSONException {
        List<AddressComponent> components = new ArrayList<>();
        for (int i = 0; i < jsonComponents.length(); i++) {
            AddressComponent component = new AddressComponent();
            JSONObject jsonComponent = jsonComponents.getJSONObject(i);
            component.long_name = jsonComponent.getString("long_name");
            component.short_name = jsonComponent.getString("short_name");
            component.types = new ArrayList<>();
            JSONArray jsonTypes = jsonComponent.getJSONArray("types");
            for (int j = 0; j < jsonTypes.length(); j++) {
                component.types.add(jsonTypes.getString(j));
            }
            components.add(component);
        }
        return components;
    }

    private static class AddressComponent {
        String long_name;
        String short_name;
        List<String> types;
    }
}
