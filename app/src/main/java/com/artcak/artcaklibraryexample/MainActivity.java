package com.artcak.artcaklibraryexample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.artcak.artcaklibrary.locationpicker.LocationPicker;
import com.artcak.artcaklibrary.locationpicker.LocationPickerActivity;

public class MainActivity extends AppCompatActivity {
    TextView tv_latitude,tv_longitude,tv_lokasi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_latitude = findViewById(R.id.tv_latitude);
        tv_longitude = findViewById(R.id.tv_longitude);
        tv_lokasi = findViewById(R.id.tv_lokasi);
    }

    public void pickLocation(View view){
        Log.i("lol","pickLocation");
        Intent locationPickerIntent = new LocationPickerActivity.Builder().build(this);
        locationPickerIntent.putExtra(LocationPickerActivity.LATITUDE,-7.319622);
        locationPickerIntent.putExtra(LocationPickerActivity.LONGITUDE, 112.7828098);
        locationPickerIntent.putExtra(LocationPickerActivity.API_KEY,Var.apiKey_GoogleMaps);
        startActivityForResult(locationPickerIntent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==100){
            if (data!=null){
                tv_latitude.setText(String.valueOf(data.getDoubleExtra(LocationPickerActivity.LATITUDE,0)));
                tv_longitude.setText(String.valueOf(data.getDoubleExtra(LocationPickerActivity.LONGITUDE,0)));
                tv_lokasi.setText(data.getStringExtra(LocationPickerActivity.ADDRESS));
            }

        }
    }
}
