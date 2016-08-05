package net.simonjensen.autounlock;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class LocationService extends Service {
    String TAG = "LocatoinService";

    int startMode;       // indicates how to behave if the service is killed
    IBinder binder;      // interface for clients that bind
    boolean allowRebind; // indicates whether onRebind should be used

    LocationManager locationManager;
    Location previousLocation;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    // Define a listener that responds to location updates
    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            if (previousLocation == null) {
                insertLocationData(location);
                previousLocation = location;
            }else if (location.getProvider().equals("network")
                    && previousLocation.getProvider().equals("gps")
                    && System.currentTimeMillis() - previousLocation.getTime() < 6000) {
                Log.v(TAG, "Ignoring network location");
            } else {
                Log.v("Timediff", String.valueOf(System.currentTimeMillis() - previousLocation.getTime()));
                previousLocation = location;
                insertLocationData(location);
            }
        }

        public void insertLocationData(Location location) {
            // Called when a new location is found by the network location provider.
            long time = System.currentTimeMillis();

            LocationData aLocation;
            aLocation = new LocationData(location.getProvider(),
                    location.getLatitude(), location.getLongitude(), location.getAccuracy(), time);
            CoreService.recordedLocation.add(aLocation);

            Log.v("LOCATION: ", location.toString());
            CoreService.dataStore.insertLocation(
                    location.getProvider(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    time);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    public void onCreate() {
        // The service is being created
        powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationService");
        wakeLock.acquire();

        // Check that permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Log.v("LocationService", "Starting location gathering");
        // Register the listener with the Location Manager to receive location updates
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 1, locationListener);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, locationListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        return startMode;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return allowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.v("LocationService", "Stopping");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(locationListener);
        wakeLock.release();
    }
}
