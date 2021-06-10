package com.admin.maps_navjotmara_c08116572;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Polygon shape;
    private static final int QUADRILATERAL_SIDES_NUMBER= 4;
    private ArrayList<Marker> markers = new ArrayList();
    private GpsTracker gpsTracker;
    private Polyline line1;
    private double mycurrentLat = 0.0;
    private double mycurrentLong = 0.0;
    private RelativeLayout clearMapButton;
    private boolean showAlert = true;
    private PolylineOptions polylineOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        clearMapButton = findViewById(R.id.clearMapButton);
        //initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //clearMapButton click listener
        clearMapButton.setOnClickListener(v -> clearRefreshMap());
    }

    @Override
    protected void onResume() {
        super.onResume();
        reFetchLocation();
    }

    private void reFetchLocation() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            getLocation();
            if (gpsTracker.getLatitude() == 0.0) {
                reFetchLocation();
            }
        }, 3000);
    }

    private void clearRefreshMap() {
        if (mMap != null) {
            for (Marker marker : markers)
                marker.remove();
            if (markers != null && shape != null) {
                markers.clear();
                shape.remove();
                shape = null;
            }
            polylineOptions = new PolylineOptions();
            mMap.clear();
        }
    }

    private void getLocation() {
        //check if gps is enabled
        gpsTracker = new GpsTracker(this);
        if (gpsTracker.canGetLocation()) {
            mycurrentLat = gpsTracker.getLatitude();
            mycurrentLong = gpsTracker.getLongitude();
            gpsTracker.stopUsingGPS();
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                //show user's current location button.
                mMap.setMyLocationEnabled(true);
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mycurrentLat, mycurrentLong), 12));
                    }
                }, 1000);
            }
        } else {
            //and if gps not enabled, then request for location permission.
            if (showAlert) {
                gpsTracker.showSettingsAlert();
                showAlert = false;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        clearMapButton.setVisibility(View.VISIBLE);
        mMap.setOnMarkerClickListener(marker -> {
            getAdress(marker);
            return false;
        });

        // listen polygon  click
        mMap.setOnPolygonClickListener(polygon -> {
            // JUST RECHECK IT.
            double twoLc = distance(markers.get(0).getPosition().latitude, markers.get(0).getPosition().longitude, markers.get(1).getPosition().longitude, markers.get(1).getPosition().latitude);
            double restPts = distance(markers.get(2).getPosition().latitude, markers.get(2).getPosition().longitude, markers.get(3).getPosition().longitude, markers.get(3).getPosition().latitude);
            double result = twoLc + restPts;
            @SuppressLint("DefaultLocale") String finalDist = String.format("%.2f", result);
            Toast.makeText(getApplicationContext(), "Total Distance: " + finalDist + " miles", Toast.LENGTH_LONG).show();
        });

        // listen poly line click
        mMap.setOnPolylineClickListener(polyline -> {
            double dis = distance(polyline.getPoints().get(0).latitude,
                    polyline.getPoints().get(0).longitude,
                    polyline.getPoints().get(1).latitude,
                    polyline.getPoints().get(1).longitude);
            Log.e("dis", polyline.getId());
            @SuppressLint("DefaultLocale") String finalDist = String.format("%.2f", dis);
            Toast.makeText(MainActivity.this, "Distance between line is: " + finalDist + " miles", Toast.LENGTH_SHORT).show();
        });

        // listen mapLong click
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                setMarker(latLng);
            }
        });
    }

    private void drawShape() {
        //init add polygon on map
        PolygonOptions options = new PolygonOptions()
                .fillColor(0x3500FF00)
                .clickable(true)
                .strokeColor(Color.RED)
                .strokeWidth(1);

        for (int i = 0; i < QUADRILATERAL_SIDES_NUMBER; i++) {
            options.add(markers.get(i).getPosition());
            // code to
            if (i < QUADRILATERAL_SIDES_NUMBER - 1) {
                drawLines(markers.get(i), markers.get(i + 1));
            } else {
                drawLines(markers.get(i), markers.get(0));
            }
            options.add(markers.get(i).getPosition());
        }
        shape = mMap.addPolygon(options);
    }

    private void setMarker(LatLng latLng) {
        String title = "";
        switch (markers.size()) {
            case 0:
                title = "A";
                break;
            case 1:
                title = "B";
                break;
            case 2:
                title = "C";
                break;
            case 3:
                title = "D";
                break;
        }
        // calculate distance between two coordinates
        Double distance = distance(mycurrentLat, mycurrentLong, latLng.latitude, latLng.longitude);

        //"%.2f" is used to round off two decimal places.
        String finalDist = String.format("%.2f", distance);
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet("Distance = " + finalDist + " miles")
                .icon(bitmapDescriptorFromVector(getApplicationContext(), R.drawable.pin));

        if (markers.size() == QUADRILATERAL_SIDES_NUMBER)
            clearRefreshMap();
        markers.add(mMap.addMarker(options));

        if (markers.size() == QUADRILATERAL_SIDES_NUMBER)
            drawShape();
    }

    private void drawLines(Marker startingPoint, Marker endPoint) {
        //init and add poly lines on map
        polylineOptions = new PolylineOptions()
                .color(Color.RED)
                .width(8)
                .clickable(true);
        polylineOptions.add(startingPoint.getPosition(), endPoint.getPosition());
        line1 = mMap.addPolyline(polylineOptions);
    }

    private void getAdress(Marker marker) {

        // retrieving address of  coordinates using geocoding.
        Geocoder geocoder = new Geocoder(MainActivity.this);
        try {
            List<Address> addressList = geocoder.getFromLocation(marker.getPosition().latitude, marker.getPosition().longitude, 10);
            if (addressList != null && addressList.size() > 0) {
                for (Address address : addressList) {
                    Toast.makeText(MainActivity.this, "Clicked location is " + address.getAddressLine(0), Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //method to calculate distance between two coords.
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    //using custom marker icon.
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorDrawableResourceId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


}