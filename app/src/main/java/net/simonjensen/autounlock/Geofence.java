package net.simonjensen.autounlock;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class Geofence implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    static String TAG = "Geofence";

    public ArrayList<com.google.android.gms.location.Geofence> geofenceArrayList;

    LocationManager locationManager;

    GoogleApiClient googleApiClient;

    public PendingIntent pendingIntent;

    public Geofence(Context context) {
        geofenceArrayList = new ArrayList<com.google.android.gms.location.Geofence>();

        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void connect() {
        googleApiClient.connect();
    }

    public void disconnect() {
        googleApiClient.disconnect();
    }

    public void populateGeofenceList(String name, LatLng location, Float radius) {

        Log.v("Populate geofence list", "Name: " + name + " Location: " + location);

        // Calculate hours to milliseconds
        int expireInHours = 12;
        long expireInMilliseconds = expireInHours * 60 * 60 * 1000;

        geofenceArrayList.add(new com.google.android.gms.location.Geofence.Builder()
                .setRequestId(name)
                .setCircularRegion(location.latitude, location.longitude, radius)
                .setExpirationDuration(expireInMilliseconds)
                .setTransitionTypes(com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER |
                        com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
    }

    public GeofencingRequest geofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceArrayList);

        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(Context context) {
        if (pendingIntent != null) {
            return pendingIntent;
        }
        Intent intent = new Intent(context, GeofenceService.class);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofenceArrayList", securityException);
    }

    public void addGeofence(Context context) {
        // Necessary permission check
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationManager.requestSingleUpdate(criteria, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                // Name should be BeKey MAC address
                populateGeofenceList("test", currentLocation, 50f);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        }, null);
    }

    public void registerGeofences(Context context) {
        Log.v("geofenceArrayList", geofenceArrayList.toString());

        if (!googleApiClient.isConnected()) {
            Log.v(TAG, "GoogleApiClient not connected");
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    geofencingRequest(),
                    getGeofencePendingIntent(context)
            ).setResultCallback(this);
        } catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }

    public void unregisterGeofences(Context context) {
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,
                    getGeofencePendingIntent(context)
            ).setResultCallback(this);
        } catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.v(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.v(TAG, "GoogleApiClient connection failed");
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.v(TAG, String.valueOf(status));
    }
}
