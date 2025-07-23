package com.example.xmobisense3.services;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.TextView;

import com.example.xmobisense3.utils.PermissionUtils;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class LocationCoordinatesService {

    private final LocationManager locationManager;
    private final Context context;
    private final TextView txtLocation;
    private final MapView mapView;
    private boolean locationReceived = false;

    public LocationCoordinatesService(Context context, TextView txtLocation, MapView mapView) {
        this.context = context;
        this.txtLocation = txtLocation;
        this.mapView = mapView;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }


    public void getLocation() {
        locationReceived = false;

        if (!PermissionUtils.hasLocationPermission(context)) {
            PermissionUtils.requestLocationPermission((Activity) context, 1);
            return;
        }

        try {
            boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!gpsEnabled && !networkEnabled) {
                txtLocation.setText("GPS or network provider is disabled.");
                return;
            }

            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null && !locationReceived) {
                        locationReceived = true;
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        String coords = "Longitude: " + longitude + "\nLatitude: " + latitude;
                        txtLocation.setText(coords);

                        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mapView.getController().setZoom(18.0);
                        mapView.getController().setCenter(point);

                        Marker marker = new Marker(mapView);
                        marker.setPosition(point);
                        marker.setTitle("Current Location");

                        mapView.getOverlays().clear();
                        mapView.getOverlays().add(marker);
                        mapView.invalidate();

                        locationManager.removeUpdates(this);
                    }
                }
            };

            if (gpsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
            }

            if (networkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
            }


        } catch (SecurityException e) {
            e.printStackTrace();
            txtLocation.setText("Location access error.");
        }
    }
}